package no.nav.fo.veilarbsituasjon.domain;

import lombok.Builder;
import lombok.Value;

import java.util.Date;

@Value
@Builder
public class EskaleringstatusData {
    private long varselId;
    private String aktorId;
    private String opprettetAv;
    private Date opprettetDato;
    private Date avsluttetDato;
    private long tilhorendeDialogId;
}
