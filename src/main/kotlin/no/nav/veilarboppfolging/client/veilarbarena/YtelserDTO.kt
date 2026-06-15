package no.nav.veilarboppfolging.client.veilarbarena

import java.time.LocalDate


data class YtelserDTO(
    val vedtak: List<Vedtak>,
    var ytelser: List<Ytelseskontrakt>,
)

data class Vedtak (
    var type: String?,
    var status: String?,
    var aktivitetsfase: String?,
    var rettighetsgruppe: String?,
    var fraDato: LocalDate?,
    var tilDato: LocalDate?,
)

data class Ytelseskontrakt (
    var type: String?,
    var status: String?,
    var motattDato: LocalDate?,
    var fraDato: LocalDate?,
    var tilDato: LocalDate?,
)

