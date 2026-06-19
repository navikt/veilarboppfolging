package no.nav.veilarboppfolging.service

import kotlin.jvm.optionals.getOrNull
import no.nav.common.types.identer.Fnr
import no.nav.veilarboppfolging.client.veilarbarena.VeilarbarenaClient
import org.springframework.stereotype.Component

private const val AKTIV_YTELSE_STATUS: String = "Aktiv"

@Component
class ArenaYtelserService(val veilarbarenaClient: VeilarbarenaClient) {

    open fun harPagaendeYtelse(fnr: Fnr): Boolean {
        return veilarbarenaClient.getArenaYtelser(fnr)
            .getOrNull()?.ytelser?.any { it.status == AKTIV_YTELSE_STATUS }
            ?: false
    }
}