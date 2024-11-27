package no.nav.veilarboppfolging.oppfolgingsbruker.arena


sealed class GetOppfolginsstatusResult
class GetOppfolginsstatusSuccess(
    val result: OppfolgingEnhetMedVeilederResponse
): GetOppfolginsstatusResult()
class GetOppfolginsstatusFailure(
    val error: Exception
): GetOppfolginsstatusResult()


data class OppfolgingEnhetMedVeilederResponse(
     val oppfolgingsenhet: Oppfolgingsenhet,
     val veilederId: String,
     val formidlingsgruppe: String,
     val servicegruppe: String,
     val hovedmaalkode: String,
) {
    data class Oppfolgingsenhet(
        val navn: String,
        val enhetId: String,
    )
}


