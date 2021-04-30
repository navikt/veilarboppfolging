package no.nav.veilarboppfolging.domain;

import lombok.Builder;
import lombok.Value;

import java.time.ZonedDateTime;
import java.util.List;

@Value
@Builder(toBuilder = true)
public class Oppfolgingsperiode {
    long id;
    String aktorId;
    String veileder;
    ZonedDateTime startDato;
    ZonedDateTime sluttDato;
    String begrunnelse;
    List<Kvp> kvpPerioder;
}
