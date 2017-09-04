package no.nav.fo.veilarbsituasjon.rest.domain;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class EndreSituasjonDTO {
    public String veilederId;
    public String begrunnelse;
}
