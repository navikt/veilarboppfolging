package no.nav.veilarboppfolging.controller;

import no.nav.common.auth.context.AuthContextHolder;
import no.nav.common.job.JobRunner;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.NavIdent;
import no.nav.veilarboppfolging.ForbiddenException;
import no.nav.veilarboppfolging.controller.response.Veilarbportefoljeinfo;
import no.nav.veilarboppfolging.controller.v2.request.RepubliserVeilederRequest;
import no.nav.veilarboppfolging.domain.AvsluttOppfolgingsperiodePayload;
import no.nav.veilarboppfolging.domain.AvsluttResultat;
import no.nav.veilarboppfolging.domain.RepubliserOppfolgingsperioderRequest;
import no.nav.veilarboppfolging.domain.AvsluttPayload;
import no.nav.veilarboppfolging.kandidatForUtmelding.KandidatForUtmeldingService;
import no.nav.veilarboppfolging.oppfolgingsbruker.VeilederRegistrant;
import no.nav.veilarboppfolging.oppfolgingsbruker.utgang.AdminAvregistrering;
import no.nav.veilarboppfolging.repository.OppfolgingsPeriodeRepository;
import no.nav.veilarboppfolging.repository.VeilederTilordningerRepository;
import no.nav.veilarboppfolging.repository.entity.ManuellStatusEntity;
import no.nav.veilarboppfolging.repository.entity.OppfolgingsperiodeEntity;
import no.nav.veilarboppfolging.repository.entity.VeilederTilordningEntity;
import no.nav.veilarboppfolging.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    public static final String POAO_ADMIN = "poao-admin";
    private final AuthService authService;
    private final AuthContextHolder authContextHolder;
    private final KafkaRepubliseringService kafkaRepubliseringService;
    private final VeilederTilordningerRepository veilederTilordningerRepository;
    private final ManuellStatusService manuellStatusService;
    private final OppfolgingsPeriodeRepository oppfolgingsPeriodeRepository;
    private final OppfolgingService oppfolgingService;
    private final AvsluttOppfolgingService avsluttOppfolgingService;
    private final KandidatForUtmeldingService kandidatForUtmeldingService;

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    public AdminController(
            AuthService authService,
            AuthContextHolder authContextHolder,
            KafkaRepubliseringService kafkaRepubliseringService,
            VeilederTilordningerRepository veilederTilordningerRepository,
            ManuellStatusService manuellStatusService,
            OppfolgingsPeriodeRepository oppfolgingsPeriodeRepository,
            OppfolgingService oppfolgingService,
            AvsluttOppfolgingService avsluttOppfolgingService,
            KandidatForUtmeldingService kandidatForUtmeldingService
    ) {
        this.authService = authService;
        this.authContextHolder = authContextHolder;
        this.kafkaRepubliseringService = kafkaRepubliseringService;
        this.veilederTilordningerRepository = veilederTilordningerRepository;
        this.manuellStatusService = manuellStatusService;
        this.oppfolgingsPeriodeRepository = oppfolgingsPeriodeRepository;
        this.oppfolgingService = oppfolgingService;
        this.avsluttOppfolgingService = avsluttOppfolgingService;
        this.kandidatForUtmeldingService = kandidatForUtmeldingService;
    }

    @PostMapping("/republiser/oppfolgingsperioder")
    public String republiserOppfolgingsperioder(@RequestBody(required = false) RepubliserOppfolgingsperioderRequest request) {
        sjekkTilgangTilAdmin();

        if (request != null && request.aktorId != null) {
            return JobRunner.runAsync("republiser-oppfolgingsperioder-for-bruker", () -> kafkaRepubliseringService.republiserOppfolgingsperiodeForBruker(request.aktorId));
        }

        return JobRunner.runAsync("republiser-oppfolgingsperioder", kafkaRepubliseringService::republiserOppfolgingsperioder);
    }

    @PostMapping("/republiser/tilordnet-veileder")
    public String republiserTilordnetVeileder() {
        sjekkTilgangTilAdmin();
        return JobRunner.runAsync("republiser-tilordnet-veileder", kafkaRepubliseringService::republiserTilordnetVeileder);
    }

    @PostMapping("/republiser/tilordnet-veileder/utvalg")
    public String republiserTilordnetVeileder(@RequestBody RepubliserVeilederRequest republiserVeilederRequest) {
        sjekkTilgangTilAdmin();
        return JobRunner.runAsync(
                "republiser-tilordnet-veileder-gitte-aktorider",
                () -> kafkaRepubliseringService.republiserTilordnetVeileder(republiserVeilederRequest.aktorIder())
        );
    }

    @PostMapping("/republiser/kvp-perioder")
    public String republiserKvpPerioder() {
        sjekkTilgangTilAdmin();
        return JobRunner.runAsync("republiser-kvp-perioder", kafkaRepubliseringService::republiserKvpPerioder);
    }

    @GetMapping("/hentVeilarbinfo/bruker")
    public Veilarbportefoljeinfo hentVeilarbportefoljeinfo(@RequestParam AktorId aktorId) {
        authService.skalVereSystemBruker();
        Optional<VeilederTilordningEntity> tilordningEntity = veilederTilordningerRepository.hentTilordnetVeileder(aktorId);
        boolean erManuell = manuellStatusService.hentManuellStatus(aktorId).map(ManuellStatusEntity::getManuell).orElse(false);
        ZonedDateTime startDato = oppfolgingsPeriodeRepository.hentGjeldendeOppfolgingsperiode(aktorId).map(OppfolgingsperiodeEntity::getStartDato).orElse(null);

        return new Veilarbportefoljeinfo(
                aktorId,
                tilordningEntity.map(VeilederTilordningEntity::getVeilederId).map(NavIdent::of).orElse(null),
                tilordningEntity.map(VeilederTilordningEntity::getOppfolging).orElse(false),
                tilordningEntity.map(VeilederTilordningEntity::getNyForVeileder).orElse(false),
                erManuell,
                startDato,
                tilordningEntity.map(VeilederTilordningEntity::getSistTilordnet).orElse(null)
        );
    }

    @PostMapping("/avsluttBrukere")
    public AvsluttResultat batchAvsluttBrukere(@RequestBody AvsluttPayload brukereSomSkalAvsluttes) {
        sjekkTilgangTilAdmin();
        var innloggetBruker = authService.getInnloggetVeilederIdent();
        log.info("Skal avslutte oppfølging for {} brukere", brukereSomSkalAvsluttes.aktorIds.size());

        var resultat = brukereSomSkalAvsluttes.aktorIds
                .stream()
                .map(aktorId -> {
                    try {
                        avsluttOppfolgingService.adminAvsluttOppfolgingForBruker(
                                new AdminAvregistrering(
                                        AktorId.of(aktorId),
                                        new VeilederRegistrant(new NavIdent(innloggetBruker)),
                                        brukereSomSkalAvsluttes.begrunnelse,
                                        null
                                )
                        );
                        return true;
                    } catch (Exception e) {
                        log.warn("Kunne ikke avslutte oppfølging: {}", e.getMessage());
                        return false;
                    }
                }).toList();

        var avsluttedeBrukere = resultat.stream().filter(it -> it).toList().size();
        var ikkeAvsluttedeBrukere = resultat.stream().filter(it -> !it).toList().size();

        log.info("Avsluttet oppfølging for {} brukere", avsluttedeBrukere);
        log.info("Kunne ikke avslutte oppfølging for {} brukere", ikkeAvsluttedeBrukere);

        return new AvsluttResultat(avsluttedeBrukere, ikkeAvsluttedeBrukere);
    }

    @PostMapping("/avsluttOppfolgingsperiode")
    public boolean avsluttOppfolgingsperiode(@RequestBody AvsluttOppfolgingsperiodePayload oppfolgingsperiodeSomSkalAvsluttes) {
        sjekkTilgangTilAdmin();
        var innloggetBruker = authService.getInnloggetVeilederIdent();

        try {
            avsluttOppfolgingService.adminAvsluttSpesifikkOppfolgingsperiode(
                    AktorId.of(oppfolgingsperiodeSomSkalAvsluttes.getAktorId()),
                    innloggetBruker,
                    oppfolgingsperiodeSomSkalAvsluttes.getBegrunnelse(),
                    oppfolgingsperiodeSomSkalAvsluttes.getOppfolgingsperiodeUuid());
            return true;
        } catch (Exception e) {
            log.warn("Kunne ikke avslutte oppfølgingsperiode: {}", e.getMessage());
            return false;
        }
    }

    private void sjekkTilgangTilAdmin() {
        if (!authService.erInternBruker()) throw new ForbiddenException("Må være internbruker");
        authService.sjekkAtApplikasjonErIAllowList(List.of(POAO_ADMIN));
    }

}
