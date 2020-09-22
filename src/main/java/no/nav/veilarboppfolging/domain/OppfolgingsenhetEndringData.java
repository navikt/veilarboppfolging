package no.nav.veilarboppfolging.domain;

import lombok.Builder;
import lombok.Value;

import java.time.ZonedDateTime;

@Value
@Builder
public class OppfolgingsenhetEndringData {
    private String enhet;
    private ZonedDateTime endretDato;
}
