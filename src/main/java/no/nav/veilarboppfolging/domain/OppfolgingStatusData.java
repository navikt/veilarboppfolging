package no.nav.veilarboppfolging.domain;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.*;

import static java.util.Comparator.naturalOrder;

@Data
@Accessors(chain = true)
public class OppfolgingStatusData {
    public String fnr;
    public String aktorId;
    public String veilederId;
    public boolean reservasjonKRR;
    public boolean manuell;
    public boolean underOppfolging;
    public boolean underKvp;
    public boolean kanStarteOppfolging;
    public boolean kanVarsles;
    public AvslutningStatusData avslutningStatusData;
    private EskaleringsvarselData gjeldendeEskaleringsvarsel;
    public List<Oppfolgingsperiode> oppfolgingsperioder = Collections.emptyList();
    public List<Kvp> kvpPerioder;
    public boolean harSkriveTilgang;
    public Boolean inaktivIArena;
    public Boolean kanReaktiveres;
    public Date inaktiveringsdato;
    public Boolean erSykmeldtMedArbeidsgiver;
    public String servicegruppe;
    public String formidlingsgruppe;
    public String rettighetsgruppe;

    @Deprecated
    public Boolean erIkkeArbeidssokerUtenOppfolging;

    public Date getOppfolgingUtgang() {
        return oppfolgingsperioder.stream().map(Oppfolgingsperiode::getSluttDato).filter(Objects::nonNull).max(naturalOrder()).orElse(null);
    }

}