package no.nav.fo.veilarboppfolging.domain;

import lombok.AllArgsConstructor;
import lombok.Value;

import java.util.Date;

@Value
@AllArgsConstructor
public class VeilederTilordningerData {
    private String veileder;
    private String lagtInnAvVeilder;
    private Date sistTilordnet;
}
