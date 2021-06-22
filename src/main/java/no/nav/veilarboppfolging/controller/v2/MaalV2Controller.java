package no.nav.veilarboppfolging.controller.v2;

import lombok.RequiredArgsConstructor;
import no.nav.common.types.identer.Fnr;
import no.nav.veilarboppfolging.controller.response.Mal;
import no.nav.veilarboppfolging.repository.entity.MaalEntity;
import no.nav.veilarboppfolging.service.AuthService;
import no.nav.veilarboppfolging.service.MalService;
import no.nav.veilarboppfolging.utils.DtoMappers;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static java.util.stream.Collectors.toList;
import static no.nav.veilarboppfolging.utils.DtoMappers.tilDto;

@RestController
@RequestMapping("/api/v2/maal")
@RequiredArgsConstructor
public class MaalV2Controller {

    private final MalService malService;

    private final AuthService authService;

    @GetMapping
    public Mal hentMal(@RequestParam(required = false) Fnr fnr) {
        Fnr fodselsnummer = authService.hentIdentForEksternEllerIntern(fnr);
        return tilDto(malService.hentMal(fodselsnummer));
    }

    @GetMapping("/historikk")
    public List<Mal> hentMalListe(@RequestParam(required = false) Fnr fnr) {
        Fnr fodselsnummer = authService.hentIdentForEksternEllerIntern(fnr);
        List<MaalEntity> malDataList = malService.hentMalList(fodselsnummer);

        return malDataList.stream()
                .map(DtoMappers::tilDto)
                .collect(toList());
    }

    @PostMapping
    public Mal oppdaterMal(@RequestBody Mal mal, @RequestParam(required = false) Fnr fnr) {
        Fnr fodselsnummer = authService.hentIdentForEksternEllerIntern(fnr);
        String endretAvVeileder = authService.erEksternBruker() ? null : authService.getInnloggetBrukerIdent();

        return tilDto(malService.oppdaterMal(mal.getMal(), fodselsnummer, endretAvVeileder));
    }

}
