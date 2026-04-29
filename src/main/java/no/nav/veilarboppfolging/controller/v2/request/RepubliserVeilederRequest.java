package no.nav.veilarboppfolging.controller.v2.request;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record RepubliserVeilederRequest(
        @JsonProperty(required = true) List<String> aktorIder
) {
}
