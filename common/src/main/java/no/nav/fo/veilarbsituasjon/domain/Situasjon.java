package no.nav.fo.veilarbsituasjon.domain;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

import static java.util.Collections.emptyList;

@Data
@Accessors(chain = true)
public class Situasjon {
    private String aktorId;
    private String veilederId;
    private boolean oppfolging;
    private Status gjeldendeStatus;
    private Brukervilkar gjeldendeBrukervilkar;
    private MalData gjeldendeMal;
    private List<Oppfolgingsperiode> oppfolgingsperioder = emptyList();
}
