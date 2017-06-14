package no.nav.fo.veilarbsituasjon.domain;

import lombok.Builder;
import lombok.Value;

import java.util.Date;

@Value
@Builder
public class Oppfolgingsperiode {
    String aktorId;
    String veileder;
    Date sluttDato;
    String begrunnelse;
}
