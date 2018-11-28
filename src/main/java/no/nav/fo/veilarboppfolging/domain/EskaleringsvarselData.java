package no.nav.fo.veilarboppfolging.domain;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.Wither;

import java.util.Date;

@Value
@Builder
@Wither
public class EskaleringsvarselData {
    private long varselId;
    private String aktorId;
    private String opprettetAv;
    private Date opprettetDato;
    private String opprettetBegrunnelse;
    private Date avsluttetDato;
    private String avsluttetAv;
    private String avsluttetBegrunnelse;
    private long tilhorendeDialogId;
}
