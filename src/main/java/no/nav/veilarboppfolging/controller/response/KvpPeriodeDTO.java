package no.nav.veilarboppfolging.controller.response;

import lombok.Value;

import java.time.ZonedDateTime;

@Value
public class KvpPeriodeDTO {
    ZonedDateTime opprettetDato;
    ZonedDateTime avsluttetDato;
}
