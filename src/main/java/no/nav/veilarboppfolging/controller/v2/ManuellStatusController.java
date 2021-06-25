package no.nav.veilarboppfolging.controller.v2;

import lombok.RequiredArgsConstructor;
import no.nav.common.types.identer.Fnr;
import no.nav.veilarboppfolging.service.AuthService;
import no.nav.veilarboppfolging.service.ManuellStatusService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v2/manuell")
public class ManuellStatusController {

    private final AuthService authService;

    private final ManuellStatusService manuellStatusService;

    /**
     * Brukes av veilarbpersonflatefs for å manuelt trigge synkronisering av manuell status med reservasjon fra DKIF(KRR).
     * @param fnr fnr/dnr til bruker som synkroniseringen skal gjøres på.
     */
    @PostMapping("/synkroniser-med-dkif")
    public void synkroniserManuellStatusMedDkif(@RequestParam Fnr fnr) {
        authService.skalVereInternBruker();
        manuellStatusService.synkroniserManuellStatusMedDkif(fnr);
    }

}
