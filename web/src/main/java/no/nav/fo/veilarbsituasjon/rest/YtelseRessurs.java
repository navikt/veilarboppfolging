package no.nav.fo.veilarbsituasjon.rest;

import no.nav.fo.veilarbsituasjon.rest.domain.Ytelse;
import org.slf4j.Logger;
import no.nav.fo.veilarbsituasjon.services.YtelseskontraktService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.xml.datatype.*;
import java.time.*;
import java.util.*;

import static org.slf4j.LoggerFactory.getLogger;


@RestController
@RequestMapping("/person/{fnr}")
public class YtelseRessurs {
    private static final Logger LOG = getLogger(YtelseRessurs.class);

    @Autowired
    YtelseskontraktService ytelseskontraktService;

    @RequestMapping(value = "/ytelser", method = RequestMethod.GET, produces = "application/json")
    public List<Ytelse> getYtelser(@PathVariable String fnr) {
        LocalDate periodeFom = LocalDate.of(2005, 10, 10);
        LocalDate periodeTom = LocalDate.now();
        XMLGregorianCalendar fom = convertDateToXMLGregorianCalendar(periodeFom);
        XMLGregorianCalendar tom = convertDateToXMLGregorianCalendar(periodeTom);

        ytelseskontraktService.hentYtelseskontraktListe(fom, tom, fnr);
        LOG.error("Henter ytelse for {}", fnr);
        return Arrays.asList(new Ytelse("Arbeidsavklaringspenger", "Aktiv", LocalDate.now(), LocalDate.of(2016, Month.APRIL, 14), LocalDate.of(2017, Month.APRIL, 14)));
    }

    private XMLGregorianCalendar convertDateToXMLGregorianCalendar(LocalDate date) {
        GregorianCalendar gregorianCalendar = GregorianCalendar.from(date.atStartOfDay(ZoneId.systemDefault()));
        XMLGregorianCalendar xmlGregorianCalendar = null;
        try {
            xmlGregorianCalendar = DatatypeFactory.newInstance().newXMLGregorianCalendar(gregorianCalendar);
        } catch (DatatypeConfigurationException e) {
            e.printStackTrace();
        }
        return xmlGregorianCalendar;

    }
}
