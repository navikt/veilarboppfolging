package no.nav.veilarboppfolging.controller.response

import no.nav.veilarboppfolging.client.ytelseskontrakt.VedtakDto
import no.nav.veilarboppfolging.client.ytelseskontrakt.YtelseskontraktDto

data class YtelserResponse(
    val vedtaksliste: List<VedtakDto>,
    val ytelser: List<YtelseskontraktDto?>,
)

