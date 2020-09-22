package no.nav.veilarboppfolging.controller.domain;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.Wither;

import java.time.ZonedDateTime;

@Value
@Builder
@Wither
public class Eskaleringsvarsel {

    private long varselId;
    private String aktorId;
    private String opprettetAv;
    private ZonedDateTime opprettetDato;
    private ZonedDateTime avsluttetDato;
    private long tilhorendeDialogId;

}
