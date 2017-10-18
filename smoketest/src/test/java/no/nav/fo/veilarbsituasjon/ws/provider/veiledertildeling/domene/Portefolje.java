package no.nav.fo.veilarbsituasjon.ws.provider.veiledertildeling.domene;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
public class Portefolje {
    List<Bruker> brukere;
}
