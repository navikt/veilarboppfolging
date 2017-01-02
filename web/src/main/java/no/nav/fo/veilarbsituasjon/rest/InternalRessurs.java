package no.nav.fo.veilarbsituasjon.rest;


import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal")
public class InternalRessurs {

    @RequestMapping(value="/isAlive", method = RequestMethod.GET, produces = "application/json")
    public String isAlive() {
        return "Application: UP";
    }
}
