package no.nav.fo.veilarbsituasjon.domain;

import lombok.Builder;
import lombok.Value;

import java.util.Date;

@Value
@Builder
public class InnstillingsHistorikk {
    private String beskrivelse;
    private String begrunnelse;
    private Date tidspunkt;
    private KodeverkBruker opptettetAv;
    private String opprettetAvBrukerId;
}
