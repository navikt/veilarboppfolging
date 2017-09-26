package no.nav.fo.veilarboppfolging.rest.domain;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class StartEskaleringDTO {
    public long dialogId;
    public String begrunnelse;
}
