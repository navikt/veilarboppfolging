package no.nav.fo.veilarboppfolging.rest.domain;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.Wither;

import java.time.LocalDate;

@Builder
@Wither
@Data
public class AktivStatus {
    private boolean aktiv;
    private LocalDate inaktiveringDato;
    private boolean underOppfolging;
}
