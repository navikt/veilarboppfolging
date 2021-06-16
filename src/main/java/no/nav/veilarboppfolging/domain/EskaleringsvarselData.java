package no.nav.veilarboppfolging.domain;

import lombok.Builder;
import lombok.Value;
import lombok.With;

import java.time.ZonedDateTime;

@Value
@Builder
@With
public class EskaleringsvarselData {
    long varselId;
    String aktorId;
    String opprettetAv;
    ZonedDateTime opprettetDato;
    String opprettetBegrunnelse;
    ZonedDateTime avsluttetDato;
    String avsluttetAv;
    String avsluttetBegrunnelse;
    long tilhorendeDialogId;
}
