package no.nav.veilarboppfolging.client.veilarbarena

sealed class ReaktiveringResult

class ReaktiveringSuccess(val kode: ArenaRegistreringResultat): ReaktiveringResult()

sealed class ReaktiveringError(val message: String): ReaktiveringResult()

object AlleredeUnderoppfolgingError: ReaktiveringError("Allerede under oppfølging")
class FeilFraArenaError(val arenaResultat: ArenaRegistreringResultat): ReaktiveringError("Kunne ikke starte reaktivering av bruker i Arena: ${arenaResultat.name}")
class UkjentFeilUnderReaktiveringError(message: String, val throwable: Throwable): ReaktiveringError(message)
