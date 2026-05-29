package no.nav.veilarboppfolging.service

import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.common.types.identer.NavIdent
import no.nav.pto_schema.enums.arena.Formidlingsgruppe
import no.nav.veilarboppfolging.client.aap.AapClient
import no.nav.veilarboppfolging.client.arbeidssoekerregisteret.ArbeidssoekerregisteretClient
import no.nav.veilarboppfolging.client.tiltakshistorikk.TiltakshistorikkClient
import no.nav.veilarboppfolging.client.ungdomsprogram.UngdomsprogramClient
import no.nav.veilarboppfolging.client.veilarbarena.ArenaOppfolgingTilstand
import no.nav.veilarboppfolging.domain.OppfolgingStatusData
import no.nav.veilarboppfolging.eventsLogger.BigQueryClient
import no.nav.veilarboppfolging.oppfolgingsbruker.VeilederRegistrant
import no.nav.veilarboppfolging.oppfolgingsbruker.arena.ArenaOppfolgingService
import no.nav.veilarboppfolging.oppfolgingsbruker.utgang.*
import no.nav.veilarboppfolging.oppfolgingsbruker.utgang.KunneAvsluttesResultat.Companion.kanAvsluttes
import no.nav.veilarboppfolging.oppfolgingsperioderHendelser.hendelser.OppfolgingsAvsluttetHendelseDto.Companion.of
import no.nav.veilarboppfolging.repository.OppfolgingsPeriodeRepository
import no.nav.veilarboppfolging.repository.OppfolgingsStatusRepository
import no.nav.veilarboppfolging.repository.entity.OppfolgingEntity
import no.nav.veilarboppfolging.repository.entity.OppfolgingsperiodeEntity
import no.nav.veilarboppfolging.utils.ArenaUtils
import no.nav.veilarboppfolging.utils.DtoMappers
import no.nav.veilarboppfolging.utils.EnumUtils
import no.nav.veilarboppfolging.utils.OppfolgingsperiodeUtils
import no.nav.veilarboppfolging.utils.SecureLog.secureLog
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.TransactionStatus
import org.springframework.transaction.support.TransactionTemplate
import java.util.Optional
import java.util.function.Consumer

