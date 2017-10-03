package no.nav.fo.veilarboppfolging.domain;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

import static java.util.Collections.emptyList;

@Data
@Accessors(chain = true)
public class Oppfolging {
    private String aktorId;
    private String veilederId;
    private boolean underOppfolging;
    private Status gjeldendeStatus;
    private Brukervilkar gjeldendeBrukervilkar;
    private EskaleringsvarselData gjeldendeEskaleringsvarsel;
    private MalData gjeldendeMal;
    private List<Oppfolgingsperiode> oppfolgingsperioder = emptyList();
}
