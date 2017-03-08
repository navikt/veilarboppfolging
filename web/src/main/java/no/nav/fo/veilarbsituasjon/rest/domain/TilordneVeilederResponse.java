package no.nav.fo.veilarbsituasjon.rest.domain;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
public class TilordneVeilederResponse {
    String resultat;
    List<VeilederTilordning> feilendeTilordninger;
}
