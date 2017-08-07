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
import no.nav.fo.veilarbsituasjon.db.BrukerRepository;
import no.nav.fo.veilarbsituasjon.domain.OppfolgingBruker;
import no.nav.fo.veilarbsituasjon.rest.domain.TilordneVeilederResponse;
import no.nav.fo.veilarbsituasjon.rest.domain.VeilederTilordning;
import no.nav.fo.veilarbsituasjon.services.AktoerIdService;
import no.nav.fo.veilarbsituasjon.services.PepClient;
import no.nav.sbl.dialogarena.common.abac.pep.exception.PepException;
import org.springframework.dao.DataAccessException;
import org.springframework.jms.JmsException;
import org.springframework.jms.core.JmsTemplate;

import javax.ws.rs.*;

import java.util.stream.Collectors;

import static no.nav.fo.veilarbsituasjon.utils.JmsUtil.messageCreator;

@Component
@Path("")
@Api(value= "Portefolje")
public class PortefoljeRessurs {

    private static final Logger LOG = getLogger(PortefoljeRessurs.class);


    private JmsTemplate endreVeilederQueue;
    private AktoerIdService aktoerIdService;
    private BrukerRepository brukerRepository;
    private final PepClient pepClient;


    public PortefoljeRessurs(JmsTemplate endreVeilederQueue, AktoerIdService aktoerIdService, BrukerRepository brukerRepository, PepClient pepClient) {
        this.endreVeilederQueue = endreVeilederQueue;
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

                OppfolgingBruker bruker = new OppfolgingBruker()
                        .setVeileder(tilordning.getTilVeilederId())
                        .setAktoerid(aktoerId);

                settVeilederDersomFraVeilederErOK(bruker, tilordning, feilendeTilordninger);
            }catch(PepException e){
                LOG.error("Kall til ABAC feilet");
                feilendeTilordninger.add(tilordning);
            }
            catch(IllegalArgumentException e) {
                LOG.error("Aktoerid ikke funnet", e);
                feilendeTilordninger.add(tilordning);
            }catch(NotAuthorizedException e) {
                LOG.warn("Request is not atuhorized", e);
                feilendeTilordninger.add(tilordning);
            }catch(Exception e) {
                LOG.error("Det skjedde en feil ved tildeling av veileder",e);
                feilendeTilordninger.add(tilordning);
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

    @Transactional
    private void skrivTilDataBaseOgLeggPaaKo(OppfolgingBruker bruker, VeilederTilordning tilordning, List<VeilederTilordning> feilendeTilordninger) {
        try {
            brukerRepository.leggTilEllerOppdaterBruker(bruker);
            leggPaaKo(bruker);
            LOG.debug(String.format("Veileder %s tilordnet aktoer %s", bruker.getVeileder(), bruker.getAktoerid()));

        } catch (JmsException e) {
            feilendeTilordninger.add(tilordning);
            LOG.error(String.format("Kunne ikke legge følgende melding på kø: %s", bruker.toString()));
        } catch (DataAccessException e) {
            feilendeTilordninger.add(tilordning);
            LOG.error("Feil ved oppdatering av brukerinformasjon til aktoerid " + bruker.getAktoerid());
        } catch (Exception e) {
            feilendeTilordninger.add(tilordning);
            LOG.error(String.format("Kunne ikke tilordne veileder %s til aktoer %s", bruker.getVeileder(), bruker.getAktoerid()), e);
            throw e;
        }
    }

    private Response leggOppfolgingsbrukerPaKo(List<OppfolgingBruker> brukere) {
        long start = System.currentTimeMillis();
        int sendt = 0;
        int feilet = 0;
        for (OppfolgingBruker bruker : brukere) {
            try {
                leggPaaKo(bruker);
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

    private void leggPaaKo(OppfolgingBruker bruker) {
        endreVeilederQueue.send(messageCreator(bruker.toString()));
    }

    private void settVeilederDersomFraVeilederErOK(OppfolgingBruker bruker, VeilederTilordning tilordning, List<VeilederTilordning> feilendeTilordninger) {
        String eksisterendeVeileder;
        try {
            eksisterendeVeileder = brukerRepository.hentVeilederForAktoer(bruker.getAktoerid());
        } catch (DataAccessException e) {
            feilendeTilordninger.add(tilordning);
            LOG.error(String.format("Kunne ikke hente veileder for aktoerid %s", bruker.getAktoerid()));
            return;
        }

        Boolean fraVeilederErOk = eksisterendeVeileder == null || eksisterendeVeileder.equals(tilordning.getFraVeilederId());

        if (fraVeilederErOk) {
            skrivTilDataBaseOgLeggPaaKo(bruker, tilordning, feilendeTilordninger);
        } else {
            feilendeTilordninger.add(tilordning);
            LOG.info("Aktoerid {} kunne ikke tildeles ettersom fraVeileder er feil", bruker.getAktoerid());

        }
    }

    private String finnAktorId(final String fnr) {
        return ofNullable(aktoerIdService.findAktoerId(fnr)).
                orElseThrow(() -> new IllegalArgumentException("Aktoerid ikke funnet"));
    }

    static boolean kanSetteNyVeileder(String fraVeileder, String tilVeileder, String eksisterendeVeileder) {
        if (tilVeileder == null) {
            return false;
        }
        return eksisterendeVeileder == null || eksisterendeVeileder.equals(fraVeileder);
    }
}
