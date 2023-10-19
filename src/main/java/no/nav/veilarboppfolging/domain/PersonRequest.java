package no.nav.veilarboppfolging.domain;

import lombok.Data;
import no.nav.common.types.identer.Fnr;

@Data
public class PersonRequest {
    Fnr fnr;
}
