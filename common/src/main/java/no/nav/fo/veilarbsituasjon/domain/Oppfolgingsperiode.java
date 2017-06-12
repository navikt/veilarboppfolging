package no.nav.fo.veilarbsituasjon.domain;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.Accessors;

import java.util.Date;

@Value
@Accessors(chain = true)
@Builder
public class Oppfolgingsperiode {
    String aktorId;
    Date sluttDato;
    String begrunnelse;
}
