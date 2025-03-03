package no.nav.veilarboppfolging.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.auth.context.AuthContextHolder;
import no.nav.common.auth.context.UserRole;
import no.nav.common.client.aktoroppslag.AktorOppslagClient;
import no.nav.common.job.JobRunner;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.NavIdent;
import no.nav.veilarboppfolging.ForbiddenException;
import no.nav.veilarboppfolging.UnauthorizedException;
import no.nav.veilarboppfolging.controller.response.Veilarbportefoljeinfo;
import no.nav.veilarboppfolging.domain.AvsluttResultat;
import no.nav.veilarboppfolging.domain.RepubliserOppfolgingsperioderRequest;
import no.nav.veilarboppfolging.domain.AvsluttPayload;
import no.nav.veilarboppfolging.repository.OppfolgingsPeriodeRepository;
import no.nav.veilarboppfolging.repository.VeilederTilordningerRepository;
import no.nav.veilarboppfolging.repository.entity.ManuellStatusEntity;
import no.nav.veilarboppfolging.repository.entity.OppfolgingsperiodeEntity;
import no.nav.veilarboppfolging.repository.entity.VeilederTilordningEntity;
import no.nav.veilarboppfolging.service.AuthService;
import no.nav.veilarboppfolging.service.KafkaRepubliseringService;
import no.nav.veilarboppfolging.service.ManuellStatusService;
import no.nav.veilarboppfolging.service.OppfolgingService;
import org.springframework.web.bind.annotation.*;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    public final static String PTO_ADMIN = "pto-admin";
    private final AuthService authService;
    private final AuthContextHolder authContextHolder;
    private final KafkaRepubliseringService kafkaRepubliseringService;
    private final VeilederTilordningerRepository veilederTilordningerRepository;
    private final ManuellStatusService manuellStatusService;
    private final OppfolgingsPeriodeRepository oppfolgingsPeriodeRepository;
    private final OppfolgingService oppfolgingService;

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

    @PostMapping("/republiser/kvp-perioder")
    public String republiserKvpPerioder() {
        sjekkTilgangTilAdmin();
        return JobRunner.runAsync("republiser-kvp-perioder", kafkaRepubliseringService::republiserKvpPerioder);
    }

    @GetMapping("/hentVeilarbinfo/bruker")
    public Veilarbportefoljeinfo hentVeilarbportefoljeinfo(@RequestParam AktorId aktorId) {
        authService.skalVereSystemBruker();
        Optional<VeilederTilordningEntity> tilordningEntity = veilederTilordningerRepository.hentTilordnetVeileder(aktorId);
        boolean erManuell = manuellStatusService.hentManuellStatus(aktorId).map(ManuellStatusEntity::isManuell).orElse(false);
        ZonedDateTime startDato = oppfolgingsPeriodeRepository.hentGjeldendeOppfolgingsperiode(aktorId).map(OppfolgingsperiodeEntity::getStartDato).orElse(null);

        return new Veilarbportefoljeinfo().setVeilederId(tilordningEntity.map(VeilederTilordningEntity::getVeilederId).map(NavIdent::of).orElse(null))
                .setErUnderOppfolging(tilordningEntity.map(VeilederTilordningEntity::isOppfolging).orElse(false))
                .setNyForVeileder(tilordningEntity.map(VeilederTilordningEntity::isNyForVeileder).orElse(false))
                .setErManuell(erManuell)
                .setStartDato(startDato);
    }

    @PostMapping("/avsluttBrukere")
    public AvsluttResultat batchAvsluttBrukere(@RequestBody AvsluttPayload brukereSomSkalAvsluttes) {
        sjekkTilgangTilAdmin();
        var innloggetBruker = authService.hentInnloggetPersonIdent();
        log.info("Skal avslutte oppfølging for {} brukere", brukereSomSkalAvsluttes.aktorIds.size());

        var resultat = brukereSomSkalAvsluttes.getAktorIds()
                .stream()
                .map(aktorId -> {
                    try {
                        oppfolgingService.adminForceAvsluttOppfolgingForBruker(AktorId.of(aktorId), innloggetBruker, brukereSomSkalAvsluttes.getBegrunnelse());
                        return true;
                    } catch (Exception e) {
                        log.warn("Kunne ikke avslutte oppfølging", e);
                        return false;
                    }
                }).toList();

        var avsluttedeBrukere = resultat.stream().filter(it -> it).toList().size();
        var ikkeAvsluttedeBrukere = resultat.stream().filter(it -> !it).toList().size();

        log.info("Avsluttet oppfølging for {} brukere", avsluttedeBrukere);
        log.info("Kunne ikke avslutte oppfølging for {} brukere", ikkeAvsluttedeBrukere);

        return new AvsluttResultat(avsluttedeBrukere, ikkeAvsluttedeBrukere);
    }

    private void sjekkTilgangTilAdmin() {
        String subject = authContextHolder.getSubject()
                .orElseThrow(() -> new UnauthorizedException("Fant ingen subject i auth-context"));

        UserRole role = authContextHolder.getRole()
                .orElseThrow(() -> new UnauthorizedException("Fant ingen rolle i auth-context"));

        if (!authService.erSystemBruker()) throw new ForbiddenException("Må være systembruker");
        authService.sjekkAtApplikasjonErIAllowList(List.of(PTO_ADMIN));
    }

}
