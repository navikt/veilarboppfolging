package no.nav.veilarboppfolging.kafka;

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import jakarta.validation.constraints.NotBlank

@JsonInclude(JsonInclude.Include.NON_NULL)
data class AoMinSideMicrofrontendMessage @JvmOverloads constructor(
    @JsonProperty("@action")
    val action: Action,

    @field:NotBlank
    val ident: String,

    val sensitivitet: String ?= null, // Kun for "enable"

    @field:NotBlank
    val microfrontendId: String = "ao-min-side-microfrontend",

    @JsonProperty("@initiated_by")
    @field:NotBlank
    val initiatedBy: String = "dab",

) {
    enum class Action(@JsonValue val verdi: String) {
        ENABLE("enable"),
        DISABLE("disable")
    }
}