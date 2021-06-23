package no.nav.veilarboppfolging.controller.v2;

import lombok.RequiredArgsConstructor;
import no.nav.common.types.identer.Fnr;
import no.nav.veilarboppfolging.controller.response.HistorikkHendelse;
import no.nav.veilarboppfolging.service.AuthService;
import no.nav.veilarboppfolging.service.HistorikkService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v2/historikk")
@RequiredArgsConstructor
public class HistorikkV2Controller {

    private final HistorikkService historikkService;

    private final AuthService authService;

    @GetMapping
    public List<HistorikkHendelse> hentInnstillingsHistorikk(@RequestParam("fnr") Fnr fnr) {
        authService.skalVereInternBruker();
        return historikkService.hentInstillingsHistorikk(fnr);
    }

}
