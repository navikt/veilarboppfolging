package no.nav.fo.veilarbsituasjon.domain;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class Situasjon {
    private String aktorId;
    private boolean oppfolging;
    private Status gjeldendeStatus;
    private Brukervilkar gjeldendeBrukervilkar;
}
