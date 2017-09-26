package no.nav.fo.veilarboppfolging.rest.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.Accessors;

import java.util.Date;

@Value
@Builder(toBuilder = true)
@AllArgsConstructor
@Accessors(chain = true)
public class AvslutningStatus {

    public boolean kanAvslutte;
    public boolean underOppfolging;
    public boolean harYtelser;
    public boolean harTiltak;
    public Date inaktiveringsDato;

}
