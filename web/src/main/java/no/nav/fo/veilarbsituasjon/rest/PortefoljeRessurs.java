package no.nav.fo.veilarbsituasjon.rest;

import static java.util.Optional.ofNullable;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import io.swagger.annotations.Api;
import no.nav.fo.feed.producer.FeedProducer;
import no.nav.fo.veilarbsituasjon.db.BrukerRepository;
import no.nav.fo.veilarbsituasjon.rest.domain.OppfolgingBruker;
import no.nav.fo.veilarbsituasjon.rest.domain.TilordneVeilederResponse;
import no.nav.fo.veilarbsituasjon.rest.domain.VeilederTilordning;
import no.nav.fo.veilarbsituasjon.services.AktoerIdService;
import no.nav.fo.veilarbsituasjon.services.PepClient;
import no.nav.sbl.dialogarena.common.abac.pep.exception.PepException;

@Component
@Path("")
@Api(value = "Portefolje")
public class PortefoljeRessurs {

    private static final Logger LOG = getLogger(PortefoljeRessurs.class);

    private AktoerIdService aktoerIdService;
    private BrukerRepository brukerRepository;
    private final PepClient pepClient;

    private FeedProducer<OppfolgingBruker> feed;

    public PortefoljeRessurs(AktoerIdService aktoerIdService, BrukerRepository brukerRepository, PepClient pepClient, FeedProducer<OppfolgingBruker> feed) {
        this.aktoerIdService = aktoerIdService;
        this.brukerRepository = brukerRepository;
        this.pepClient = pepClient;
        this.feed = feed;
    }

    @POST
    @Consumes("application/json")
    @Produces("application/json")
    @Path("/tilordneveileder")
    public Response postVeilederTilordninger(List<VeilederTilordning> tilordninger) {
        List<VeilederTilordning> feilendeTilordninger = new ArrayList<>();

        for (VeilederTilordning tilordning : tilordninger) {
            try {
                final String fnr = tilordning.getBrukerFnr();
                pepClient.isServiceCallAllowed(fnr);

                String aktoerId = finnAktorId(fnr);

                String eksisterendeVeileder = brukerRepository.hentVeilederForAktoer(aktoerId);

                boolean fraVeilederErOk = eksisterendeVeileder == null || eksisterendeVeileder.equals(tilordning.getFraVeilederId());
                if (fraVeilederErOk) {
                    skrivTilDatabase(aktoerId, tilordning.getTilVeilederId());
                } else {
                    feilendeTilordninger.add(tilordning);
                    LOG.info("Aktoerid {} kunne ikke tildeles ettersom fraVeileder er feil", aktoerId);
                }
            } catch (Exception e) {
                feilendeTilordninger.add(tilordning);
                loggFeilsituasjon(e);
            }
        }

        TilordneVeilederResponse response = new TilordneVeilederResponse().setFeilendeTilordninger(feilendeTilordninger);

        if (feilendeTilordninger.isEmpty()) {
            response.setResultat("OK: Veiledere tilordnet");
        } else {
            response.setResultat("WARNING: Noen brukere kunne ikke tilordnes en veileder");
        }
        if(tilordninger.size() > feilendeTilordninger.size()) {
            feed.activateWebhook();
        }
        return Response.ok().entity(response).build();

    }

    private String finnAktorId(final String fnr) {
        return ofNullable(aktoerIdService.findAktoerId(fnr)).
                orElseThrow(() -> new IllegalArgumentException("Aktoerid ikke funnet"));
    }

    private void loggFeilsituasjon(Exception e) {
        if(e instanceof NotAuthorizedException) {
            LOG.warn("Request is not authorized", e);
        } else {
            LOG.error(loggMeldingForException(e), e);
        }
    }

    private String loggMeldingForException(Exception e) {
        return (e instanceof PepException) ? "Kall til ABAC feilet" 
                : (e instanceof IllegalArgumentException) ? "Aktoerid ikke funnet" 
                : "Det skjedde en feil ved tildeling av veileder";
    }

    // Tror ikke denne annotasjonen funker. @Transactional funker bare på public metoder, så vidt jeg vet. 
    // Men det er vel uansett ikke behov for @Transactional her? Dersom selve databaseoppdateringen feiler, 
    // trenger den ikke rulles tilbake. Det eneste som kan feile bortsett fra databaseoppdateringen er om det skjer en 
    // exception i den påfølgende loggingen. Det kan løses på flere måter, f.eks. ved å logge før databaseoppdatering gjøres
    // i stedet for etterpå.
    @Transactional
    private void skrivTilDatabase(String aktoerId, String veileder) {
        try {
            brukerRepository.upsertVeilederTilordning(aktoerId, veileder);
            LOG.debug(String.format("Veileder %s tilordnet aktoer %s", veileder, aktoerId));
        } catch (Exception e) {
            LOG.error(String.format("Kunne ikke tilordne veileder %s til aktoer %s", veileder, aktoerId), e);
            throw e;
        }
    }

    static boolean kanSetteNyVeileder(String fraVeileder, String tilVeileder, String eksisterendeVeileder) {
        if (tilVeileder == null) {
            return false;
        }
        return eksisterendeVeileder == null || eksisterendeVeileder.equals(fraVeileder);
    }
}