package no.nav.fo.veilarboppfolging.ws.provider.domene;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
public class Portefolje {
    List<Bruker> brukere;
}
