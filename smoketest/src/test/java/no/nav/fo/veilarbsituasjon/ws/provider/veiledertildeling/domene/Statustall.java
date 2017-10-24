package no.nav.fo.veilarbsituasjon.ws.provider.veiledertildeling.domene;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class Statustall {
    private long totalt;
}