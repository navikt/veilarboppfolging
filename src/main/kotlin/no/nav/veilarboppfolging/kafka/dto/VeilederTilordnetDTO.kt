package no.nav.veilarboppfolging.kafka.dto

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.ZonedDateTime

data class VeilederTilordnetDTO @JsonCreator constructor(
    @JsonProperty("aktorId")
    val aktorId: String,

    @JsonProperty("veilederId")
    val veilederId: String? = null,

    @JsonProperty("tilordnet")
    val tilordnet: ZonedDateTime? = null
)
