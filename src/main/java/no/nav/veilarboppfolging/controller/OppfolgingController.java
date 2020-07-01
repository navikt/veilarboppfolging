package no.nav.veilarboppfolging.controller;

import no.nav.veilarboppfolging.domain.*;
import no.nav.veilarboppfolging.controller.api.VeilederOppfolgingController;
import no.nav.veilarboppfolging.controller.domain.AvslutningStatus;
import no.nav.veilarboppfolging.controller.domain.Bruker;
import no.nav.veilarboppfolging.controller.domain.Eskaleringsvarsel;
import no.nav.veilarboppfolging.controller.domain.KvpPeriodeDTO;
import no.nav.veilarboppfolging.controller.domain.Mal;
import no.nav.veilarboppfolging.controller.domain.OppfolgingPeriodeDTO;
import no.nav.veilarboppfolging.controller.domain.OppfolgingStatus;
import no.nav.veilarboppfolging.controller.domain.StartEskaleringDTO;
import no.nav.veilarboppfolging.controller.domain.StartKvpDTO;
import no.nav.veilarboppfolging.controller.domain.StoppEskaleringDTO;
import no.nav.veilarboppfolging.controller.domain.StoppKvpDTO;
import no.nav.veilarboppfolging.controller.domain.VeilederBegrunnelseDTO;
import no.nav.veilarboppfolging.services.*;
import no.nav.veilarboppfolging.utils.FnrParameterUtil;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;

@RequestMapping("/oppfolging")
@RestController
public class OppfolgingController {

    @Inject
    private OppfolgingService oppfolgingService;

    @Inject
    private KvpService kvpService;

    @Inject
    private HistorikkService historikkService;

    @Inject
    private MalService malService;

    @Inject
    private FnrParameterUtil fnrParameterUtil;

    @Inject
    private AktiverBrukerService aktiverBrukerService;

    @Inject
    private AuthService authService;

    @Inject
    private PepClient pepClient;

    @Inject
    private AktorService aktorService;

    @GetMapping("/me")
    public Bruker hentBrukerInfo() {
        return new Bruker()
                .setId(getUid())
                .setErVeileder(AuthService.erInternBruker())
                .setErBruker(AuthService.erEksternBruker());
    }

    @GetMapping
    public OppfolgingStatus hentOppfolgingsStatus() {
        return tilDto(oppfolgingService.hentOppfolgingsStatus(getFnr()));
    }

    @PostMapping("/startOppfolging")
    public OppfolgingStatus startOppfolging() {
        authService.skalVereInternBruker();
        return tilDto(oppfolgingService.startOppfolging(getFnr()));
    }

    @GetMapping("/avslutningStatus")
    public OppfolgingStatus hentAvslutningStatus() {
        authService.skalVereInternBruker();
        return tilDto(oppfolgingService.hentAvslutningStatus(getFnr()));
    }

    @PostMapping("/avsluttOppfolging")
    public OppfolgingStatus avsluttOppfolging(VeilederBegrunnelseDTO dto) {
        authService.skalVereInternBruker();
        return tilDto(oppfolgingService.avsluttOppfolging(
                getFnr(),
                dto.veilederId,
                dto.begrunnelse
        ));
    }

    @PostMapping("/settManuell")
    public OppfolgingStatus settTilManuell(VeilederBegrunnelseDTO dto) {
        authService.skalVereInternBruker();
        return tilDto(oppfolgingService.oppdaterManuellStatus(getFnr(),
                true,
                dto.begrunnelse,
                KodeverkBruker.NAV,
                hentBrukerInfo().getId())
        );
    }

    @PostMapping("/settDigital")
    public OppfolgingStatus settTilDigital(VeilederBegrunnelseDTO dto) {

        if (AuthService.erEksternBruker()) {
            return tilDto(oppfolgingService.settDigitalBruker(getFnr()));
        }

        return tilDto(oppfolgingService.oppdaterManuellStatus(getFnr(),
                false,
                dto.begrunnelse,
                KodeverkBruker.NAV,
                hentBrukerInfo().getId())
        );
    }

    @GetMapping("/innstillingsHistorikk")
    public List<InnstillingsHistorikk> hentInnstillingsHistorikk() {
        authService.skalVereInternBruker();
        return historikkService.hentInstillingsHistorikk(getFnr());
    }

    @PostMapping("/startEskalering")
    public void startEskalering(StartEskaleringDTO startEskalering) {
        authService.skalVereInternBruker();
        oppfolgingService.startEskalering(
                getFnr(),
                startEskalering.getBegrunnelse(),
                startEskalering.getDialogId()
        );
    }

    @PostMapping("/stoppEskalering")
    public void stoppEskalering(StoppEskaleringDTO stoppEskalering) {
        authService.skalVereInternBruker();
        oppfolgingService.stoppEskalering(getFnr(), stoppEskalering.getBegrunnelse());
    }

    @PostMapping("/startKvp")
    public void startKvp(StartKvpDTO startKvp) {
        authService.skalVereInternBruker();
        kvpService.startKvp(getFnr(), startKvp.getBegrunnelse());
    }

    @PostMapping("/stoppKvp")
    public void stoppKvp(StoppKvpDTO stoppKvp) {
        authService.skalVereInternBruker();
        kvpService.stopKvp(getFnr(), stoppKvp.getBegrunnelse());
    }

