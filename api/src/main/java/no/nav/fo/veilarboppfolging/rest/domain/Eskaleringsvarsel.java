package no.nav.fo.veilarboppfolging.rest.domain;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.Wither;

import java.util.Date;

@Value
@Builder
@Wither
public class Eskaleringsvarsel {

    private long varselId;
    private String aktorId;
    private String opprettetAv;
    private Date opprettetDato;
    private Date avsluttetDato;
    private long tilhorendeDialogId;

}
