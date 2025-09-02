package no.nav.veilarboppfolging.service

import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.pto_schema.enums.arena.Kvalifiseringsgruppe
import no.nav.veilarboppfolging.client.digdir_krr.KRRData
import no.nav.veilarboppfolging.eventsLogger.BigQueryClient
import no.nav.veilarboppfolging.oppfolgingsbruker.arena.ArenaOppfolgingService
import no.nav.veilarboppfolging.oppfolgingsbruker.inngang.ArenaSyncRegistrering
import no.nav.veilarboppfolging.oppfolgingsbruker.inngang.ManuellRegistrering
import no.nav.veilarboppfolging.oppfolgingsbruker.inngang.OppfolgingsRegistrering
import no.nav.veilarboppfolging.oppfolgingsperioderHendelser.hendelser.OppfolgingStartetHendelseDto
import no.nav.veilarboppfolging.repository.OppfolgingsPeriodeRepository
import no.nav.veilarboppfolging.repository.OppfolgingsStatusRepository
import no.nav.veilarboppfolging.repository.entity.OppfolgingEntity
import no.nav.veilarboppfolging.repository.entity.OppfolgingsperiodeEntity
import no.nav.veilarboppfolging.utils.DtoMappers
import no.nav.veilarboppfolging.utils.OppfolgingsperiodeUtils
import no.nav.veilarboppfolging.utils.SecureLog
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import java.util.*

@Service
open class StartOppfolgingService(
    val manuellStatusService: ManuellStatusService,
    val oppfolgingsStatusRepository: OppfolgingsStatusRepository,
    val oppfolgingsPeriodeRepository: OppfolgingsPeriodeRepository,
    val kafkaProducerService: KafkaProducerService,
    val bigQueryClient: BigQueryClient,
    val transactor: TransactionTemplate,
    val arenaOppfolgingService: ArenaOppfolgingService,
    @Value("\${app.env.nav-no-url}")
    val navNoUrl: String
) {
    val log = LoggerFactory.getLogger(StartOppfolgingService::class.java)

    fun startOppfolgingHvisIkkeAlleredeStartet(oppfolgingsbruker: OppfolgingsRegistrering) {
        val aktorId = oppfolgingsbruker.aktorId
        val fnr = oppfolgingsbruker.fnr
        val kontaktinfo = manuellStatusService.hentDigdirKontaktinfo(fnr)

        transactor.executeWithoutResult { _ ->
            val maybeOppfolging = oppfolgingsStatusRepository.hentOppfolging(aktorId)
            val erUnderOppfolging = maybeOppfolging
                .map { obj: OppfolgingEntity? -> obj!!.isUnderOppfolging }
                .orElse(false)

            if (erUnderOppfolging) return@executeWithoutResult
            if (maybeOppfolging.isEmpty) {
                // Siden det blir gjort mange kall samtidig til flere noder kan det oppstå en race condition
                // hvor oppfølging har blitt insertet av en annen node etter at den har sjekket at oppfølging
                // ikke ligger i databasen.
                try {
                    oppfolgingsStatusRepository.opprettOppfolging(aktorId)
                } catch (e: DuplicateKeyException) {
                    SecureLog.secureLog.warn(
                        "Race condition oppstod under oppretting av ny oppfølging for bruker: {}",
                        aktorId
                    )
                    return@executeWithoutResult
                }
            }

            oppfolgingsPeriodeRepository.start(oppfolgingsbruker)

            val perioder = oppfolgingsPeriodeRepository.hentOppfolgingsperioder(aktorId)
            val sistePeriode = OppfolgingsperiodeUtils.hentSisteOppfolgingsperiode(perioder)
            val kontorSattAvVeileder = if (oppfolgingsbruker is ManuellRegistrering) (oppfolgingsbruker.kontorSattAvVeileder) else null

            log.info("Oppfølgingsperiode startet for bruker - publiserer endringer på oppfølgingsperiode-topics.")
            kafkaProducerService.publiserOppfolgingsperiode(DtoMappers.tilOppfolgingsperiodeDTO(sistePeriode))
            kafkaProducerService.publiserVisAoMinSideMicrofrontend(aktorId, fnr)

//            val arenaKontor = when ()
            kafkaProducerService.publiserOppfolgingsStartet(lagOppfolgingStartetHendelseDto(fnr, sistePeriode, "123", kontorSattAvVeileder))
            publiserMinSideBeskjedHvisIkkeReservert(kontaktinfo, aktorId, fnr)

            bigQueryClient.loggStartOppfolgingsperiode(
                oppfolgingsbruker.oppfolgingStartBegrunnelse,
                sistePeriode.uuid,
                oppfolgingsbruker.registrertAv.getType(),
                Optional.ofNullable(getKvalifiseringsGruppe(oppfolgingsbruker))
            )
        }
    }

    private fun publiserMinSideBeskjedHvisIkkeReservert(kontaktinfo: KRRData, aktorId: AktorId, fnr: Fnr) {
        when {
            kontaktinfo.isReservert -> manuellStatusService.settBrukerTilManuellGrunnetReservertIKRR(aktorId)
            else -> {
                kafkaProducerService.publiserMinSideBeskjed(
                    fnr,
                    "Du er nå under arbeidsrettet oppfølging hos Nav. Se detaljer på MinSide.",
                    String.format("%s/minside", navNoUrl)
                )
            }
        }
    }

    private fun getKvalifiseringsGruppe(oppfolgingsbruker: OppfolgingsRegistrering?): Kvalifiseringsgruppe? {
        return when (oppfolgingsbruker) {
            is ArenaSyncRegistrering -> oppfolgingsbruker.kvalifiseringsgruppe as Kvalifiseringsgruppe?
            else -> null
        }
    }

    private fun lagOppfolgingStartetHendelseDto(
        fnr: Fnr,
        oppfølgingsperiode: OppfolgingsperiodeEntity,
        arenaKontor: String,
        arbeidsoppfolgingskontor: String?
    ): OppfolgingStartetHendelseDto {
        return OppfolgingStartetHendelseDto(
            oppfolgingsPeriodeId = oppfølgingsperiode.uuid,
            startetTidspunkt = oppfølgingsperiode.startDato,
            startetAv = oppfølgingsperiode.startetAv
                ?: throw IllegalStateException("Dette skal aldri skje, alle nystartede oppfølgingsperioder har 'startetAv'"),
            startetAvType = oppfølgingsperiode.startetAvType?.name
                ?: throw IllegalStateException("Dette skal aldri skje, alle nystartede oppfølgingsperioder har 'startetAvType'"),
            startetBegrunnelse = oppfølgingsperiode.startetBegrunnelse?.name
                ?: throw IllegalStateException("Dette skal aldri skje, alle nystartede oppfølgingsperioder har 'startetBegrunnelse'"),
            arenaKontor = arenaKontor,
            arbeidsoppfolgingsKontorSattAvVeileder = arbeidsoppfolgingskontor,
            fnr = fnr.get()
        )
    }
}