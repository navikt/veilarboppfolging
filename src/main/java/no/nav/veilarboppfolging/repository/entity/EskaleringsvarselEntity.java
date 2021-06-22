package no.nav.veilarboppfolging.repository.entity;

import lombok.Builder;
import lombok.Value;
import lombok.With;

import java.time.ZonedDateTime;

@Value
@Builder
@With
public class EskaleringsvarselEntity {
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
