package no.nav.fo.veilarboppfolging.rest.domain;

import lombok.Builder;
import lombok.Data;
import lombok.ToString;
import lombok.experimental.Wither;

import java.time.LocalDate;

@Builder
@Wither
@ToString
@Data
public class AktivStatus {
    private boolean aktiv;
    private LocalDate inaktiveringDato;
    private boolean underOppfolging;
}
