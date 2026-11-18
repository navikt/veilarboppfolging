package no.nav.veilarboppfolging.controller;

import no.nav.common.types.identer.Fnr;
import no.nav.veilarboppfolging.controller.response.Maal;
import no.nav.veilarboppfolging.repository.entity.MaalEntity;
import no.nav.veilarboppfolging.service.AuthService;
import no.nav.veilarboppfolging.service.MaalService;
import no.nav.veilarboppfolging.utils.DtoMappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static java.util.stream.Collectors.toList;
import static no.nav.veilarboppfolging.utils.DtoMappers.tilDto;

@RestController
@RequestMapping("/api/oppfolging")
public class MaalController {

    private final MaalService maalService;
    private final AuthService authService;

    @Autowired
    public MaalController(MaalService maalService, AuthService authService) {
        this.maalService = maalService;
        this.authService = authService;
    }

    @GetMapping("/mal")
    public Maal hentMal(@RequestParam(required = false) Fnr fnr) {
        Fnr fodselsnummer = authService.hentIdentForEksternEllerIntern(fnr);
        return tilDto(maalService.hentMal(fodselsnummer));
    }
}
