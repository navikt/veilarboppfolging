package no.nav.fo.veilarbsituasjon.rest.domain;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;

@Data
@Accessors(chain = true)
public class OppfolgingPeriodeDTO {

    public Date sluttDato;

}
