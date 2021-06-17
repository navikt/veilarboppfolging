package no.nav.veilarboppfolging.controller.response;

import lombok.Builder;
import lombok.Value;
import lombok.With;

import java.time.ZonedDateTime;

@Value
@Builder
@With
public class Eskaleringsvarsel {
    long varselId;
    String aktorId;
    String opprettetAv;
    ZonedDateTime opprettetDato;
    ZonedDateTime avsluttetDato;
    long tilhorendeDialogId;
}
