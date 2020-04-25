package no.nav.fo.veilarboppfolging.rest.domain;

import lombok.Data;
import lombok.experimental.Accessors;
import no.nav.dialogarena.aktor.AktorService;
import no.nav.fo.veilarboppfolging.domain.AktorId;

@Data
@Accessors(chain = true)
public class VeilederTilordning {
    String brukerFnr;
    String aktoerId;
    String innloggetVeilederId;
    String fraVeilederId;
    String tilVeilederId;

    public AktorId toAktorId() {
        return new AktorId(aktoerId);
    }
}
