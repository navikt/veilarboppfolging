package no.nav.veilarboppfolging.kandidatForUtmelding

import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.veilarboppfolging.service.AvsluttOppfolgingService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

sealed class KandidatForUtmeldingHendelse (
    val aktorId : AktorId,
    val fnr: Fnr,
) {
    abstract val type: KandidatForUtmeldingHendelseType
}

enum class KandidatForUtmeldingHendelseType {
    ARBEIDSSOKERPERIODE_AVSLUTTET
}

class ArbeidssøkerPeriodeAvsluttet(
    aktorId: AktorId,
    fnr: Fnr,
): KandidatForUtmeldingHendelse(aktorId, fnr)  {
    override val type: KandidatForUtmeldingHendelseType = KandidatForUtmeldingHendelseType.ARBEIDSSOKERPERIODE_AVSLUTTET
}

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
}