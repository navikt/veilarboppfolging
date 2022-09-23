package no.nav.veilarboppfolging.repository.entity;

import lombok.Builder;
import lombok.Value;

import java.time.ZonedDateTime;

@Value
@Builder
public class VeilederTilordningHistorikkEntity {
    String veileder;
    ZonedDateTime sistTilordnet;
    String tilordnetAvVeileder;
}
