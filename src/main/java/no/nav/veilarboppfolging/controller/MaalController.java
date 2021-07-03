package no.nav.veilarboppfolging.controller;

import lombok.RequiredArgsConstructor;
import no.nav.common.types.identer.Fnr;
import no.nav.veilarboppfolging.controller.response.Maal;
import no.nav.veilarboppfolging.repository.entity.MaalEntity;
import no.nav.veilarboppfolging.service.AuthService;
import no.nav.veilarboppfolging.service.MaalService;
import no.nav.veilarboppfolging.utils.DtoMappers;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static java.util.stream.Collectors.toList;
import static no.nav.veilarboppfolging.utils.DtoMappers.tilDto;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/oppfolging")
public class MaalController {

    private final MaalService maalService;

    private final AuthService authService;

    @GetMapping("/mal")
    public Maal hentMal(@RequestParam(required = false) Fnr fnr) {
        Fnr fodselsnummer = authService.hentIdentForEksternEllerIntern(fnr);
        return tilDto(maalService.hentMal(fodselsnummer));
    }

    @GetMapping("/malListe")
    public List<Maal> hentMalListe(@RequestParam(required = false) Fnr fnr) {
        Fnr fodselsnummer = authService.hentIdentForEksternEllerIntern(fnr);
        List<MaalEntity> malDataList = maalService.hentMalList(fodselsnummer);

        return malDataList.stream()
                .map(DtoMappers::tilDto)
                .collect(toList());
    }

    @PostMapping("/mal")
    public Maal oppdaterMal(@RequestBody Maal maal, @RequestParam(required = false) Fnr fnr) {
        Fnr fodselsnummer = authService.hentIdentForEksternEllerIntern(fnr);
        String endretAvVeileder = authService.erEksternBruker() ? null : authService.getInnloggetBrukerIdent();

        return tilDto(maalService.oppdaterMal(maal.getMal(), fodselsnummer, endretAvVeileder));
    }

}
