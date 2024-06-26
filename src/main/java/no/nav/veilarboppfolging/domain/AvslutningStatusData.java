package no.nav.veilarboppfolging.domain;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class AvslutningStatusData {
    public final boolean kanAvslutte;
    public final boolean underOppfolging;
    public final boolean harYtelser;
    public final boolean underKvp;
    public final LocalDate inaktiveringsDato;
    public final boolean erIserv;
    public final boolean harAktiveTiltaksdeltakelser;
}