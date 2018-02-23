package no.nav.fo.veilarboppfolging.rest.domain;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;

@Data
@Accessors(chain = true)
public class KvpPeriodeDTO {

    private Date opprettetDato;
    private Date avsluttetDato;

}
