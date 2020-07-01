package no.nav.veilarboppfolging.controller;

import no.nav.veilarboppfolging.controller.domain.Mal;
import no.nav.veilarboppfolging.domain.MalData;
import no.nav.veilarboppfolging.services.AuthService;
import no.nav.veilarboppfolging.services.MalService;
import no.nav.veilarboppfolging.utils.DtoMappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static java.util.stream.Collectors.toList;
import static no.nav.veilarboppfolging.utils.DtoMappers.tilDto;

@RestController
@RequestMapping("/api/oppfolging")
public class MalController {

    private final MalService malService;

    private final AuthService authService;

    @Autowired
    public MalController(MalService malService, AuthService authService) {
        this.malService = malService;
        this.authService = authService;
    }

    @GetMapping("/mal")
    public Mal hentMal(@RequestParam(required = false) String fnr) {
        String fodselsnummer = authService.hentIdentForEksternEllerIntern(fnr);
        return tilDto(malService.hentMal(fodselsnummer));
    }

    @GetMapping("/malListe")
    public List<Mal> hentMalListe(@RequestParam(required = false) String fnr) {
        String fodselsnummer = authService.hentIdentForEksternEllerIntern(fnr);
        List<MalData> malDataList = malService.hentMalList(fodselsnummer);
        return malDataList.stream()
                .map(DtoMappers::tilDto)
                .collect(toList());
    }

    @PostMapping("/mal")
    public Mal oppdaterMal(@RequestBody Mal mal, @RequestParam(required = false) String fnr) {
        String fodselsnummer = authService.hentIdentForEksternEllerIntern(fnr);
        String endretAvVeileder = authService.erEksternBruker() ? null : authService.getInnloggetBrukerIdent();

        return tilDto(malService.oppdaterMal(mal.getMal(), fodselsnummer, endretAvVeileder));
    }

}
