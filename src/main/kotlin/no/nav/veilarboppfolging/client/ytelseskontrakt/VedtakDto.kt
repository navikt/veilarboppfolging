package no.nav.veilarboppfolging.client.ytelseskontrakt

data class VedtakDto(
    val vedtakstype: String?,
    val status: String?,
    val aktivitetsfase: String?,
    val rettighetsgruppe: String?,
    val fradato: Dato?,
    val tildato: Dato?,
)
