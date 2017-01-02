package no.nav.fo.veilarbsituasjon.rest;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/person/{fnr}")
public class YtelseRessurs {

    @RequestMapping("/ytelser")
    public String getYtelser(@PathVariable String fnr) {
        return "Greetings from VeilArbSituasjon";
    }
}
