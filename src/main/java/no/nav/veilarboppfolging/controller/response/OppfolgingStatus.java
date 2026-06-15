package no.nav.veilarboppfolging.controller.response;




import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;


public class OppfolgingStatus {
    public String fnr;
    public String aktorId;
    public String veilederId;
    public boolean reservasjonKRR;
    public boolean registrertKRR;
    public boolean kanVarsles;
    public boolean manuell;
    public boolean underOppfolging;
    public boolean underKvp;

    public ZonedDateTime oppfolgingUtgang;
    private boolean kanStarteOppfolging;
    @Deprecated
    private AvslutningsStatusDto avslutningStatus;
    private List<OppfolgingPeriodeDTO> oppfolgingsPerioder;
    private boolean harSkriveTilgang;
    private Boolean inaktivIArena;
    private Boolean kanReaktiveres;
    public LocalDate inaktiveringsdato;
    /*
    * Får treff i "mulighetsrommet", "arbopp", "arbopp-new"
    * */
    private Boolean erSykmeldtMedArbeidsgiver;
    private String servicegruppe;
    private String formidlingsgruppe;
    private String rettighetsgruppe;
}