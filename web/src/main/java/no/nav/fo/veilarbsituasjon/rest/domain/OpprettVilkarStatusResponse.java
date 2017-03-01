package no.nav.fo.veilarbsituasjon.rest.domain;

import lombok.Data;
import lombok.experimental.Accessors;
import no.nav.fo.veilarbsituasjon.domain.VilkarStatus;

@Data
@Accessors(chain = true)
public class OpprettVilkarStatusResponse {
    public String fnr;
    public VilkarStatus status;
}