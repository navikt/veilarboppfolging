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
    private List<VeilederTilordning> feilendeTilordninger;
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
        feilendeTilordninger = new ArrayList<>();

        for (VeilederTilordning tilordning : tilordninger) {
            try {
                final String fnr = tilordning.getBrukerFnr();
                pepClient.isServiceCallAllowed(fnr);

                String aktoerId = ofNullable(aktoerIdService.findAktoerId(fnr)).
                        orElseThrow(() -> new IllegalArgumentException("Aktoerid ikke funnet"));


                val oppdatertBruker = OppfolgingBruker
                        .builder()
                        .veileder(tilordning.getTilVeilederId())
                        .aktoerid(aktoerId)
                        .build();

                settVeilederDersomFraVeilederErOK(oppdatertBruker, tilordning);
            } catch (PepException e) {
                LOG.error("Kall til ABAC feilet");
                feilendeTilordninger.add(tilordning);
            } catch (IllegalArgumentException e) {
                LOG.error("Aktoerid ikke funnet", e);
                feilendeTilordninger.add(tilordning);
            } catch (NotAuthorizedException e) {
                LOG.warn("Request is not authorized", e);
                feilendeTilordninger.add(tilordning);
            } catch (Exception e) {
                LOG.error("Det skjedde en feil ved tildeling av veileder", e);
                feilendeTilordninger.add(tilordning);
            } finally {
                feed.activateWebhook();
            }
        }

        TilordneVeilederResponse response = new TilordneVeilederResponse()
                .setFeilendeTilordninger(feilendeTilordninger);

        if (feilendeTilordninger.isEmpty()) {
            response.setResultat("OK: Veiledere tilordnet");
            return Response.ok().entity(response).build();
        } else {
            response.setResultat("WARNING: Noen brukere kunne ikke tilordnes en veileder");
            return Response.ok().entity(response).build();
        }

    }

    private void skrivTilDatabase(OppfolgingBruker bruker, VeilederTilordning tilordning) {
        try {
            brukerRepository.upsertVeilederTilordning(bruker);
            LOG.debug(String.format("Veileder %s tilordnet aktoer %s", bruker.getVeileder(), bruker.getAktoerid()));
        } catch (Exception e) {
            feilendeTilordninger.add(tilordning);
            LOG.error(String.format("Kunne ikke tilordne veileder %s til aktoer %s", bruker.getVeileder(), bruker.getAktoerid()), e);
            throw e;
        }
    }

    @Transactional
    private void settVeilederDersomFraVeilederErOK(OppfolgingBruker brukerTilOppdatering, VeilederTilordning tilordning) {
        val bruker = brukerRepository.hentTilordningForAktoer(brukerTilOppdatering.getAktoerid());
        boolean fraVeilederErOk = bruker == null ||
                bruker.getVeileder() == null ||
                bruker.getVeileder().equals(tilordning.getFraVeilederId());

        if (fraVeilederErOk) {
            if (bruker == null || bruker.getOppfolging()){
                situasjonRepository.opprettOppfolgingsperiode(
                        Oppfolgingsperiode
                                .builder()
                                .aktorId(brukerTilOppdatering.getAktoerid())
                                .build());
            }

            skrivTilDatabase(brukerTilOppdatering, tilordning);
        } else {
            feilendeTilordninger.add(tilordning);
            LOG.info("Aktoerid {} kunne ikke tildeles ettersom fraVeileder er feil", bruker.getAktoerid());
        }
    }

    static boolean kanSetteNyVeileder(String fraVeileder, String tilVeileder, String eksisterendeVeileder) {
        if (tilVeileder == null) {
            return false;
        }
        return eksisterendeVeileder == null || eksisterendeVeileder.equals(fraVeileder);
    }
}