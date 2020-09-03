package no.nav.veilarboppfolging.controller.domain;

import lombok.Value;

import java.util.Date;

@Value
public class KvpPeriodeDTO {

    private Date opprettetDato;
    private Date avsluttetDato;

}
