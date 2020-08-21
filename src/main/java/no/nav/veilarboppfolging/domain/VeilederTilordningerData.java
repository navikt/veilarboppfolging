package no.nav.veilarboppfolging.domain;

import lombok.Builder;
import lombok.Value;

import java.util.Date;

@Value
@Builder
public class VeilederTilordningerData {
    private String veileder;
    private Date sistTilordnet;
}
