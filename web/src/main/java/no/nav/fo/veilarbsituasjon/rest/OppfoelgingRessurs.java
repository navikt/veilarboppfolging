package no.nav.fo.veilarbsituasjon.rest;


import no.nav.fo.veilarbsituasjon.rest.domain.OppfoelgingskontraktResponse;
import no.nav.fo.veilarbsituasjon.services.OppfoelgingService;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.xml.datatype.XMLGregorianCalendar;
import java.time.LocalDate;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static no.nav.fo.veilarbsituasjon.utils.CalendarConverter.convertDateToXMLGregorianCalendar;
import static org.slf4j.LoggerFactory.getLogger;

@Component
@Path("/person/{fnr}")
@Produces(APPLICATION_JSON)
public class OppfoelgingRessurs {
    private static final Logger LOG = getLogger(OppfoelgingRessurs.class);
    private static final int MANEDER_BAK_I_TID = 2;
    private static final int MANEDER_FREM_I_TID = 1;

    private final OppfoelgingService oppfoelgingService;

    public OppfoelgingRessurs(OppfoelgingService oppfoelgingService) {
        this.oppfoelgingService = oppfoelgingService;
    }

    @GET
    @Path("/oppfoelging")
    public OppfoelgingskontraktResponse getOppfoelging(@PathParam("fnr") String fnr){
        LocalDate periodeFom = LocalDate.now().minusMonths(MANEDER_BAK_I_TID);
        LocalDate periodeTom = LocalDate.now().plusMonths(MANEDER_FREM_I_TID);
        XMLGregorianCalendar fom = convertDateToXMLGregorianCalendar(periodeFom);
        XMLGregorianCalendar tom = convertDateToXMLGregorianCalendar(periodeTom);

        LOG.info("Henter oppfoelging for {}", fnr);
        return oppfoelgingService.hentOppfoelgingskontraktListe(fom, tom, fnr);
    }

}
