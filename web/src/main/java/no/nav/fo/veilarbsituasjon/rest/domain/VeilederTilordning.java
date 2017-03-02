package no.nav.fo.veilarbsituasjon.rest.domain;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class VeilederTilordning {
    String brukerFnr;
    String fraVeilederId;
    String tilVeilederId;
}
