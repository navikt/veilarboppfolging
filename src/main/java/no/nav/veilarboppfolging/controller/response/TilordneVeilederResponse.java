package no.nav.veilarboppfolging.controller.response;

import lombok.Data;
import lombok.experimental.Accessors;
import no.nav.veilarboppfolging.controller.request.VeilederTilordning;

import java.util.List;

@Data
@Accessors(chain = true)
public class TilordneVeilederResponse {
    String resultat;
    List<VeilederTilordning> feilendeTilordninger;
}
