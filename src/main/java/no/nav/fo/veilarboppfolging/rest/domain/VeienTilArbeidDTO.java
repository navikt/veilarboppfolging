package no.nav.fo.veilarboppfolging.rest.domain;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class VeienTilArbeidDTO {
    private boolean reservasjonKRR;
    private boolean underOppfolging;
    private Boolean kanReaktiveres;
    private String servicegruppe;
}
