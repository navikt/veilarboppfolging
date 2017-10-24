package no.nav.fo.veilarbsituasjon.ws.provider.veiledertildeling.domene;

import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@Data
@Accessors(chain = true)
public class Bruker {
    String fnr;
    String veilederId;
}
