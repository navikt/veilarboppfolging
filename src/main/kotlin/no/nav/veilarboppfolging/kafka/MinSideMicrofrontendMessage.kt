package no.nav.veilarboppfolging.kafka;

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.NotBlank

@JsonInclude(JsonInclude.Include.NON_NULL)
data class MinSideMicrofrontendMessage @JvmOverloads constructor(
    @JsonProperty("@action")
    val action: String,

    @field:NotBlank
    val ident: String,

    val sensitivitet: String ?= null, // Kun for "enable"

    @JsonProperty("microfrontend_id")
    @field:NotBlank
    val microfrontendId: String,

    @JsonProperty("@initiated_by")
    @field:NotBlank
    val initiatedBy: String = "dab",

)