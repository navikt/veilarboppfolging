package no.nav.veilarboppfolging.controller.v2.request;

import lombok.Data;
import lombok.experimental.Accessors;
import no.nav.common.types.identer.Fnr;

@Data
@Accessors(chain = true)
public class StartEskaleringV2Request {
    long dialogId;
    String begrunnelse;
    Fnr fnr;
}
