package no.nav.veilarboppfolging.controller.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import no.nav.common.types.identer.Fnr;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReaktiverBrukerRequest {
    Fnr fnr;
}
