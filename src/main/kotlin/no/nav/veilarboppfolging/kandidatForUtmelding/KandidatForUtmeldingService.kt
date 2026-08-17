package no.nav.veilarboppfolging.kandidatForUtmelding

import java.util.UUID
import kotlin.jvm.optionals.getOrNull
import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.veilarboppfolging.repository.OppfolgingsPeriodeRepository
import no.nav.veilarboppfolging.service.AvsluttOppfolgingService
import no.nav.veilarboppfolging.service.KafkaProducerService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate

@Service
class KandidatForUtmeldingService(
    private val avsluttOppfolgingService: AvsluttOppfolgingService,
    private val kandidatForUtmeldingRepository: KandidatForUtmeldingRepository,
    private val oppfolgingsPeriodeRepository: OppfolgingsPeriodeRepository,
    private val transactor: TransactionTemplate,
    private val kafkaProducerService: KafkaProducerService,
    @Value("\${app.sendUtmeldingskandidaterTilObo}") private val sendUtmeldingskandidaterTilObo: Boolean,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun lagreKandidatForUtmelding(fnr: Fnr, kandidatForUtmeldingHendelse: KandidatForUtmeldingHendelse) {
        // Vi sjekker avslutningsstatus for manuell avregistrering siden de bare blir kandidater for utmelding
        // Vi tar dem ikke ut av oppfølging automatisk
        transactor.executeWithoutResult { _ ->
            val avslutningsstatus = avsluttOppfolgingService.hentAvslutningstatusForManuellAvslutning(fnr)

            if (avslutningsstatus.kanAvslutte) {
                kandidatForUtmeldingRepository.lagreKandidat(kandidatForUtmeldingHendelse)
                logger.info("Kandidat ble lagret fordi arbeidssøkerperiode ble avsluttet, oppfølgingsperiode ${kandidatForUtmeldingHendelse.oppfolgingsperiodeUuid}")

                if (sendUtmeldingskandidaterTilObo) {
                    val filterkategoriPersonId = kandidatForUtmeldingRepository.hentEllerOpprettFilterhendelseId(kandidatForUtmeldingHendelse.oppfolgingsperiodeUuid)
                    logger.info("Sender kandidat for utmelding til OBO med key=$filterkategoriPersonId for oppfølgingsperiode ${kandidatForUtmeldingHendelse.oppfolgingsperiodeUuid}")
                    val filterhendelse = kandidatForUtmeldingHendelse.tilFilterhendelseRecord(fnr)
                    kafkaProducerService.publiserFilterhendelse(filterkategoriPersonId, filterhendelse)
                } else {
                    logger.info("Sender ikke kandidat for utmelding til OBO for oppfølgingsperiode ${kandidatForUtmeldingHendelse.oppfolgingsperiodeUuid} fordi sending til OBO er togglet av")
                }
            } else {
                logger.info("Kandidat kunne ikke avsluttes selvom arbeidssøkerperiode ble avsluttet, oppfølgingsperiode ${kandidatForUtmeldingHendelse.oppfolgingsperiodeUuid}")
            }
        }
    }

    /**
     * Foreløpig sletter vi ikke kandidatene når kandidatene avslutter sin oppfølgingsperiode, kun når de starter ny periode.
     * Dette er for å kunne samle data om hvilke kandidater som har blitt tatt ut av oppfølging enten automatisk
     * eller manuelt, og når de ble tatt ut av oppfølging.
     *
     * Fjerner ikke kandidater fra tabellen når bruker reaktiveres. Disse filtreres nå bort i hentKandidat spørringen
     */
    fun fjernKandidatForUtmelding(oppfolgingsperiodeId: UUID) {
        kandidatForUtmeldingRepository.fjernKandidat(oppfolgingsperiodeId)
        logger.info("Fjerner kandidat for utmelding")
    }

    fun hentKandidatForUtmeldingTag(oppfolgingsperiodeId: UUID): KandidatForUtmeldingTag? {
        return kandidatForUtmeldingRepository.hentKandidat(oppfolgingsperiodeId)?.mapTilTag()
    }

    fun hentKandidatForUtmeldingTag(aktorId: AktorId): KandidatForUtmeldingTag? {
        val oppfolgingsperiodeId = oppfolgingsPeriodeRepository.hentGjeldendeOppfolgingsperiode(aktorId)?.getOrNull()?.uuid ?: return null
        return hentKandidatForUtmeldingTag(oppfolgingsperiodeId)
    }
}