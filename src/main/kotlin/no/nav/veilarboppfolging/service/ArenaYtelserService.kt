package no.nav.veilarboppfolging.service

import no.nav.common.types.identer.Fnr
import no.nav.veilarboppfolging.client.veilarbarena.VeilarbarenaClient
import no.nav.veilarboppfolging.client.ytelseskontrakt.Dato
import no.nav.veilarboppfolging.client.ytelseskontrakt.VedtakDto
import no.nav.veilarboppfolging.client.ytelseskontrakt.YtelseskontraktDto
import no.nav.veilarboppfolging.controller.response.YtelserResponse
import org.springframework.stereotype.Component
import java.time.LocalDate

private const val AKTIV_YTELSE_STATUS: String = "Aktiv"

@Component
class ArenaYtelserService(val veilarbarenaClient: VeilarbarenaClient) {

    open fun harPagaendeYtelse(fnr: Fnr): Boolean {
        return veilarbarenaClient.getArenaYtelser(fnr)
            .map { response -> response.ytelser.any { it.status == AKTIV_YTELSE_STATUS } }
            .orElse(false)
    }

    open fun hentYtelser(fnr: Fnr): YtelserResponse {
        val response = veilarbarenaClient.getArenaYtelser(fnr).orElseGet { throw Error("Finner ingen ytelser") }
        return YtelserResponse().withYtelser(
            response.ytelser.map {
                YtelseskontraktDto()
                    .withStatus(it.status)
                    .withYtelsestype(it.type)
                    .withDatoMottatt(it.motattDato)
                    .withDatoFra(it.fraDato)
                    .withDatoTil(it.tilDato)
            }).withVedtaksliste(
                response.vedtak.map {
                    VedtakDto()
                        .setStatus(it.status)
                        .setFradato(it.fraDato.tilEgenDatoType())
                        .setTildato(it.tilDato.tilEgenDatoType())
                        .setVedtakstype(it.type)
                        .setAktivitetsfase(it.aktivitetsfase)
                        .setRettighetsgruppe(it.rettighetsgruppe)
                }
            )
    }


    private fun LocalDate.tilEgenDatoType(): Dato {
        return Dato(this.year, this.monthValue, this.dayOfMonth)
    }
}