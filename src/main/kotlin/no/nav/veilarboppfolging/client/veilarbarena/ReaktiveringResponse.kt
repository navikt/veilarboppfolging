package no.nav.veilarboppfolging.client.veilarbarena

sealed class ReaktiveringResult

data class ReaktiveringResponse(
    val ok: Boolean,
    val kode: REAKTIVERING_RESULTAT
)

class ReaktiveringError(val message: String, val throwable: Throwable): ReaktiveringResult()
class ReaktiveringSuccess(val reaktiveringResponse: ReaktiveringResponse): ReaktiveringResult()

enum class REAKTIVERING_RESULTAT {
    OK_REGISTRERT_I_ARENA,
    FNR_FINNES_IKKE,
    KAN_REAKTIVERES_FORENKLET,
    BRUKER_ALLEREDE_ARBS,
    BRUKER_ALLEREDE_IARBS,
    UKJENT_FEIL,
    KAN_IKKE_REAKTIVERES
}