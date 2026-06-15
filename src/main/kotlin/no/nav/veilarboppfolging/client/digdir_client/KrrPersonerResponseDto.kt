package no.nav.veilarboppfolging.client.digdir_client

import no.nav.veilarboppfolging.client.digdir_krr.DigdirKontaktinfo
import no.nav.veilarboppfolging.client.digdir_krr.KRRData
import org.slf4j.LoggerFactory
import java.util.*

class KrrPersonerResponseDto {
    var personer: MutableMap<String?, DigdirKontaktinfo?>? = null
    var feil: MutableMap<String, String?>? = null

    companion object {
        val log = LoggerFactory.getLogger(KrrPersonerResponseDto::class.java)
    }

    fun assertSinglePersonToKrrData(): Optional<KRRData> {
        if (feil?.isNotEmpty() ?: false) {
            val personident = feil!!.keys.stream().findFirst().get()
            log.warn("Kunne ikke hente kontaktinfo fra KRR, feil: {}", feil!!.get(personident))
            return Optional.empty()
        }
        if (personer == null || personer!!.size != 1) {
            log.warn("Fant ikke person i response fra KRR")
            return Optional.empty()
        }
        val key = personer?.keys?.firstOrNull()
        return key?.let { personer?.get(it) }
            ?.let { Optional.of(it.toKrrData()) } ?: Optional.empty()
    }
}
