package no.nav.fo.veilarbsituasjon.rest;

import no.nav.fo.veilarbsituasjon.rest.domain.YtelseskontraktResponse;
import no.nav.fo.veilarbsituasjon.services.YtelseskontraktService;
import org.slf4j.Logger;
import org.springframework.web.bind.annotation.*;

import javax.xml.datatype.*;
import java.time.*;

import static no.nav.fo.veilarbsituasjon.utils.CalendarConverter.convertDateToXMLGregorianCalendar;
import static org.slf4j.LoggerFactory.getLogger;


@RestController
@RequestMapping("/person/{fnr}")
public class YtelseRessurs {
    private static final Logger LOG = getLogger(YtelseRessurs.class);

    final private YtelseskontraktService ytelseskontraktService;

    public YtelseRessurs(YtelseskontraktService ytelseskontraktService) {
        this.ytelseskontraktService = ytelseskontraktService;
    }

    @RequestMapping(value = "/ytelser", method = RequestMethod.GET, produces = "application/json")
    public YtelseskontraktResponse getYtelser(@PathVariable String fnr) {
        LocalDate periodeFom = LocalDate.of(2005, 10, 10);
        LocalDate periodeTom = LocalDate.now();
        XMLGregorianCalendar fom = convertDateToXMLGregorianCalendar(periodeFom);
        XMLGregorianCalendar tom = convertDateToXMLGregorianCalendar(periodeTom);

        LOG.info("Henter ytelse for {}", fnr);
        return ytelseskontraktService.hentYtelseskontraktListe(fom, tom, fnr);
    }


}
