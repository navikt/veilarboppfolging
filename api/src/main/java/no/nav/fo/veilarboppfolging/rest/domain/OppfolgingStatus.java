package no.nav.fo.veilarboppfolging.rest.domain;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;
import java.util.List;

@Data
@Accessors(chain = true)
public class OppfolgingStatus {
    public String fnr;
    public String veilederId;
    public boolean reservasjonKRR;
    public boolean manuell;
    public boolean underOppfolging;
    public boolean underKvp;
    public boolean vilkarMaBesvares;
    public Date oppfolgingUtgang;
    public Eskaleringsvarsel gjeldendeEskaleringsvarsel;
    private boolean kanStarteOppfolging;
    private AvslutningStatus avslutningStatus;
    private List<OppfolgingPeriodeDTO> oppfolgingsPerioder;
    private boolean harSkriveTilgang;
    private Boolean inaktivIArena;
    private Boolean kanReaktiveres;
}