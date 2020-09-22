package no.nav.veilarboppfolging.domain;

import lombok.Builder;
import lombok.Value;

import java.time.ZonedDateTime;

@Value
@Builder
public class AvsluttetOppfolgingFeedData {

    public String aktoerid;
    public ZonedDateTime sluttdato;
    public ZonedDateTime oppdatert;
}
