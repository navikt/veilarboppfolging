package no.nav.veilarboppfolging.service

import no.nav.common.types.identer.Fnr
import no.nav.veilarboppfolging.client.veilarbarena.VeilarbarenaClient
import org.springframework.stereotype.Component

private const val AKTIV_YTELSE_STATUS: String = "Aktiv"
@Component
class ArenaYtelserService(val veilarbarenaClient: VeilarbarenaClient) {

    open fun harPagaendeYtelse(fnr: Fnr): Boolean {
        return veilarbarenaClient.getArenaYtelser(fnr)
            .map { response -> response.ytelser.any { it.status == AKTIV_YTELSE_STATUS} }
            .orElse(false)
    }
}