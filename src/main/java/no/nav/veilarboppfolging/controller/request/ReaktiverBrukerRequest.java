package no.nav.veilarboppfolging.controller.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReaktiverBrukerRequest {
    no.nav.common.types.identer.Fnr fnr;
}