    @GetMapping("/veilederTilgang")
    public VeilederTilgang hentVeilederTilgang() {
        authService.skalVereInternBruker();
        return oppfolgingService.hentVeilederTilgang(getFnr());
    }

    private Eskaleringsvarsel tilDto(EskaleringsvarselData eskaleringsvarselData) {
        return Optional.ofNullable(eskaleringsvarselData)
                .map(eskalering -> Eskaleringsvarsel.builder()
                        .varselId(eskalering.getVarselId())
                        .aktorId(eskalering.getAktorId())
                        .opprettetAv(AuthService.erInternBruker() ? eskalering.getOpprettetAv() : null)
                        .opprettetDato(eskalering.getOpprettetDato())
                        .avsluttetDato(eskalering.getAvsluttetDato())
                        .tilhorendeDialogId(eskalering.getTilhorendeDialogId())
                        .build()
                ).orElse(null);
    }

    private String getUid() {
        return SubjectHandler.getIdent().orElseThrow(RuntimeException::new);
    }

    private String getFnr() {
        return fnrParameterUtil.getFnr();
    }

    private AvslutningStatus tilDto(AvslutningStatusData avslutningStatusData) {
        return new AvslutningStatus(
                avslutningStatusData.kanAvslutte,
                avslutningStatusData.underOppfolging,
                avslutningStatusData.harYtelser,
                avslutningStatusData.harTiltak,
                avslutningStatusData.underKvp,
                avslutningStatusData.inaktiveringsDato
        );
    }

    private OppfolgingStatus tilDto(OppfolgingStatusData oppfolgingStatusData) {
        OppfolgingStatus status = new OppfolgingStatus()
                .setFnr(oppfolgingStatusData.fnr)
                .setAktorId(oppfolgingStatusData.aktorId)
                .setUnderOppfolging(oppfolgingStatusData.underOppfolging)
                .setManuell(oppfolgingStatusData.manuell)
                .setReservasjonKRR(oppfolgingStatusData.reservasjonKRR)
                .setOppfolgingUtgang(oppfolgingStatusData.getOppfolgingUtgang())
                .setKanReaktiveres(oppfolgingStatusData.kanReaktiveres)
                .setOppfolgingsPerioder(oppfolgingStatusData.oppfolgingsperioder.stream().map(this::tilDTO).collect(toList()))
                .setInaktiveringsdato(oppfolgingStatusData.inaktiveringsdato)
                .setGjeldendeEskaleringsvarsel(tilDto(oppfolgingStatusData.getGjeldendeEskaleringsvarsel()))
                .setErIkkeArbeidssokerUtenOppfolging(oppfolgingStatusData.getErSykmeldtMedArbeidsgiver())
                .setErSykmeldtMedArbeidsgiver(oppfolgingStatusData.getErSykmeldtMedArbeidsgiver())
                .setHarSkriveTilgang(true)
                .setServicegruppe(oppfolgingStatusData.getServicegruppe())
                .setFormidlingsgruppe(oppfolgingStatusData.getFormidlingsgruppe())
                .setRettighetsgruppe(oppfolgingStatusData.getRettighetsgruppe())
                .setKanVarsles(oppfolgingStatusData.kanVarsles)
                .setUnderKvp(oppfolgingStatusData.underKvp);

        if (AuthService.erInternBruker()) {
            status
                    .setVeilederId(oppfolgingStatusData.veilederId)
                    .setKanStarteOppfolging(oppfolgingStatusData.isKanStarteOppfolging())
                    .setAvslutningStatus(
                            ofNullable(oppfolgingStatusData.getAvslutningStatusData())
                                    .map(this::tilDto)
                                    .orElse(null)
                    )
                    .setOppfolgingUtgang(oppfolgingStatusData.getOppfolgingUtgang())
                    .setHarSkriveTilgang(oppfolgingStatusData.harSkriveTilgang)
                    .setInaktivIArena(oppfolgingStatusData.inaktivIArena);
        }

        return status;
    }

    private OppfolgingPeriodeDTO tilDTO(Oppfolgingsperiode oppfolgingsperiode) {
        OppfolgingPeriodeDTO periode = new OppfolgingPeriodeDTO()
                .setSluttDato(oppfolgingsperiode.getSluttDato())
                .setStartDato(oppfolgingsperiode.getStartDato())
                .setKvpPerioder(oppfolgingsperiode.getKvpPerioder().stream().map(this::tilDTO).collect(toList()));

        if (AuthService.erInternBruker()) {
            periode.setVeileder(oppfolgingsperiode.getVeileder())
                    .setAktorId(oppfolgingsperiode.getAktorId())
                    .setBegrunnelse(oppfolgingsperiode.getBegrunnelse())
            ;
        }

        return periode;
    }

    private KvpPeriodeDTO tilDTO(Kvp kvp) {
        return new KvpPeriodeDTO(kvp.getOpprettetDato(), kvp.getAvsluttetDato());
    }

    private Mal tilDto(MalData malData) {
        return new Mal()
                .setMal(malData.getMal())
                .setEndretAv(malData.getEndretAvFormattert())
                .setDato(malData.getDato());
    }
}
