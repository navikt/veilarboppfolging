package no.nav.fo.veilarbsituasjon.rest;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    @RequestMapping("/veilarbsituasjon")
    public String index() {
        return "Greetings from VeilArbSituasjon";
    }
}
