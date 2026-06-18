package no.nav.veilarboppfolging.service

import no.nav.common.types.identer.Fnr
import no.nav.veilarboppfolging.client.veilarbarena.VeilarbarenaClient
import no.nav.veilarboppfolging.client.ytelseskontrakt.Dato
import no.nav.veilarboppfolging.client.ytelseskontrakt.VedtakDto
import no.nav.veilarboppfolging.client.ytelseskontrakt.YtelseskontraktDto
import no.nav.veilarboppfolging.controller.response.YtelserResponse
import org.springframework.stereotype.Component
import java.time.LocalDate
import kotlin.jvm.optionals.getOrNull

private const val AKTIV_YTELSE_STATUS: String = "Aktiv"

@Component
class ArenaYtelserService(val veilarbarenaClient: VeilarbarenaClient) {

    open fun harPagaendeYtelse(fnr: Fnr): Boolean {
        return veilarbarenaClient.getArenaYtelser(fnr)
            .getOrNull()?.ytelser?.any { it.status == AKTIV_YTELSE_STATUS }
            ?: false
    }

    open fun hentYtelser(fnr: Fnr): YtelserResponse {
        val response = veilarbarenaClient.getArenaYtelser(fnr).orElseGet { throw Error("Finner ingen ytelser") }
        return YtelserResponse(
            vedtaksliste = response.vedtak.map {
                VedtakDto(
                    vedtakstype = it.type,
                    status = it.status,
                    aktivitetsfase = it.aktivitetsfase,
                    rettighetsgruppe = it.rettighetsgruppe,
                    fradato = it.fraDato.tilEgenDatoType(),
                    tildato = it.tilDato.tilEgenDatoType(),
                )
            },
            ytelser = response.ytelser.map {
                YtelseskontraktDto(
                    status = it.status,
                    ytelsestype = it.type,
                    datoMottatt = it.motattDato.tilEgenDatoType(),
                    datoFra = it.fraDato.tilEgenDatoType(),
                    datoTil = it.tilDato.tilEgenDatoType(),
                )
            }
        )
    }


    private fun LocalDate?.tilEgenDatoType(): Dato? {
        if (this == null) return null
        return Dato(this.year, this.monthValue, this.dayOfMonth)
    }
}