package no.nav.fo.veilarbsituasjon.rest;

import io.swagger.annotations.Api;
import lombok.val;
import no.nav.fo.feed.producer.FeedProducer;
import no.nav.fo.veilarbsituasjon.db.BrukerRepository;
import no.nav.fo.veilarbsituasjon.db.SituasjonRepository;
import no.nav.fo.veilarbsituasjon.domain.Oppfolgingsperiode;
import no.nav.fo.veilarbsituasjon.rest.domain.OppfolgingBruker;
import no.nav.fo.veilarbsituasjon.rest.domain.TilordneVeilederResponse;
import no.nav.fo.veilarbsituasjon.rest.domain.VeilederTilordning;
import no.nav.fo.veilarbsituasjon.services.AktoerIdService;
import no.nav.fo.veilarbsituasjon.services.PepClient;
import no.nav.sbl.dialogarena.common.abac.pep.exception.PepException;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;

import static java.util.Optional.ofNullable;
import static org.slf4j.LoggerFactory.getLogger;

@Component
@Path("")
@Api(value = "Portefolje")
public class PortefoljeRessurs {

    private static final Logger LOG = getLogger(PortefoljeRessurs.class);

    private AktoerIdService aktoerIdService;
    private BrukerRepository brukerRepository;
    private final PepClient pepClient;
    private final SituasjonRepository situasjonRepository;

    private FeedProducer<OppfolgingBruker> feed;

    public PortefoljeRessurs(AktoerIdService aktoerIdService,
                             BrukerRepository brukerRepository,
                             PepClient pepClient,
                             FeedProducer<OppfolgingBruker> feed,
                             SituasjonRepository situasjonRepository) {
        this.aktoerIdService = aktoerIdService;
        this.brukerRepository = brukerRepository;
        this.pepClient = pepClient;
        this.feed = feed;
        this.situasjonRepository = situasjonRepository;
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

                val bruker = brukerRepository.hentTilordningForAktoer(aktoerId);

                if (bruker == null || kanSetteNyVeileder(bruker.getVeileder(), tilordning.getFraVeilederId())) {
                    skrivTilDatabase(bruker, aktoerId, tilordning.getTilVeilederId());
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
        if (tilordninger.size() > feilendeTilordninger.size()) {
            kallWebhook();
        }
        return Response.ok().entity(response).build();

    }

    private void kallWebhook() {
        try {
            feed.activateWebhook();
        } catch (Exception e) {
            // Logger feilen, men bryr oss ikke om det. At webhooken feiler påvirker ikke funksjonaliteten
            // men gjør at endringen kommer senere inn i portefølje
            LOG.warn("Webhook feilet", e);
        }
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

    @Transactional
    private void skrivTilDatabase(OppfolgingBruker bruker, String aktoerId, String veileder) {
        try {
            if (bruker == null || !bruker.getOppfolging()){
                situasjonRepository.opprettOppfolgingsperiode(
                        Oppfolgingsperiode
                                .builder()
                                .aktorId(aktoerId)
                                .build());
            }
            brukerRepository.upsertVeilederTilordning(aktoerId, veileder);
            LOG.debug(String.format("Veileder %s tilordnet aktoer %s", veileder, aktoerId));
        } catch (Exception e) {
            LOG.error(String.format("Kunne ikke tilordne veileder %s til aktoer %s", veileder, aktoerId), e);
            throw e;
        }
    }

    static boolean kanSetteNyVeileder(String eksisterendeVeileder, String fraVeileder) {
        return eksisterendeVeileder == null || eksisterendeVeileder.equals(fraVeileder);
    }
}