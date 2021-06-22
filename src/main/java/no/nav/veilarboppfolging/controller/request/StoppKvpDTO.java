package no.nav.veilarboppfolging.controller.request;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class StoppKvpDTO {

    private String begrunnelse;

}