@Service
class AvsluttOppfolgingService(
    val authService: AuthService,
    val oppfolgingsStatusRepository: OppfolgingsStatusRepository,
    val oppfolgingsPeriodeRepository: OppfolgingsPeriodeRepository,
    val arenaOppfolgingService: ArenaOppfolgingService,
    val kafkaProducerService: KafkaProducerService,
    val kvpService: KvpService,
    val tiltakshistorikkClient: TiltakshistorikkClient,
    val ungdomsprogramClient: UngdomsprogramClient,
    val aapClient: AapClient,
    val arbeidssoekerregisteretClient: ArbeidssoekerregisteretClient,
    val bigQueryClient: BigQueryClient,
    val transactor: TransactionTemplate
) {

    val log = LoggerFactory.getLogger(this::class.java)

    public fun avsluttOppfolging(avregistrering: Avregistrering): KunneAvsluttesResultat {
        val aktorId = avregistrering.aktorId
        val fnr = authService.getFnrOrThrow(aktorId)

        val oppfolging: OppfolgingEntity? = oppfolgingsStatusRepository.hentOppfolging(aktorId).orElse(null)

        if (authService.erInternBruker()) {
            authService.sjekkSkriveTilgangMedFnr(fnr)
            oppfolging?.getOppfolgingsEnhet()
                ?.let { enhet -> authService.sjekkTilgangTilEnhet(enhet.get()) }
            secureLog.info(
                "Veileder: {} forsøker å avslutte oppfølging for fnr: {}",
                authService.innloggetBrukerIdent,
                fnr.get()
            )
        } else {
            secureLog.info("Forsøker å avslutte oppfølging for fnr: {} som systembruker", fnr.get())
        }

        val kanAvslutte: KunneAvsluttesResultat = kanAvslutteOppfolging(avregistrering, fnr, oppfolging)
        if (kanAvslutte is KunneAvsluttes) {
            val veilederId = avregistrering.avsluttetAv.getIdent()
            val begrunnelse = avregistrering.begrunnelse
            secureLog.info(
                "Avslutting av oppfølging utført av: {}, begrunnelse: {}, tilstand i Arena for aktorid {}",
                veilederId,
                begrunnelse,
                aktorId
            )
            avsluttOppfolgingForBruker(kanAvslutte)
        } else {
            log.warn(
                "Oppfølging ble ikke avsluttet likevel, avregistreringstype {}: begrunnelse {}",
                avregistrering.getAvregistreringsType(),
                (kanAvslutte as KunneIkkeAvsluttes).begrunnelse
            )
        }
        return kanAvslutte
    }

    private fun avsluttOppfolgingForBruker(kanAvsluttesResultat: KunneAvsluttes) {
        val avregistrering = kanAvsluttesResultat.avregistrering
        val fnr = authService.getFnrOrThrow(avregistrering.aktorId)
        val aktivIArena = !kanAvsluttesResultat.erIserv
        val aktorId = avregistrering.aktorId
        transactor.executeWithoutResult(Consumer { ignored: TransactionStatus? ->
            oppfolgingsPeriodeRepository.avsluttSistePeriodeOgAvsluttOppfolging(
                aktorId,
                avregistrering.avsluttetAv.getIdent(),
                avregistrering.begrunnelse
            )
            val perioder: List<OppfolgingsperiodeEntity> = oppfolgingsPeriodeRepository.hentOppfolgingsperioder(aktorId)
            val sistePeriode = OppfolgingsperiodeUtils.hentSisteOppfolgingsperiode(perioder)

            log.info("Oppfølgingsperiode avsluttet for bruker - publiserer endringer på oppfølgingsperiode-topics.")
            kafkaProducerService.publiserOppfolgingsperiode(DtoMappers.tilOppfolgingsperiodeDTO(sistePeriode))
            kafkaProducerService.publiserVeilederTilordnet(aktorId, null, null)
            kafkaProducerService.publiserEndringPaNyForVeileder(aktorId, false)
            kafkaProducerService.publiserEndringPaManuellStatus(aktorId, false)
            kafkaProducerService.publiserOppfolgingsAvsluttet(of(avregistrering, sistePeriode, fnr))
            kafkaProducerService.publiserSkjulAoMinSideMicrofrontend(aktorId, fnr)

            // oppfolgingsperiodeEndretService.oppdaterSisteOppfolgingsperiodeV2MedAvsluttetStatus(sistePeriode); // TODO I en overgangsperiode lytter vi heller på tombstone fra ao-oppfolgingskontor
            bigQueryClient.loggAvsluttOppfolgingsperiode(sistePeriode.getUuid(), avregistrering, aktivIArena)
        })
    }

    fun adminForceAvsluttOppfolgingForBruker(aktorId: AktorId, veilederId: String, begrunnelse: String) {
        adminAvsluttOppfolgingForBruker(
            AdminAvregistrering(
                aktorId,
                VeilederRegistrant(NavIdent(veilederId)),
                begrunnelse,
                null
            )
        )
    }

    private fun adminAvsluttOppfolgingForBruker(avregistrering: AdminAvregistrering) {
        val formidlingsgruppe = oppfolgingsStatusRepository.hentOppfolging(avregistrering.aktorId)
            .flatMap { it.localArenaOppfolging }
            .map { it.formidlingsgruppe }.orElse(null)
        val erIservIArena = Formidlingsgruppe.ISERV == formidlingsgruppe
        avsluttOppfolgingForBruker(KunneAvsluttes(avregistrering, erIservIArena))
    }

    fun kanAvsluttePgaBleIserv(
        fnr: Fnr,
        aktorId: AktorId,
        oppfolging: Optional<OppfolgingEntity>,
        erInaktivIArena: Boolean,
        kanEnkeltReaktiveres: Boolean
    ): OppfolgingService.KanAvslutteMedBegrunnelse {
        val harAktiveTiltaksdeltakelser = harAktiveTiltaksdeltakelser(fnr)
        val erDeltakerIUngdomsprogrammet = erDeltakerIUngdomsprogrammet(fnr)
        val erArbeidssoeker = erArbeidssoeker(fnr)
        val harAap = harAap(fnr)
        val underKvp = kvpService.erUnderKvp(aktorId)

        val kanIkkeAvsluttesBegrunnelse = KunneAvsluttesResultat.kanAvsluttesPgaIservIArena(
            KanAvsluttesInput(
                erUnderOppfolging = oppfolging.map { it.isUnderOppfolging }.orElse(false) ?: false,
                erIservIArena = erInaktivIArena,
                harAktiveTiltaksdeltakelser = harAktiveTiltaksdeltakelser,
                erDeltakerIUngdomsprogrammet = erDeltakerIUngdomsprogrammet,
                erArbeidssoeker = erArbeidssoeker,
                harAap = harAap,
                underKvp = underKvp,
            ),
            kanReaktiveres = kanEnkeltReaktiveres
        )

        secureLog.info(
            "Status for automatisk avslutting av oppfølging. aktorId={} kanEnkeltReaktiveres={} erUnderKvp={} harAktiveTiltaksdeltakelser={} erDeltakerIUngdomsprogrammet={} erArbeidssoeker={} harAap={} kanAvsluttes={}",
            aktorId,
            kanEnkeltReaktiveres,
            underKvp,
            harAktiveTiltaksdeltakelser,
            erDeltakerIUngdomsprogrammet,
            erArbeidssoeker,
            harAap,
            kanIkkeAvsluttesBegrunnelse
        )

        return when (kanIkkeAvsluttesBegrunnelse) {
            null -> OppfolgingService.KanAvslutteMedBegrunnelse(true, erInaktivIArena, null)
            else -> OppfolgingService.KanAvslutteMedBegrunnelse(false, erInaktivIArena, kanIkkeAvsluttesBegrunnelse)
        }
    }


    private fun kanAvslutteOppfolging(
        avregistrering: Avregistrering,
        fnr: Fnr,
        oppfolging: OppfolgingEntity?
    ): KunneAvsluttesResultat {
        val erIserv = when (avregistrering) {
            is ArenaIservKanIkkeReaktiveres -> true
            else -> {
                arenaOppfolgingService.hentArenaOppfolgingTilstand(fnr)
                    .orElseThrow { RuntimeException("Feilet under henting av areana-oppfolgingsstatus (db) med fallback til veilarbarena /oppfolgingsbruker") }
                    .let { EnumUtils.valueOf(Formidlingsgruppe::class.java, it.getFormidlingsgruppe()) }
                    .let { it == Formidlingsgruppe.ISERV }
            }
        }

        val harAktiveTiltaksdeltakelser = harAktiveTiltaksdeltakelser(fnr)
        val erDeltakerIUngdomsprogrammet = erDeltakerIUngdomsprogrammet(fnr)
        val erArbeidssoeker = erArbeidssoeker(fnr)
        val harAap = harAap(fnr)
        val underKvp = kvpService.erUnderKvp(avregistrering.aktorId)
        return kanAvsluttes(
            avregistrering,
            KanAvsluttesInput(
                erUnderOppfolging = oppfolging?.isUnderOppfolging ?: false,
                erIservIArena = erIserv,
                harAktiveTiltaksdeltakelser = harAktiveTiltaksdeltakelser,
                erDeltakerIUngdomsprogrammet = erDeltakerIUngdomsprogrammet,
                erArbeidssoeker = erArbeidssoeker,
                harAap = harAap,
                underKvp = underKvp
            )
        )
    }

    fun harAktiveTiltaksdeltakelser(fnr: Fnr) = tiltakshistorikkClient.harAktiveTiltaksdeltakelser(fnr.get())
    fun erDeltakerIUngdomsprogrammet(fnr: Fnr) = ungdomsprogramClient.erDeltakerIUngdomsprogrammet(fnr.get())
    fun erArbeidssoeker(fnr: Fnr) = arbeidssoekerregisteretClient.erArbeidssoeker(fnr.get())
    fun harAap(fnr: Fnr) = aapClient.harAap(fnr.get())
}