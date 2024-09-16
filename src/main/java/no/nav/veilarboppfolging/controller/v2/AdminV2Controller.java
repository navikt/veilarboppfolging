package no.nav.veilarboppfolging.controller.v2;

import lombok.RequiredArgsConstructor;
import no.nav.common.auth.context.AuthContextHolder;
import no.nav.common.auth.context.UserRole;
import no.nav.common.job.JobRunner;
import no.nav.veilarboppfolging.ForbiddenException;
import no.nav.veilarboppfolging.UnauthorizedException;
import no.nav.veilarboppfolging.service.KafkaRepubliseringService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v2/admin")
@RequiredArgsConstructor
public class AdminV2Controller {

    public final static String PTO_ADMIN_SERVICE_USER = "srvpto-admin";

    private final AuthContextHolder authContextHolder;

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
        String subject = authContextHolder.getSubject()
                .orElseThrow(() -> new UnauthorizedException("Fant ikke subject"));

        UserRole role = authContextHolder.getRole()
                .orElseThrow(() -> new UnauthorizedException("Fant ikke rolle"));

        if (!PTO_ADMIN_SERVICE_USER.equals(subject) || !role.equals(UserRole.SYSTEM)) {
            throw new ForbiddenException("Bare Pto-admin har adminrettigheter");
        }
    }

}
