package no.nav.veilarboppfolging.controller.domain;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class DkifResponse {
    private boolean krr;
    private boolean kanVarsles;
}