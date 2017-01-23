package no.nav.fo.veilarbsituasjon.rest;

import no.nav.fo.veilarbsituasjon.rest.domain.*;
import no.nav.fo.veilarbsituasjon.services.OppfoelgingService;
import no.nav.fo.veilarbsituasjon.services.YtelseskontraktService;
import org.slf4j.Logger;
import org.springframework.web.bind.annotation.*;

import javax.xml.datatype.XMLGregorianCalendar;
import java.time.LocalDate;

import static no.nav.fo.veilarbsituasjon.utils.CalendarConverter.convertDateToXMLGregorianCalendar;
import static org.slf4j.LoggerFactory.getLogger;


@RestController
@RequestMapping("/person/{fnr}")
public class YtelseRessurs {
    private static final Logger LOG = getLogger(YtelseRessurs.class);

    final private YtelseskontraktService ytelseskontraktService;
    final private OppfoelgingService oppfoelgingService;


    public YtelseRessurs(YtelseskontraktService ytelseskontraktService, OppfoelgingService oppfoelgingService) {
        this.ytelseskontraktService = ytelseskontraktService;
        this.oppfoelgingService = oppfoelgingService;
    }

    @RequestMapping(value = "/ytelser", method = RequestMethod.GET, produces = "application/json")
    public YtelserResponse getYtelser(@PathVariable String fnr) {
        LocalDate periodeFom = LocalDate.of(2005, 10, 10);
        LocalDate periodeTom = LocalDate.now();
        XMLGregorianCalendar fom = convertDateToXMLGregorianCalendar(periodeFom);
        XMLGregorianCalendar tom = convertDateToXMLGregorianCalendar(periodeTom);

        LOG.info("Henter ytelse for {}", fnr);
        final YtelseskontraktResponse ytelseskontraktResponse = ytelseskontraktService.hentYtelseskontraktListe(fom, tom, fnr);
        final OppfoelgingskontraktResponse oppfoelgingskontraktResponse = oppfoelgingService.hentOppfoelgingskontraktListe(fom, tom, fnr);

        return new YtelserResponse()
                .withVedtaksliste(ytelseskontraktResponse.getVedtaksliste())
                .withYtelser(ytelseskontraktResponse.getYtelser())
                .withInnsatsgruppe(oppfoelgingskontraktResponse.getOppfoelgingskontrakter());

    }
}
