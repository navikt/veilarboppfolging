package no.nav.fo.veilarboppfolging.domain;

import lombok.Builder;
import lombok.Data;

import java.util.Date;

@Data
@Builder
public class AvslutningStatusData {

    public final boolean kanAvslutte;
    public final boolean underOppfolging;
    public final boolean harYtelser;
    public final boolean harTiltak;
    public final boolean underKvp;
    public final Date inaktiveringsDato;

}