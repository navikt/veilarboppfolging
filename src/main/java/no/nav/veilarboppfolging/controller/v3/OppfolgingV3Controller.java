package no.nav.veilarboppfolging.controller.v3;

import lombok.RequiredArgsConstructor;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.veilarboppfolging.BadRequestException;
import no.nav.veilarboppfolging.controller.response.*;
import no.nav.veilarboppfolging.controller.v2.response.UnderOppfolgingV2Response;
import no.nav.veilarboppfolging.controller.v3.request.*;
import no.nav.veilarboppfolging.repository.entity.KvpPeriodeEntity;
import no.nav.veilarboppfolging.repository.entity.OppfolgingsperiodeEntity;
import no.nav.veilarboppfolging.repository.enums.KodeverkBruker;
import no.nav.veilarboppfolging.service.*;
import no.nav.veilarboppfolging.utils.DtoMappers;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

import static no.nav.veilarboppfolging.utils.DtoMappers.tilDto;
import static no.nav.veilarboppfolging.utils.DtoMappers.tilOppfolgingPeriodeDTO;
import static no.nav.veilarboppfolging.utils.auth.AllowListApplicationName.*;

@RestController
@RequestMapping("/api/v3")
@RequiredArgsConstructor
public class OppfolgingV3Controller {
    private final OppfolgingService oppfolgingService;

    private final AuthService authService;
    private final ManuellStatusService manuellStatusService;
    private final KvpService kvpService;
    private final static List<String> ALLOWLIST = List.of(VEILARBREGISTRERING);
    private final AktiverBrukerService aktiverBrukerService;

    @PostMapping("/hent-oppfolging")
    public UnderOppfolgingV2Response underOppfolging(@RequestBody OppfolgingRequest oppfolgingRequest) {
        List<String> allowlist = List.of(VEILARBVEDTAKSSTOTTE, VEILARBDIALOG, VEILARBAKTIVITET, VEILARBREGISTRERING, VEILARBPORTEFOLJE);
        authService.authorizeRequest(oppfolgingRequest.fnr(), allowlist);
        return new UnderOppfolgingV2Response(oppfolgingService.erUnderOppfolging(oppfolgingRequest.fnr()));
    }

    @GetMapping("/oppfolging/me")
    public Bruker hentBrukerInfo() {
        return new Bruker()
                .setId(authService.getInnloggetBrukerIdent())
                .setErVeileder(authService.erInternBruker())
                .setErBruker(authService.erEksternBruker());
    }

    @PostMapping("/oppfolging/hent-status")
    public OppfolgingStatus hentOppfolgingsStatus(@RequestBody(required = false) OppfolgingRequest oppfolgingRequest) {
        Fnr maybeFodselsnummer = oppfolgingRequest == null ? null : oppfolgingRequest.fnr();
        Fnr fodselsnummer = authService.hentIdentForEksternEllerIntern(maybeFodselsnummer);
        return tilDto(oppfolgingService.hentOppfolgingsStatus(fodselsnummer), authService.erInternBruker());
    }

    @PostMapping("/oppfolging/hent-avslutning-status")
    public AvslutningStatus hentAvslutningStatus(@RequestBody OppfolgingRequest oppfolgingRequest) {
        authService.skalVereInternBruker();
        return tilDto(oppfolgingService.hentAvslutningStatus(oppfolgingRequest.fnr()));
    }

    @PostMapping("/oppfolging/hent-gjeldende-periode")
    public ResponseEntity<OppfolgingPeriodeMinimalDTO> hentGjeldendePeriode(@RequestBody OppfolgingRequest oppfolgingRequest) {
        List<String> allowlist = List.of(VEILARBVEDTAKSSTOTTE, VEILARBDIALOG, VEILARBAKTIVITET, MULIGHETSROMMET);
        authService.authorizeRequest(oppfolgingRequest.fnr(), allowlist);
        return oppfolgingService.hentGjeldendeOppfolgingsperiode(oppfolgingRequest.fnr())
                .map(DtoMappers::tilOppfolgingPeriodeMinimalDTO)
                .map(op -> new ResponseEntity<>(op, HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NO_CONTENT));
    }

    @PostMapping(value = "/oppfolging/hent-perioder")
    public List<OppfolgingPeriodeDTO> hentOppfolgingsperioder(@RequestBody OppfolgingRequest oppfolgingRequest) {
        List<String> allowlist = List.of(VEILARBVEDTAKSSTOTTE, AMT_PERSON_SERVICE);
        authService.authorizeRequest(oppfolgingRequest.fnr(), allowlist);
        AktorId aktorId = authService.getAktorIdOrThrow(oppfolgingRequest.fnr());
        return hentOppfolgingsperioder(aktorId);
    }

