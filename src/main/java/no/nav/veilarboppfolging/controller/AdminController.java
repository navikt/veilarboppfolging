package no.nav.veilarboppfolging.controller;

import lombok.RequiredArgsConstructor;
import no.nav.common.auth.context.AuthContextHolder;
import no.nav.common.auth.context.UserRole;
import no.nav.common.job.JobRunner;
import no.nav.veilarboppfolging.service.KafkaRepubliseringService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

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

    @PostMapping("/republiser/endring-pa-ny-for-veileder-brukere-under-oppfolging")
    public String republiserEndringPaNyForVeileder() {
        sjekkTilgangTilAdmin();
        return JobRunner.runAsync(
                "republiser-endring-pa-ny-for-veileder-brukere-under-oppfolging",
                kafkaRepubliseringService::republiserEndringPaNyForVeilederForBrukereUnderOppfolging
        );
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
