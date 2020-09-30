package no.nav.veilarboppfolging.controller.domain;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;

@Data
@Accessors(chain = true)
public class OppfolgingStatus {
    public String fnr;
    public String aktorId;
    public String veilederId;
    public boolean reservasjonKRR;
    public boolean kanVarsles;
    public boolean manuell;
    public boolean underOppfolging;
    public boolean underKvp;

    public ZonedDateTime oppfolgingUtgang;
    public Eskaleringsvarsel gjeldendeEskaleringsvarsel;
    private boolean kanStarteOppfolging;
    private AvslutningStatus avslutningStatus;
    private List<OppfolgingPeriodeDTO> oppfolgingsPerioder;
    private boolean harSkriveTilgang;
    private Boolean inaktivIArena;
    private Boolean kanReaktiveres;
    public LocalDate inaktiveringsdato;
    private Boolean erSykmeldtMedArbeidsgiver;
    private String servicegruppe;
    private String formidlingsgruppe;
    private String rettighetsgruppe;

    @Deprecated
    private Boolean erIkkeArbeidssokerUtenOppfolging;
}