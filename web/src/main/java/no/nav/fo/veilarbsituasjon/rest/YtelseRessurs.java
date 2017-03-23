package no.nav.fo.veilarbsituasjon.rest;

import no.nav.fo.veilarbsituasjon.mappers.OppfolgingMapper;
import no.nav.fo.veilarbsituasjon.mappers.YtelseskontraktMapper;
import no.nav.fo.veilarbsituasjon.rest.domain.*;
import no.nav.fo.veilarbsituasjon.services.*;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.xml.datatype.XMLGregorianCalendar;
import java.time.LocalDate;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static no.nav.fo.veilarbsituasjon.utils.CalendarConverter.convertDateToXMLGregorianCalendar;
import static org.slf4j.LoggerFactory.getLogger;

@Path("/person/{fnr}")
@Component
@Produces(APPLICATION_JSON)
public class YtelseRessurs {
    private static final Logger LOG = getLogger(YtelseRessurs.class);
    private static final int MANEDER_BAK_I_TID = 2;
    private static final int MANEDER_FREM_I_TID = 1;

    final private YtelseskontraktService ytelseskontraktService;
    final private OppfolgingService oppfolgingService;
    final private OppfolgingMapper oppfolgingMapper;
    final private YtelseskontraktMapper ytelseskontraktMapper;
    private final PepClient pepClient;

    public YtelseRessurs(YtelseskontraktService ytelseskontraktService,
                         OppfolgingService oppfolgingService,
                         OppfolgingMapper oppfolgingMapper,
                         YtelseskontraktMapper ytelseskontraktMapper,
                         PepClient pepClient) {
        this.ytelseskontraktService = ytelseskontraktService;
        this.oppfolgingService = oppfolgingService;
        this.oppfolgingMapper = oppfolgingMapper;
        this.ytelseskontraktMapper = ytelseskontraktMapper;
        this.pepClient = pepClient;
    }

    @GET
    @Path("/ytelser")
    public YtelserResponse getYtelser(@PathParam("fnr") String fnr) {

        pepClient.isServiceCallAllowed(fnr);

        LocalDate periodeFom = LocalDate.now().minusMonths(MANEDER_BAK_I_TID);
        LocalDate periodeTom = LocalDate.now().plusMonths(MANEDER_FREM_I_TID);
        XMLGregorianCalendar fom = convertDateToXMLGregorianCalendar(periodeFom);
        XMLGregorianCalendar tom = convertDateToXMLGregorianCalendar(periodeTom);

        LOG.info("Henter ytelse for {}", fnr);
        final YtelseskontraktResponse ytelseskontraktResponse = ytelseskontraktMapper.tilYtelseskontrakt(ytelseskontraktService.hentYtelseskontraktListe(fom, tom, fnr));
        final OppfolgingskontraktResponse oppfolgingskontraktResponse = oppfolgingMapper.tilOppfolgingskontrakt(oppfolgingService.hentOppfolgingskontraktListe(fom, tom, fnr));

        return new YtelserResponse()
                .withVedtaksliste(ytelseskontraktResponse.getVedtaksliste())
                .withYtelser(ytelseskontraktResponse.getYtelser())
                .withOppfoelgingskontrakter(oppfolgingskontraktResponse.getOppfoelgingskontrakter());
    }

}
