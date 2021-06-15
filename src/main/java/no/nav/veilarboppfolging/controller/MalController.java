package no.nav.veilarboppfolging.controller;

import lombok.RequiredArgsConstructor;
import no.nav.common.types.identer.Fnr;
import no.nav.veilarboppfolging.controller.response.Mal;
import no.nav.veilarboppfolging.domain.MalData;
import no.nav.veilarboppfolging.service.AuthService;
import no.nav.veilarboppfolging.service.MalService;
import no.nav.veilarboppfolging.utils.DtoMappers;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static java.util.stream.Collectors.toList;
import static no.nav.veilarboppfolging.utils.DtoMappers.tilDto;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/oppfolging")
public class MalController {

    private final MalService malService;

    private final AuthService authService;

    @GetMapping("/mal")
    public Mal hentMal(@RequestParam(required = false) Fnr fnr) {
        Fnr fodselsnummer = authService.hentIdentForEksternEllerIntern(fnr);
        return tilDto(malService.hentMal(fodselsnummer));
    }

    @GetMapping("/malListe")
    public List<Mal> hentMalListe(@RequestParam(required = false) Fnr fnr) {
        Fnr fodselsnummer = authService.hentIdentForEksternEllerIntern(fnr);
        List<MalData> malDataList = malService.hentMalList(fodselsnummer);

        return malDataList.stream()
                .map(DtoMappers::tilDto)
                .collect(toList());
    }

    @PostMapping("/mal")
    public Mal oppdaterMal(@RequestBody Mal mal, @RequestParam(required = false) Fnr fnr) {
        Fnr fodselsnummer = authService.hentIdentForEksternEllerIntern(fnr);
        String endretAvVeileder = authService.erEksternBruker() ? null : authService.getInnloggetBrukerIdent();

        return tilDto(malService.oppdaterMal(mal.getMal(), fodselsnummer, endretAvVeileder));
    }

}
