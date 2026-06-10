package no.nav.veilarboppfolging.service

import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.common.types.identer.NavIdent
import no.nav.pto_schema.enums.arena.Formidlingsgruppe
import no.nav.veilarboppfolging.client.aap.AapClient
import no.nav.veilarboppfolging.client.arbeidssoekerregisteret.ArbeidssoekerregisteretClient
import no.nav.veilarboppfolging.client.tiltakshistorikk.TiltakshistorikkClient
import no.nav.veilarboppfolging.client.ungdomsprogram.UngdomsprogramClient
import no.nav.veilarboppfolging.domain.AvslutningStatusData
import no.nav.veilarboppfolging.eventsLogger.BigQueryClient
import no.nav.veilarboppfolging.oppfolgingsbruker.VeilederRegistrant
import no.nav.veilarboppfolging.oppfolgingsbruker.arena.ArenaOppfolgingService
import no.nav.veilarboppfolging.oppfolgingsbruker.arena.ArenaOppfolgingTilstandOppslagResult
import no.nav.veilarboppfolging.oppfolgingsbruker.utgang.*
import no.nav.veilarboppfolging.oppfolgingsbruker.utgang.KunneAvsluttesResultat.Companion.kanAvsluttes
import no.nav.veilarboppfolging.oppfolgingsperioderHendelser.hendelser.OppfolgingsAvsluttetHendelseDto.Companion.of
import no.nav.veilarboppfolging.repository.OppfolgingsPeriodeRepository
import no.nav.veilarboppfolging.repository.OppfolgingsStatusRepository
import no.nav.veilarboppfolging.repository.entity.OppfolgingEntity
import no.nav.veilarboppfolging.repository.entity.OppfolgingsperiodeEntity
import no.nav.veilarboppfolging.utils.DtoMappers
import no.nav.veilarboppfolging.utils.EnumUtils
import no.nav.veilarboppfolging.utils.OppfolgingsperiodeUtils
import no.nav.veilarboppfolging.utils.SecureLog.secureLog
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import java.time.ZonedDateTime
import java.util.*
import no.nav.veilarboppfolging.repository.ArbeidsoppfolgingskontorRepository

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
    val arenaYtelserService: ArenaYtelserService,
    val bigQueryClient: BigQueryClient,
    val transactor: TransactionTemplate,
    val arbeidsoppfolgingskontorRepository: ArbeidsoppfolgingskontorRepository,
) {

    val log = LoggerFactory.getLogger(this::class.java)

    fun avsluttOppfolgingHvisKanAvsluttes(avregistrering: Avregistrering): KunneAvsluttesResultat {
        val aktorId = avregistrering.aktorId
        val fnr = authService.getFnrOrThrow(aktorId)

        val oppfolging = oppfolgingsStatusRepository.hentOppfolging(aktorId).orElse(null)

        if (authService.erInternBruker()) {
            authService.sjekkSkriveTilgangMedFnr(fnr)
            oppfolging?.getOppfolgingsEnhet()
                ?.let { enhet -> authService.sjekkTilgangTilEnhet(enhet.get()) }
            secureLog.info(
                "Veileder: {} forsøker å avslutte oppfølging for fnr: {}",
                authService.innloggetBrukerIdent,
                fnr.get()
            )
        }
        else if (authService.erEksternBruker()) {
            throw IllegalStateException("Vi støtter ikke at eksternbrukere kan avslutte oppfølging")
        } else {
            secureLog.info("Forsøker å avslutte oppfølging for fnr: {} som systembruker", fnr.get())
        }

        val kanAvslutte: KunneAvsluttesResultat = samleDataSynkrontOgSjekkOmOppfolgingKanAvsluttes(avregistrering, fnr, oppfolging)
        when (kanAvslutte) {
            is KunneAvsluttes -> {
                secureLog.info(
                    "Avslutting av oppfølging utført av: {}, begrunnelse: {}, tilstand i Arena for aktorid {}",
                    avregistrering.avsluttetAv.getIdent(),
                    avregistrering.begrunnelse,
                    avregistrering.aktorId
                )
                avsluttOppfolgingForBruker(kanAvslutte)
            }
            is KunneIkkeAvsluttes -> {
                log.warn(
                    "Oppfølging ble ikke avsluttet likevel, avregistreringstype {}: begrunnelse {}",
                    avregistrering.getAvregistreringsType(),
                    kanAvslutte.begrunnelse
                )
            }
        }
        return kanAvslutte
    }

    fun hentAvslutningstatusForManuellAvslutning(fnr: Fnr): AvslutningStatusData {
        authService.sjekkLesetilgangMedFnr(fnr)
        return getAvslutningStatusForManuellAvslutning(fnr)
    }

    private fun getAvslutningStatusForManuellAvslutning(fnr: Fnr): AvslutningStatusData {
        val aktorId = authService.getAktorIdOrThrow(fnr)
        val arenaOppfolgingResult = arenaOppfolgingService.hentArenaOppfolgingTilstand(fnr)
        val inaktiveringsDato = when(arenaOppfolgingResult) {
            is ArenaOppfolgingTilstandOppslagResult.Fail, is ArenaOppfolgingTilstandOppslagResult.NotFound -> null
            is ArenaOppfolgingTilstandOppslagResult.Success -> arenaOppfolgingResult.arenaOppfolgingTilstand.inaktiveringsdato
        }

        val oppfolging = oppfolgingsStatusRepository.hentOppfolging(aktorId).orElse(null)

        val kanAvsluttes = samleDataSynkrontOgSjekkOmOppfolgingKanAvsluttes(
            ManuellAvregistrering(
                aktorId,
                VeilederRegistrant(NavIdent("12321")),
                "fordi"
            ), fnr, oppfolging
        )

        return AvslutningStatusData.builder()
            .harYtelser(arenaYtelserService.harPagaendeYtelse(fnr))
            .inaktiveringsDato(inaktiveringsDato)
            .kanAvslutte(kanAvsluttes is KunneAvsluttes)
            .underOppfolging(kanAvsluttes.kanAvsluttesInput.erUnderOppfolging)
            .underKvp(kanAvsluttes.kanAvsluttesInput.underKvp)
            .erIserv(kanAvsluttes.kanAvsluttesInput.erIservIArena)
            .harAktiveTiltaksdeltakelser(kanAvsluttes.kanAvsluttesInput.harAktiveTiltaksdeltakelser)
            .erDeltakerIUngdomsprogrammet(kanAvsluttes.kanAvsluttesInput.erDeltakerIUngdomsprogrammet)
            .erArbeidssoeker(kanAvsluttes.kanAvsluttesInput.erArbeidssoeker)
            .harAap(kanAvsluttes.kanAvsluttesInput.harAap)
            .build()
    }

    private fun avsluttOppfolgingForBruker(kanAvsluttesResultat: AvslutningsInput) {
        val avregistrering = kanAvsluttesResultat.avregistrering
        val fnr = authService.getFnrOrThrow(avregistrering.aktorId)
        val aktivIArena = when (kanAvsluttesResultat) {
            is KunneAvsluttes -> !kanAvsluttesResultat.erIserv
            is KunneAvsluttesOverstyring -> null
        }
        val aktorId = avregistrering.aktorId
        transactor.executeWithoutResult { _ ->
            oppfolgingsPeriodeRepository.avsluttSistePeriodeOgAvsluttOppfolging(
                aktorId,
                avregistrering.avsluttetAv.getIdent(),
                avregistrering.begrunnelse,
                avregistrering.getAvregistreringsType()
            )
            val perioder: List<OppfolgingsperiodeEntity> = oppfolgingsPeriodeRepository.hentOppfolgingsperioder(aktorId)
            val sistePeriode = OppfolgingsperiodeUtils.hentSisteOppfolgingsperiode(perioder)

            arbeidsoppfolgingskontorRepository.slettNavKontor(sistePeriode.uuid)

            log.info("Oppfølgingsperiode avsluttet for bruker - publiserer endringer på oppfølgingsperiode-topics.")
            kafkaProducerService.publiserOppfolgingsperiode(DtoMappers.tilOppfolgingsperiodeDTO(sistePeriode))
            kafkaProducerService.publiserVeilederTilordnet(aktorId, null, null)
            kafkaProducerService.publiserEndringPaNyForVeileder(aktorId, false)
            kafkaProducerService.publiserEndringPaManuellStatus(aktorId, false)
            kafkaProducerService.publiserOppfolgingsAvsluttet(of(avregistrering, sistePeriode, fnr))
            kafkaProducerService.publiserSkjulAoMinSideMicrofrontend(aktorId, fnr)

            // oppfolgingsperiodeEndretService.oppdaterSisteOppfolgingsperiodeV2MedAvsluttetStatus(sistePeriode); // TODO I en overgangsperiode lytter vi heller på tombstone fra ao-oppfolgingskontor
            bigQueryClient.loggAvsluttOppfolgingsperiode(sistePeriode.uuid, avregistrering, aktivIArena)
        }
    }

    private fun samleDataSynkrontOgSjekkOmOppfolgingKanAvsluttes(
        avregistrering: Avregistrering,
        fnr: Fnr,
        oppfolging: OppfolgingEntity?
    ): KunneAvsluttesResultat {
        val erIserv = when (avregistrering) {
            is ArenaIservKanIkkeReaktiveres -> true
            else -> {
                val arenaOppfolingResult = arenaOppfolgingService.hentArenaOppfolgingTilstand(fnr)
                when (arenaOppfolingResult) {
                    is ArenaOppfolgingTilstandOppslagResult.Fail -> throw RuntimeException("Feilet under henting av arena-oppfolgingsstatus (db) med fallback til veilarbarena /oppfolgingsbruker")
                    is ArenaOppfolgingTilstandOppslagResult.NotFound -> true.also { log.info("Finnes ikke Arena-data for bruker, tolker det som ISERV") }
                    is ArenaOppfolgingTilstandOppslagResult.Success -> arenaOppfolingResult
                        .let { EnumUtils.valueOf(Formidlingsgruppe::class.java, it.arenaOppfolgingTilstand.formidlingsgruppe) }
                        .let { it == Formidlingsgruppe.ISERV }
                }
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

    fun adminAvsluttSpesifikkOppfolgingsperiode(
        aktorId: AktorId,
        veilederId: String,
        begrunnelse: String,
        uuid: String?
    ) {
        if (uuid == null) {
            log.info("oppfolgingsperiodeUUID er null")
            return
        }

        try {
            val oppfolgingsperiodeUUID = UUID.fromString(uuid)
            adminAvsluttValgtOppfolgingsperiode(
                AdminAvregistrering(
                    aktorId,
                    VeilederRegistrant(NavIdent(veilederId)),
                    begrunnelse,
                    oppfolgingsperiodeUUID
                )
            )
        } catch (e: IllegalArgumentException) {
            log.warn("Invalid UUID format for oppfolgingsperiodeUUID: {}", uuid, e)
        }
    }

    private fun adminAvsluttValgtOppfolgingsperiode(avregistrering: AdminAvregistrering) {
        val oppfolgingsperiodeUUID = avregistrering.oppfolgingsperiodeUUID
        val gjeldendePerioder = oppfolgingsPeriodeRepository.hentOppfolgingsperioder(avregistrering.aktorId)
            .filter { p -> p.sluttDato == null }
        val sisteGjeldendePeriode = OppfolgingsperiodeUtils.hentSisteOppfolgingsperiode(gjeldendePerioder)
        val valgtGjeldendePeriode = gjeldendePerioder.firstOrNull { p -> p.uuid == oppfolgingsperiodeUUID }

        if (valgtGjeldendePeriode == null) {
            log.warn(
                "Fant ikke oppfølgingsperiode med UUID: {}. (eller den er allerede avsluttet)",
                oppfolgingsperiodeUUID
            )
            return
        }

        val erSisteGjeldendePeriode = valgtGjeldendePeriode.uuid == sisteGjeldendePeriode.uuid
        val erEnesteGjeldendePeriode = gjeldendePerioder.size == 1

        if (erSisteGjeldendePeriode && erEnesteGjeldendePeriode) {
            log.info("Valgt oppfølgingsperiode er siste og eneste. Avslutter oppfølging.")
            adminAvsluttOppfolgingForBruker(avregistrering)
            return
        }

        // OBS: Avslutter en periode som ikke er siste periode eller er en av flere gjeldende (dårlig data),
        // Person beholder underoppfolging i oppfolgingstatus, sender bare perioden på oppfolgingsperiode v1
        // kafka topic og gjør ikke andre opprydninger. Publiserer ikke på siste-oppfolgingsperiode-v3
        val sluttDato = if (erSisteGjeldendePeriode) ZonedDateTime.now() else sisteGjeldendePeriode.startDato
        val avsluttetOppfolgingsperiode = oppfolgingsPeriodeRepository.avsluttOppfolgingsperiode(
            oppfolgingsperiodeUUID,
            avregistrering.avsluttetAv.getIdent(),
            avregistrering.begrunnelse,
            sluttDato,
            avregistrering.getAvregistreringsType(),
        )

        log.info(
            "Oppfølgingsperiode med UUID: {} avsluttet for bruker - publiserer endringer på oppfølgingsperiode-topics.",
            oppfolgingsperiodeUUID
        )
        kafkaProducerService.publiserValgtOppfolgingsperiode(
            DtoMappers.tilOppfolgingsperiodeDTO(
                avsluttetOppfolgingsperiode
            )
        )
        bigQueryClient.loggAvsluttOppfolgingsperiode(oppfolgingsperiodeUUID!!, avregistrering, null)
    }

    fun adminAvsluttOppfolgingForBruker(avregistrering: AdminAvregistrering) {
        avsluttOppfolgingForBruker(KunneAvsluttesOverstyring(avregistrering))
    }

    fun harAktiveTiltaksdeltakelser(fnr: Fnr) = tiltakshistorikkClient.harAktiveTiltaksdeltakelser(fnr.get())
    fun erDeltakerIUngdomsprogrammet(fnr: Fnr) = ungdomsprogramClient.erDeltakerIUngdomsprogrammet(fnr.get())
    fun erArbeidssoeker(fnr: Fnr) = arbeidssoekerregisteretClient.erArbeidssoeker(fnr.get())
    fun harAap(fnr: Fnr) = aapClient.harAap(fnr.get())
}
