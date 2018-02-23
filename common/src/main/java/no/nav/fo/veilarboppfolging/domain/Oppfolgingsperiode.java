package no.nav.fo.veilarboppfolging.domain;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;
import java.util.List;

@Data
@Accessors(chain = true)
@Builder
public class Oppfolgingsperiode {
    String aktorId;
    String veileder;
    Date startDato;
    Date sluttDato;
    String begrunnelse;
    List<Kvp> kvpPerioder;
}
