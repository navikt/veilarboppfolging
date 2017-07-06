package no.nav.fo.veilarbsituasjon.domain;

import lombok.Builder;
import lombok.Value;

import java.util.Date;

@Value
@Builder
public class InnstillingsHistorikk {
    private boolean manuell;
    private Date dato;
    private String begrunnelse;
    private KodeverkBruker opprettetAv;
    private String opprettetAvBrukerId;
}
