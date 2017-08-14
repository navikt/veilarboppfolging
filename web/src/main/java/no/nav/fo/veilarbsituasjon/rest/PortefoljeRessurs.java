package no.nav.fo.veilarbsituasjon.rest;

import io.swagger.annotations.Api;
import no.nav.fo.veilarbsituasjon.db.BrukerRepository;
import no.nav.fo.veilarbsituasjon.domain.OppfolgingBruker;
import no.nav.fo.veilarbsituasjon.rest.domain.TilordneVeilederResponse;
import no.nav.fo.veilarbsituasjon.rest.domain.VeilederTilordning;
import no.nav.fo.veilarbsituasjon.services.AktoerIdService;
import no.nav.fo.veilarbsituasjon.services.PepClient;
import no.nav.fo.veilarbsituasjon.services.TilordningService;
import no.nav.sbl.dialogarena.common.abac.pep.exception.PepException;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;
import static org.slf4j.LoggerFactory.getLogger;

@Component
@Path("")
@Api(value = "Portefolje")
public class PortefoljeRessurs {

    private static final Logger LOG = getLogger(PortefoljeRessurs.class);

    private final AktoerIdService aktoerIdService;
    private final BrukerRepository brukerRepository;
    private final PepClient pepClient;
    private final TilordningService tilordningService;

    public PortefoljeRessurs(TilordningService tilordningService, AktoerIdService aktoerIdService, BrukerRepository brukerRepository, PepClient pepClient) {
        this.tilordningService = tilordningService;
        this.aktoerIdService = aktoerIdService;
        this.brukerRepository = brukerRepository;
        this.pepClient = pepClient;
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

                if (kanSetteNyVeileder(eksisterendeVeileder, tilordning.getFraVeilederId())) {
                    tilordningService.skrivTilDataBaseOgLeggPaaKo(aktoerId, tilordning.getTilVeilederId());
                } else {
                    feilendeTilordninger.add(tilordning);
                    LOG.info("Aktoerid {} kunne ikke tildeles ettersom fraVeileder er feil", aktoerId);
                }
            } catch (Exception e) {
                feilendeTilordninger.add(tilordning);
                logFeilSituasjon(e);
            }
        }

        TilordneVeilederResponse response = new TilordneVeilederResponse().setFeilendeTilordninger(feilendeTilordninger);

        if (feilendeTilordninger.isEmpty()) {
            response.setResultat("OK: Veiledere tilordnet");
        } else {
            response.setResultat("WARNING: Noen brukere kunne ikke tilordnes en veileder");
        }

        return Response.ok().entity(response).build();
    }

    private void logFeilSituasjon(Exception e) {
        if (e instanceof NotAuthorizedException) {
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

    private String finnAktorId(String fnr) {
        return ofNullable(aktoerIdService.findAktoerId(fnr)).
                orElseThrow(() -> new IllegalArgumentException("Aktoerid ikke funnet"));
    }

    @GET
    @Path("/sendalleveiledertilordninger")
    public Response getSendAlleVeiledertilordninger() {
        LOG.info("Sender alle veiledertilordninger");
        return leggOppfolgingsbrukerPaKo(brukerRepository.hentAlleVeiledertilordninger());
    }

    @GET
    @Path("/sendveiledertilordninger")
    public Response getSendVeiledertilordninger(@QueryParam("fnr") List<String> fnrs) {
        LOG.info("Sender veiledertilordninger for {}", fnrs);
        List<OppfolgingBruker> brukere = fnrs
                .stream()
                .map(aktoerIdService::findAktoerId)
                .map(brukerRepository::hentVeiledertilordningForAktoer)
                .collect(Collectors.toList());

        return leggOppfolgingsbrukerPaKo(brukere);
    }

    private Response leggOppfolgingsbrukerPaKo(List<OppfolgingBruker> brukere) {
        long start = System.currentTimeMillis();
        int sendt = 0;
        int feilet = 0;
        for (OppfolgingBruker bruker : brukere) {
            try {
                tilordningService.leggPaaKo(bruker);
                sendt++;
            } catch (Exception e) {
                feilet++;
            }
        }
        String status = String.format("Sending fullført. Sendt: %1$s/%2$s. Feilet: %3$s/%2$s. Tid brukt: %4$s ms",
                sendt, brukere.size(), feilet, System.currentTimeMillis() - start);

        if (feilet > 0) {
            LOG.warn(status);
            return Response.serverError().entity(status).build();
        } else {
            LOG.info(status);
            return Response.ok().entity(status).build();
        }
    }

    static boolean kanSetteNyVeileder(String eksisterendeVeileder, String fraVeileder) {
        return eksisterendeVeileder == null || eksisterendeVeileder.equals(fraVeileder);
    }
}