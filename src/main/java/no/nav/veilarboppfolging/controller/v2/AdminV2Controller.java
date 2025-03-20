package no.nav.veilarboppfolging.controller.v2;

import lombok.RequiredArgsConstructor;
import no.nav.common.auth.context.AuthContextHolder;
import no.nav.common.auth.context.UserRole;
import no.nav.common.job.JobRunner;
import no.nav.veilarboppfolging.ForbiddenException;
import no.nav.veilarboppfolging.UnauthorizedException;
import no.nav.veilarboppfolging.service.AuthService;
import no.nav.veilarboppfolging.service.KafkaRepubliseringService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v2/admin")
@RequiredArgsConstructor
public class AdminV2Controller {

    public final static String POAO_ADMIN = "poao-admin";
    private final AuthService authService;
    private final KafkaRepubliseringService kafkaRepubliseringService;

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

    private void sjekkTilgangTilAdmin() {
        authService.sjekkAtApplikasjonErIAllowList(List.of(POAO_ADMIN));
        if (!authService.erInternBruker()) throw new ForbiddenException("Må være internbruker");
    }

}
