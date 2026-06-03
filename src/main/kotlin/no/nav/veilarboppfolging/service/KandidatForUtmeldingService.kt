package no.nav.veilarboppfolging.service

import no.nav.veilarboppfolging.ident.AktorId
import org.springframework.stereotype.Service

sealed class KandidatHendelse (
    val aktorId : AktorId,
)

class ArbeidssøkerAvsluttet(aktorId: AktorId): KandidatHendelse(aktorId)

@Service
class KandidatForUtmeldingService {

    fun lagreKandidatForUtmelding(aktorId: AktorId) {

    }
}