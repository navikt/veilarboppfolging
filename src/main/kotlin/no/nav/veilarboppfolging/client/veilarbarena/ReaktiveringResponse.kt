package no.nav.veilarboppfolging.client.veilarbarena

sealed class ReaktiveringResult

data class ReaktiveringResponse(
    val ok: Boolean,
    val kode: ReaktiveringResultat
)

class ReaktiveringSuccess(val reaktiveringResponse: ReaktiveringResponse): ReaktiveringResult()
class ReaktiveringError(val message: String, val throwable: Throwable): ReaktiveringResult()

enum class ReaktiveringResultat {
    OK_REGISTRERT_I_ARENA,
    FNR_FINNES_IKKE,
    KAN_REAKTIVERES_FORENKLET,
    BRUKER_ALLEREDE_ARBS,
    BRUKER_ALLEREDE_IARBS,
    UKJENT_FEIL,
    KAN_IKKE_REAKTIVERES
}