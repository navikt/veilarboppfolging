package no.nav.veilarboppfolging.controller;

import lombok.RequiredArgsConstructor;
import no.nav.common.auth.context.AuthContextHolder;
import no.nav.common.auth.context.UserRole;
import no.nav.common.job.JobRunner;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.NavIdent;
import no.nav.veilarboppfolging.controller.response.Veilarbportefoljeinfo;
import no.nav.veilarboppfolging.repository.OppfolgingsPeriodeRepository;
import no.nav.veilarboppfolging.repository.VeilederTilordningerRepository;
import no.nav.veilarboppfolging.repository.entity.ManuellStatusEntity;
import no.nav.veilarboppfolging.repository.entity.OppfolgingsperiodeEntity;
import no.nav.veilarboppfolging.repository.entity.VeilederTilordningEntity;
import no.nav.veilarboppfolging.service.AuthService;
import no.nav.veilarboppfolging.service.KafkaRepubliseringService;
import no.nav.veilarboppfolging.service.ManuellStatusService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.ZonedDateTime;
import java.util.Optional;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    public final static String PTO_ADMIN_SERVICE_USER = "srvpto-admin";

    private final AuthContextHolder authContextHolder;

    private final AuthService authService;

    private final KafkaRepubliseringService kafkaRepubliseringService;

    private final VeilederTilordningerRepository veilederTilordningerRepository;

    private final ManuellStatusService manuellStatusService;

    private final OppfolgingsPeriodeRepository oppfolgingsPeriodeRepository;

    @PostMapping("/republiser/oppfolgingsperioder")
    public String republiserOppfolgingsperioder() {
        sjekkTilgangTilAdmin();
        return JobRunner.runAsync("republiser-oppfolgingsperioder", kafkaRepubliseringService::republiserOppfolgingsperioder);
    }

    @PostMapping("/republiser/tilordnet-veileder")
    public String republiserTilordnetVeileder() {
        sjekkTilgangTilAdmin();
        return JobRunner.runAsync("republiser-tilordnet-veileder", kafkaRepubliseringService::republiserTilordnetVeileder);
    }

    @GetMapping("/hentVeilarbinfo/bruker")
    public Veilarbportefoljeinfo hentVeilarbportefoljeinfo(@RequestParam AktorId aktorId) {
        authService.skalVereSystemBruker();
        Optional<VeilederTilordningEntity> tilordningEntity = veilederTilordningerRepository.hentTilordnetVeileder(aktorId);
        boolean erManuell = manuellStatusService.hentManuellStatus(aktorId).map(ManuellStatusEntity::isManuell).orElse(false);
        ZonedDateTime startDato = oppfolgingsPeriodeRepository.hentGjeldendeOppfolgingsperiode(aktorId).map(OppfolgingsperiodeEntity::getStartDato).orElse(null);

        return new Veilarbportefoljeinfo().setVeilederId(tilordningEntity.map(x -> NavIdent.of(x.getVeilederId())).orElse(null))
                .setErUnderOppfolging(tilordningEntity.map(VeilederTilordningEntity::isOppfolging).orElse(false))
                .setNyForVeileder(tilordningEntity.map(VeilederTilordningEntity::isNyForVeileder).orElse(false))
                .setErManuell(erManuell)
                .setStartDato(startDato);
    }

    private void sjekkTilgangTilAdmin() {
        String subject = authContextHolder.getSubject()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        UserRole role = authContextHolder.getRole()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        if (!PTO_ADMIN_SERVICE_USER.equals(subject) || !role.equals(UserRole.SYSTEM)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }

}
