package no.nav.fo.veilarbsituasjon.rest.domain;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.Accessors;

@Value
@Accessors(chain = true)
@Builder
public class OppfolgingsperiodeDTO {
    String veilederId;
    String begrunnelse;
}
