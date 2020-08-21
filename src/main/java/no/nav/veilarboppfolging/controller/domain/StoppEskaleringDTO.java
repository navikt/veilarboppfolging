package no.nav.veilarboppfolging.controller.domain;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class StoppEskaleringDTO {
    public String begrunnelse;
}
