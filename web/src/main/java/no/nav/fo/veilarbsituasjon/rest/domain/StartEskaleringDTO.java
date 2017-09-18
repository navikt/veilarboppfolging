package no.nav.fo.veilarbsituasjon.rest.domain;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class StartEskaleringDTO {
    public long dialogId;
    public String begrunnelse;
}
