package no.nav.veilarboppfolging.client.ytelseskontrakt

data class YtelseskontraktDto(
    val status: String?,
    val ytelsestype: String?,
    val datoMottatt: Dato?,
    val datoFra: Dato?,
    val datoTil: Dato?,
)
