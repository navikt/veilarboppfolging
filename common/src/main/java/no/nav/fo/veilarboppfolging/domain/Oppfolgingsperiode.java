package no.nav.fo.veilarboppfolging.domain;

import lombok.Builder;
import lombok.Value;

import java.util.Date;

@Value
@Builder
public class Oppfolgingsperiode {
    String aktorId;
    String veileder;
    Date startDato;
    Date sluttDato;
    String begrunnelse;
}
