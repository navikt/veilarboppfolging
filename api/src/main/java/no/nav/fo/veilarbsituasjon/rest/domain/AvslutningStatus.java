package no.nav.fo.veilarbsituasjon.rest.domain;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;

@Data
@Accessors(chain = true)
public class AvslutningStatus {

    public boolean kanAvslutte;
    public boolean underOppfolging;
    public boolean harYtelser;
    public boolean harTiltak;
    public Date inaktiveringsDato;

}
