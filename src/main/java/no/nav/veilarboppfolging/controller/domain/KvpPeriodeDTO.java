package no.nav.veilarboppfolging.controller.domain;

import lombok.Value;

import java.time.ZonedDateTime;

@Value
public class KvpPeriodeDTO {

    private ZonedDateTime opprettetDato;
    private ZonedDateTime avsluttetDato;

}
