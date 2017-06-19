package no.nav.fo.veilarbsituasjon.domain;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;
import java.util.List;

import static java.util.Collections.emptyList;

@Data
@Accessors(chain = true)
public class Situasjon {
    private String aktorId;
    private boolean oppfolging;
    private Status gjeldendeStatus;
    private Brukervilkar gjeldendeBrukervilkar;
    private Date oppfolgingUtgang;
    private MalData gjeldendeMal;
    private List<Oppfolgingsperiode> oppfolgingsperioder = emptyList();
}
