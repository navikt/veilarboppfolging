package no.nav.fo.veilarboppfolging.domain;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import static java.util.Comparator.naturalOrder;

@Data
@Accessors(chain = true)
public class OppfolgingStatusData {
    public String fnr;
    public String veilederId;
    public boolean reservasjonKRR;
    public boolean manuell;
    public boolean underOppfolging;
    public boolean underKvp;
    public boolean vilkarMaBesvares;
    public boolean kanStarteOppfolging;
    public AvslutningStatusData avslutningStatusData;
    private EskaleringsvarselData gjeldendeEskaleringsvarsel;
    public List<Oppfolgingsperiode> oppfolgingsperioder = Collections.emptyList();
    public List<Kvp> kvpPerioder;
    public boolean harSkriveTilgang;
    public Boolean inaktivIArena;
    public Boolean kanReaktiveres;
    public Boolean erIkkeArbeidssokerUtenOppfolging;

    public Date getOppfolgingUtgang() {
        return oppfolgingsperioder.stream().map(Oppfolgingsperiode::getSluttDato).filter(Objects::nonNull).max(naturalOrder()).orElse(null);
    }

}