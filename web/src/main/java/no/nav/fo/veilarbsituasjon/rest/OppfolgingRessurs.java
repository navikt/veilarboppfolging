package no.nav.fo.veilarbsituasjon.rest;


import no.nav.fo.veilarbsituasjon.rest.domain.Oppfolgingsstatus;
import no.nav.fo.veilarbsituasjon.rest.domain.OppfolgingskontraktResponse;
import no.nav.fo.veilarbsituasjon.services.OppfolgingService;
import no.nav.fo.veilarbsituasjon.services.PepClient;
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
public class OppfolgingRessurs {
    private static final Logger LOG = getLogger(OppfolgingRessurs.class);
    private static final int MANEDER_BAK_I_TID = 2;
    private static final int MANEDER_FREM_I_TID = 1;

    private final OppfolgingService oppfolgingService;
    private final PepClient pepClient;

    public OppfolgingRessurs(OppfolgingService oppfolgingService, PepClient pepClient) {
        this.oppfolgingService = oppfolgingService;
        this.pepClient = pepClient;
    }

    @GET
    @Path("/oppfoelging")
    public OppfolgingskontraktResponse getOppfoelging(@PathParam("fnr") String fnr) {
        pepClient.isServiceCallAllowed(fnr);

        LocalDate periodeFom = LocalDate.now().minusMonths(MANEDER_BAK_I_TID);
        LocalDate periodeTom = LocalDate.now().plusMonths(MANEDER_FREM_I_TID);
        XMLGregorianCalendar fom = convertDateToXMLGregorianCalendar(periodeFom);
        XMLGregorianCalendar tom = convertDateToXMLGregorianCalendar(periodeTom);

        LOG.info("Henter oppfoelging for {}", fnr);
        return oppfolgingService.hentOppfolgingskontraktListe(fom, tom, fnr);
    }

    @GET
    @Path("/oppfoelgingsstatus")
    public Oppfolgingsstatus getOppfoelginsstatus(@PathParam("fnr") String fnr) {
        pepClient.isServiceCallAllowed(fnr);

        LOG.info("Henter oppfølgingsstatus for {}", fnr);
        return oppfolgingService.hentOppfolgingsstatus(fnr);
    }
}
