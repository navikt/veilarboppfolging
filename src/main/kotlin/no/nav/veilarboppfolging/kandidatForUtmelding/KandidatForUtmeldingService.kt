package no.nav.veilarboppfolging.kandidatForUtmelding

import no.nav.common.types.identer.AktorId
import no.nav.veilarboppfolging.oppfolgingsbruker.utgang.AvregistreringsType
import no.nav.veilarboppfolging.service.AvsluttOppfolgingService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class KandidatForUtmeldingService(
    private val avsluttOppfolgingService: AvsluttOppfolgingService,
    private val kandidatForUtmeldingRepository: KandidatForUtmeldingRepository
) {

    private val logger = LoggerFactory.getLogger(this::class.java)

    fun lagreKandidatForUtmelding(kandidatForUtmeldingHendelse: KandidatForUtmeldingHendelse) {
        // Vi sjekker avslutningsstatus for manuell avregistrering siden de bare blir kandidater for utmelding
        // Vi tar dem ikke ut av oppfølging automatisk
        val avslutningsstatus = avsluttOppfolgingService.hentAvslutningstatusForManuellAvslutning(kandidatForUtmeldingHendelse.fnr)

        if (avslutningsstatus.kanAvslutte) {
            kandidatForUtmeldingRepository.lagreKandidat(kandidatForUtmeldingHendelse)
            logger.info("Kandidat ble lagret fordi arbeidssøkerperiode ble avsluttet")
        } else {
            logger.info("Kandidat kunne ikke avsluttes selvom arbeidssøkerperiode ble avsluttet")
        }
    }

    fun fjernKandidatForUtmelding(aktorId: AktorId) {
        kandidatForUtmeldingRepository.fjernKandidat(aktorId)
        logger.info("Fjerner kandidat for utmelding")
    }

    /**
     * Foreløpig sletter vi ikke kandidatene, men lagrer timestamp for når de egentlig skulle slettes.
     * Dette er for å kunne samle data om hvilke kandidater som har blitt tatt ut av oppfølging enten automatisk
     * eller manuelt, og når de ble tatt ut av oppfølging.
     */
    fun markerOppfolgingSomAvsluttet(aktorId: AktorId, avregistreringsType: AvregistreringsType) {
        kandidatForUtmeldingRepository.markerAtOppfolgingBleAvsluttet(aktorId, avregistreringsType)
        logger.info("Markerer at kandidat for utmelding")
    }
}