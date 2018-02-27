package no.nav.fo.veilarboppfolging.domain;

import lombok.Builder;
import lombok.Value;

import java.util.Date;
import java.util.List;

@Value
@Builder(toBuilder = true)
public class Oppfolgingsperiode {
    String aktorId;
    String veileder;
    Date startDato;
    Date sluttDato;
    String begrunnelse;
    List<Kvp> kvpPerioder;
}
