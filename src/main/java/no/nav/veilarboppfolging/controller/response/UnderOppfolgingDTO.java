package no.nav.veilarboppfolging.controller.response;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class UnderOppfolgingDTO {

    private boolean underOppfolging;

    private boolean erManuell;

}
