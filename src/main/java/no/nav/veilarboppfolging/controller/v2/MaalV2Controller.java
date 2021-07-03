package no.nav.veilarboppfolging.controller.v2;

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
@RequestMapping("/api/v2/maal")
@RequiredArgsConstructor
public class MaalV2Controller {

    private final MaalService maalService;

    private final AuthService authService;

    @GetMapping
    public Maal hentMal(@RequestParam(required = false) Fnr fnr) {
        Fnr fodselsnummer = authService.hentIdentForEksternEllerIntern(fnr);
        return tilDto(maalService.hentMal(fodselsnummer));
    }

    @GetMapping("/historikk") // TODO: /alle?
    public List<Maal> hentMalListe(@RequestParam(value = "fnr", required = false) Fnr fnr) {
        Fnr fodselsnummer = authService.hentIdentForEksternEllerIntern(fnr);
        List<MaalEntity> malDataList = maalService.hentMalList(fodselsnummer);

        return malDataList.stream()
                .map(DtoMappers::tilDto)
                .collect(toList());
    }

    @PostMapping
    public Maal oppdaterMal(@RequestBody Maal maal, @RequestParam(value = "fnr", required = false) Fnr fnr) {
        Fnr fodselsnummer = authService.hentIdentForEksternEllerIntern(fnr);
        String endretAvVeileder = authService.erEksternBruker() ? null : authService.getInnloggetBrukerIdent();

        return tilDto(maalService.oppdaterMal(maal.getMal(), fodselsnummer, endretAvVeileder));
    }

}
