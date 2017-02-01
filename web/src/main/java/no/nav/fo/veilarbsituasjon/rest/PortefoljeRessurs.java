package no.nav.fo.veilarbsituasjon.rest;

import no.nav.fo.veilarbsituasjon.rest.domain.VeilederTilordning;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tilordning")
public class PortefoljeRessurs {

    @RequestMapping(value = "/", method = RequestMethod.POST, consumes = "application/json")
    public void postVeilederTilordninger(@RequestBody List<VeilederTilordning> veilederTilordninger) {
        System.out.println();
    }
}