    @PostMapping("/oppfolging/settManuell")
    public ResponseEntity settTilManuell(@RequestBody VeilederBegrunnelseRequest veilederBegrunnelseRequest) {
        authService.skalVereInternBruker();

        manuellStatusService.oppdaterManuellStatus(
                veilederBegrunnelseRequest.fnr(), true, veilederBegrunnelseRequest.begrunnelse(),
                KodeverkBruker.NAV, authService.getInnloggetVeilederIdent()
        );

        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @PostMapping("/oppfolging/settDigital")
    public ResponseEntity settTilDigital(@RequestBody(required = false) VeilederBegrunnelseRequest veilederBegrunnelseRequest) {
        Fnr maybeFodselsnummer = veilederBegrunnelseRequest == null ? null : veilederBegrunnelseRequest.fnr();
        Fnr fodselsnummer = authService.hentIdentForEksternEllerIntern(maybeFodselsnummer);

        if (authService.erEksternBruker()) {
            manuellStatusService.settDigitalBruker(fodselsnummer);
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        }

        // Påkrevd for intern bruker
        if (veilederBegrunnelseRequest == null) {
            throw new BadRequestException("veilederBegrunnelseRequest er påkrevd for interne brukere");
        }

        manuellStatusService.oppdaterManuellStatus(
                fodselsnummer, false, veilederBegrunnelseRequest.begrunnelse(),
                KodeverkBruker.NAV, hentBrukerInfo().getId()
        );

        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @PostMapping("/oppfolging/startKvp")
    public ResponseEntity startKvp(@RequestBody KvpRequest startKvp) {
        authService.skalVereInternBruker();
        kvpService.startKvp(startKvp.fnr(), startKvp.begrunnelse());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @PostMapping("/oppfolging/stoppKvp")
    public ResponseEntity stoppKvp(@RequestBody KvpRequest stoppKvp) {
        authService.skalVereInternBruker();
        kvpService.stopKvp(stoppKvp.fnr(), stoppKvp.begrunnelse());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @PostMapping("/oppfolging/hent-veilederTilgang")
    public VeilederTilgang hentVeilederTilgang(@RequestBody VeilederRequest veilederRequest) {
        authService.skalVereInternBruker();
        return oppfolgingService.hentVeilederTilgang(veilederRequest.fnr());
    }

    @PostMapping("/oppfolging/harFlereAktorIderMedOppfolging")
    public boolean harFlereAktorIderMedOppfolging(@RequestBody(required = false) OppfolgingRequest oppfolgingRequest) {
        Fnr maybeFodselsnummer = oppfolgingRequest == null ? null : oppfolgingRequest.fnr();
        Fnr fodselsnummer = authService.hentIdentForEksternEllerIntern(maybeFodselsnummer);
        return oppfolgingService.hentHarFlereAktorIderMedOppfolging(fodselsnummer);
    }

    @PostMapping("/oppfolging/aktiverSykmeldt")
    public ResponseEntity aktiverSykmeldt(@RequestBody SykmeldtBrukerRequest sykmeldtBrukerRequest) {
        authService.skalVereSystemBrukerFraAzureAd();
        authService.sjekkAtApplikasjonErIAllowList(ALLOWLIST);

        aktiverBrukerService.aktiverSykmeldt(sykmeldtBrukerRequest.fnr(), sykmeldtBrukerRequest.sykmeldtBrukerType());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    private List<OppfolgingPeriodeDTO> hentOppfolgingsperioder(AktorId aktorId) {
        return oppfolgingService.hentOppfolgingsperioder(aktorId)
                .stream()
                .map(this::filtrerKvpPerioder)
                .map(this::mapTilDto)
                .collect(Collectors.toList());
    }

    private OppfolgingPeriodeDTO mapTilDto(OppfolgingsperiodeEntity oppfolgingsperiode) {
        return tilOppfolgingPeriodeDTO(oppfolgingsperiode, !authService.erEksternBruker());
    }

    private OppfolgingsperiodeEntity filtrerKvpPerioder(OppfolgingsperiodeEntity periode) {
        if (!authService.erInternBruker() || periode.getKvpPerioder() == null || periode.getKvpPerioder().isEmpty()) {
            return periode;
        }

        List<KvpPeriodeEntity> kvpPeriodeEntities = periode
                .getKvpPerioder()
                .stream()
                .filter(it -> authService.harTilgangTilEnhet(it.getEnhet()))
                .collect(Collectors.toList());

        return periode.toBuilder().kvpPerioder(kvpPeriodeEntities).build();
    }


}
