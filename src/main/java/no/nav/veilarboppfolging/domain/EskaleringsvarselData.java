package no.nav.veilarboppfolging.domain;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.Wither;

import java.time.ZonedDateTime;

@Value
@Builder
@Wither
public class EskaleringsvarselData {
    private long varselId;
    private String aktorId;
    private String opprettetAv;
    private ZonedDateTime opprettetDato;
    private String opprettetBegrunnelse;
    private ZonedDateTime avsluttetDato;
    private String avsluttetAv;
    private String avsluttetBegrunnelse;
    private long tilhorendeDialogId;
}
