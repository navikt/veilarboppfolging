package no.nav.veilarboppfolging.controller;

import lombok.RequiredArgsConstructor;
import no.nav.common.types.identer.Fnr;
import no.nav.veilarboppfolging.service.AuthService;
import no.nav.veilarboppfolging.service.ManuellStatusService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/manuell")
@RequiredArgsConstructor
public class ManuellStatusController {

    private final AuthService authService;

    private final ManuellStatusService manuellStatusService;

    @PostMapping("/oppdater")
    public void oppdaterManuellStatus(@RequestParam Fnr fnr) {
        authService.skalVereInternBruker();
        manuellStatusService.oppdaterManuellStatus(fnr);
    }


}
