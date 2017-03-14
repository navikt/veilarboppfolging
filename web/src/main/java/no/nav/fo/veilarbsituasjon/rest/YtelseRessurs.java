package no.nav.fo.veilarbsituasjon.rest;

import no.nav.brukerdialog.security.context.SubjectHandler;
import no.nav.fo.veilarbsituasjon.rest.domain.*;
import no.nav.fo.veilarbsituasjon.services.OppfolgingService;
import no.nav.fo.veilarbsituasjon.services.YtelseskontraktService;
import no.nav.sbl.dialogarena.common.abac.pep.Pep;
import no.nav.sbl.dialogarena.common.abac.pep.domain.response.BiasedDecisionResponse;
import no.nav.sbl.dialogarena.common.abac.pep.domain.response.Decision;
import no.nav.sbl.dialogarena.common.abac.pep.exception.PepException;
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
    final private Pep pep;


    public YtelseRessurs(YtelseskontraktService ytelseskontraktService, OppfolgingService oppfolgingService, Pep pep) {
        this.ytelseskontraktService = ytelseskontraktService;
        this.oppfolgingService = oppfolgingService;
        this.pep = pep;
    }

    @GET
    @Path("/ytelser")
    public YtelserResponse getYtelser(@PathParam("fnr") String fnr) {

        final String ident = SubjectHandler.getSubjectHandler().getUid();
        BiasedDecisionResponse callAllowed;
        try {
            callAllowed = pep.isServiceCallAllowedWithIdent(ident, "veilarb", fnr);
        } catch (PepException e) {
            throw new InternalServerErrorException("something went wrong in PEP", e);
        }
        if (callAllowed.getBiasedDecision().equals(Decision.Deny)) {
            throw new NotAuthorizedException(ident + " doesn't have access to " + fnr);
        }

        LocalDate periodeFom = LocalDate.now().minusMonths(MANEDER_BAK_I_TID);
        LocalDate periodeTom = LocalDate.now().plusMonths(MANEDER_FREM_I_TID);
        XMLGregorianCalendar fom = convertDateToXMLGregorianCalendar(periodeFom);
        XMLGregorianCalendar tom = convertDateToXMLGregorianCalendar(periodeTom);

        LOG.info("Henter ytelse for {}", fnr);
        final YtelseskontraktResponse ytelseskontraktResponse = ytelseskontraktService.hentYtelseskontraktListe(fom, tom, fnr);
        final OppfolgingskontraktResponse oppfolgingskontraktResponse = oppfolgingService.hentOppfolgingskontraktListe(fom, tom, fnr);

        return new YtelserResponse()
                .withVedtaksliste(ytelseskontraktResponse.getVedtaksliste())
                .withYtelser(ytelseskontraktResponse.getYtelser())
                .withOppfoelgingskontrakter(oppfolgingskontraktResponse.getOppfoelgingskontrakter());
    }

}
