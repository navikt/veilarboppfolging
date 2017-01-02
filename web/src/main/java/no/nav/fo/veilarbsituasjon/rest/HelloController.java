package no.nav.fo.veilarbsituasjon.rest;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest")
public class HelloController {

    @RequestMapping("/hello")
    public String index() {
        return "Greetings from VeilArbSituasjon";
    }
}
