package no.nav.fo.veilarbsituasjon.rest;

import no.nav.fo.veilarbsituasjon.rest.domain.Ytelse;
import org.slf4j.Logger;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.Month;
import java.util.Arrays;
import java.util.List;

import static org.slf4j.LoggerFactory.getLogger;


@RestController
@RequestMapping("/person/{fnr}")
public class YtelseRessurs {
    private static final Logger LOG = getLogger(YtelseRessurs.class);

    @RequestMapping(value = "/ytelser", method = RequestMethod.GET, produces = "application/json")
    public List<Ytelse> getYtelser(@PathVariable String fnr) {
        LOG.error("Henter ytelse for {}", fnr);
        return Arrays.asList(new Ytelse("Arbeidsavklaringspenger", "Aktiv", LocalDate.now(), LocalDate.of(2016, Month.APRIL, 14), LocalDate.of(2017, Month.APRIL, 14)));
    }
}
