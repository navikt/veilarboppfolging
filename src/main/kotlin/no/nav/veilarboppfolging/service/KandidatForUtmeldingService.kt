package no.nav.veilarboppfolging.service

import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import org.springframework.stereotype.Service

sealed class KandidatForUtmeldingHendelse (
    val aktorId : AktorId,
    val fnr: Fnr,
)

class ArbeidssøkerPeriodeAvsluttet(aktorId: AktorId, fnr: Fnr): KandidatForUtmeldingHendelse(aktorId, fnr)

@Service
class KandidatForUtmeldingService(
    private val avsluttOppfolgingService: AvsluttOppfolgingService
) {

    fun lagreKandidatForUtmelding(kandidatForUtmeldingHendelse: KandidatForUtmeldingHendelse) {
        // Vi sjekker avslutningsstatus for manuell avregistrering siden de bare blir kandidater for utmelding
        // Vi tar dem ikke ut av oppfølging automatisk
        val avslutningsstatus = avsluttOppfolgingService.hentAvslutningstatusForManuellAvslutning(kandidatForUtmeldingHendelse.fnr)

    }
}