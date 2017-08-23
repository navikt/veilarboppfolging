package no.nav.fo.veilarbsituasjon.domain;

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
    private Date avsluttetDato;
    private long tilhorendeDialogId;
}
