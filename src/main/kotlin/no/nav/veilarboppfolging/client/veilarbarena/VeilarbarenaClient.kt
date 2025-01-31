package no.nav.veilarboppfolging.client.veilarbarena

import no.nav.common.health.HealthCheck
import no.nav.common.types.identer.Fnr
import java.util.*

interface VeilarbarenaClient : HealthCheck {
    fun hentOppfolgingsbruker(fnr: Fnr): Optional<VeilarbArenaOppfolgingsBruker>
    fun getArenaOppfolgingsstatus(fnr: Fnr): Optional<VeilarbArenaOppfolgingsStatus>
    fun getArenaYtelser(fnr: Fnr): Optional<YtelserDTO>
    fun registrerIkkeArbeidsoker(fnr: Fnr): RegistrerIArenaResult
}
