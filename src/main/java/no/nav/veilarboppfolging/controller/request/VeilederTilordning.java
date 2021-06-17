package no.nav.veilarboppfolging.controller.request;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class VeilederTilordning {
    String brukerFnr;
    String aktoerId;
    String innloggetVeilederId;
    String fraVeilederId;
    String tilVeilederId;
}
