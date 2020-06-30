package no.nav.veilarboppfolging.rest.domain;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class VeilederBegrunnelseDTO {
    public String veilederId;
    public String begrunnelse;
}
