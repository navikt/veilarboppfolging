package no.nav.veilarboppfolging.domain;

import lombok.Builder;
import lombok.Value;

import java.time.ZonedDateTime;

@Value
@Builder
public class VeilederTilordningerData {
    private String veileder;
    private ZonedDateTime sistTilordnet;
}
