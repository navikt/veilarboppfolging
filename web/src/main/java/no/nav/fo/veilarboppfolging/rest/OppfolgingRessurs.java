package no.nav.fo.veilarboppfolging.rest;


import io.swagger.annotations.Api;
import no.nav.apiapp.security.PepClient;
import no.nav.fo.veilarboppfolging.domain.OppfolgingskontraktResponse;
import no.nav.fo.veilarboppfolging.domain.Oppfolgingsstatus;
import no.nav.fo.veilarboppfolging.mappers.OppfolgingMapper;
import no.nav.fo.veilarboppfolging.services.OppfolgingService;
import no.nav.sbl.dialogarena.common.abac.pep.exception.PepException;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.xml.datatype.XMLGregorianCalendar;
import java.time.LocalDate;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static no.nav.fo.veilarboppfolging.utils.CalendarConverter.convertDateToXMLGregorianCalendar;
import static org.slf4j.LoggerFactory.getLogger;

@Component
@Api(value = "Oppfølging")
@Path("/person/{fnr}")
@Produces(APPLICATION_JSON)
public class OppfolgingRessurs {
    private static final Logger LOG = getLogger(OppfolgingRessurs.class);
    private static final int MANEDER_BAK_I_TID = 2;
    private static final int MANEDER_FREM_I_TID = 1;

    private final OppfolgingService oppfolgingService;
    private final OppfolgingMapper oppfolgingMapper;
    private final PepClient pepClient;

    public OppfolgingRessurs(OppfolgingService oppfolgingService, OppfolgingMapper oppfolgingMapper, PepClient pepClient) {
        this.oppfolgingService = oppfolgingService;
        this.oppfolgingMapper = oppfolgingMapper;
        this.pepClient = pepClient;
    }

    @GET
    @Path("/oppfoelging")
    public OppfolgingskontraktResponse getOppfoelging(@PathParam("fnr") String fnr) throws PepException {
        pepClient.sjekkTilgangTilFnr(fnr);
        LocalDate periodeFom = LocalDate.now().minusMonths(MANEDER_BAK_I_TID);
        LocalDate periodeTom = LocalDate.now().plusMonths(MANEDER_FREM_I_TID);
        XMLGregorianCalendar fom = convertDateToXMLGregorianCalendar(periodeFom);
        XMLGregorianCalendar tom = convertDateToXMLGregorianCalendar(periodeTom);

        LOG.info("Henter oppfoelging for fnr");
        return oppfolgingMapper.tilOppfolgingskontrakt(oppfolgingService.hentOppfolgingskontraktListe(fom, tom, fnr));
    }

    @GET
    @Path("/oppfoelgingsstatus")
    public Oppfolgingsstatus getOppfoelginsstatus(@PathParam("fnr") String fnr) throws PepException {
        pepClient.sjekkTilgangTilFnr(fnr);

        LOG.info("Henter oppfølgingsstatus for fnr");
        return oppfolgingService.hentOppfolgingsstatus(fnr);
    }
}
