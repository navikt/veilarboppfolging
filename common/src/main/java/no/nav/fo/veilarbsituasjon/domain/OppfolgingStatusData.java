package no.nav.fo.veilarbsituasjon.domain;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Collections;
import java.util.Date;
import java.util.List;

@Data
@Accessors(chain = true)
public class OppfolgingStatusData {
    public String fnr;
    public String veilederId;
    public boolean reservasjonKRR;
    public boolean manuell;
    public boolean underOppfolging;
    public boolean vilkarMaBesvares;
    public Date oppfolgingUtgang;
    public boolean kanStarteOppfolging;
    public AvslutningStatusData avslutningStatusData;
    private EskaleringsvarselData gjeldendeEskaleringsvarsel;
    public List<Oppfolgingsperiode> oppfolgingsperioder = Collections.emptyList();
}