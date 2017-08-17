package no.nav.fo.veilarbsituasjon.rest.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.Accessors;

import java.util.Date;

@Value
@Builder(toBuilder = true)
@AllArgsConstructor
@Accessors(chain = true)
public class Eskaleringstatus {

    private int varselId;
    private String aktorId;
    private String opprettetAv;
    private Date opprettetDato;
    private Date avsluttetDato;
    private int tilhorendeDialogId;

}
