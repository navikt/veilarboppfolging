package no.nav.fo.veilarbsituasjon.domain;

import lombok.Builder;
import lombok.Value;

import java.util.Date;

@Value
@Builder
public class EskaleringstatusData {
    private int varselId;
    private String aktorId;
    private String opprettetAv;
    private Date opprettetDato;
    private Date avsluttetDato;
    private int tilhorendeDialogId;
    private boolean gjeldende;
}
