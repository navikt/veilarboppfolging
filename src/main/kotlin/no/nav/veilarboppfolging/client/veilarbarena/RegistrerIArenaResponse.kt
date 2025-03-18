package no.nav.veilarboppfolging.client.veilarbarena

sealed class RegistrerIArenaResult

enum class ARENA_REGISTRERING_RESULTAT {
    OK_REGISTRERT_I_ARENA,
    FNR_FINNES_IKKE,
    KAN_REAKTIVERES_FORENKLET,
    BRUKER_ALLEREDE_ARBS,
    BRUKER_ALLEREDE_IARBS,
    UKJENT_FEIL
}

data class RegistrerIkkeArbeidssokerDto(
    val resultat: String,
    var kode: ARENA_REGISTRERING_RESULTAT
)

class RegistrerIArenaSuccess(val arenaResultat: RegistrerIkkeArbeidssokerDto): RegistrerIArenaResult()
class RegistrerIArenaError(val message: String, val throwable: Throwable): RegistrerIArenaResult()
