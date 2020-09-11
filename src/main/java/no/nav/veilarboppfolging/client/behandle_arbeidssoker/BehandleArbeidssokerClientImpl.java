package no.nav.veilarboppfolging.client.behandle_arbeidssoker;

import lombok.extern.slf4j.Slf4j;
import no.nav.common.cxf.CXFClient;
import no.nav.common.cxf.StsConfig;
import no.nav.common.health.HealthCheckResult;
import no.nav.tjeneste.virksomhet.behandlearbeidssoeker.v1.binding.*;
import no.nav.tjeneste.virksomhet.behandlearbeidssoeker.v1.informasjon.Brukerident;
import no.nav.tjeneste.virksomhet.behandlearbeidssoeker.v1.meldinger.AktiverBrukerRequest;
import no.nav.tjeneste.virksomhet.behandlearbeidssoeker.v1.meldinger.ReaktiverBrukerForenkletRequest;
import no.nav.veilarboppfolging.domain.Fnr;
import no.nav.veilarboppfolging.domain.Innsatsgruppe;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static no.nav.veilarboppfolging.client.behandle_arbeidssoker.ArenaFeilException.Type.*;


/**
 * Klient for å behandle arbeidssøkere gjennom Arena.
 */
@Slf4j
public class BehandleArbeidssokerClientImpl implements BehandleArbeidssokerClient {

    public static final int CONNECTION_TIMEOUT = 10_000;

    private static final int RECEIVE_TIMEOUT = 300_000;

    private final BehandleArbeidssoekerV1 behandleArbeidssoeker;

    private final BehandleArbeidssoekerV1 behandleArbeidssoekerPing;

    public BehandleArbeidssokerClientImpl(String behandleArbeidssoekerV1Endpoint, StsConfig stsConfig) {
        behandleArbeidssoeker = new CXFClient<>(BehandleArbeidssoekerV1.class)
                .withOutInterceptor(new LoggingOutInterceptor())
                .configureStsForSystemUser(stsConfig)
                .timeout(CONNECTION_TIMEOUT, RECEIVE_TIMEOUT)
                .address(behandleArbeidssoekerV1Endpoint)
                .build();

        behandleArbeidssoekerPing = new CXFClient<>(BehandleArbeidssoekerV1.class)
                .withOutInterceptor(new LoggingOutInterceptor())
                .configureStsForSystemUser(stsConfig)
                .address(behandleArbeidssoekerV1Endpoint)
                .build();
    }

    @Override
    public void opprettBrukerIArena(String fnr, Innsatsgruppe innsatsgruppe) {
        Brukerident brukerident = new Brukerident();
        brukerident.setBrukerident(fnr);
        AktiverBrukerRequest request = new AktiverBrukerRequest();
        request.setIdent(brukerident);
        request.setKvalifiseringsgruppekode(innsatsgruppe.getKode());

        try {
            behandleArbeidssoeker.aktiverBruker(request);
        } catch (Exception e) {
            log.warn("Klarte ikke å aktivere bruker i Arena: {}", e.getClass().getSimpleName(), e);

            ArenaFeilException.Type arenaFeilType = mapExceptionTilArenaFeilType(e);
            if (arenaFeilType != null) {
                throw new ArenaFeilException(arenaFeilType);
            }

            if (e instanceof AktiverBrukerSikkerhetsbegrensning) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
            } else if (e instanceof AktiverBrukerUgyldigInput) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
            }

            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public void reaktiverBrukerIArena(Fnr fnr) {
        Brukerident brukerident = new Brukerident();
        brukerident.setBrukerident(fnr.getFnr());
        ReaktiverBrukerForenkletRequest request = new ReaktiverBrukerForenkletRequest();
        request.setIdent(brukerident);

        try {
            behandleArbeidssoeker.reaktiverBrukerForenklet(request);
        } catch (Exception e) {
            log.warn("Klarte ikke å reaktivere bruker i Arena: {}", e.getClass().getSimpleName(), e);

            ArenaFeilException.Type arenaFeilType = mapExceptionTilArenaFeilType(e);
            if (arenaFeilType != null) {
                throw new ArenaFeilException(arenaFeilType);
            }

            if (e instanceof ReaktiverBrukerForenkletSikkerhetsbegrensning) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
            } else if (e instanceof ReaktiverBrukerForenkletUgyldigInput) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
            }

            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private ArenaFeilException.Type mapExceptionTilArenaFeilType(Exception exception) {
        if (exception instanceof AktiverBrukerBrukerFinnesIkke ||
                exception instanceof ReaktiverBrukerForenkletBrukerFinnesIkke) {
            return BRUKER_ER_UKJENT;
        }
        else if (exception instanceof AktiverBrukerBrukerIkkeReaktivert) {
            return BRUKER_KAN_IKKE_REAKTIVERES;
        }
        else if (exception instanceof AktiverBrukerBrukerKanIkkeAktiveres ||
                exception instanceof ReaktiverBrukerForenkletBrukerKanIkkeAktiveres) {
            return BRUKER_ER_DOD_UTVANDRET_ELLER_FORSVUNNET;
        }
        else if (exception instanceof AktiverBrukerBrukerManglerArbeidstillatelse ||
                exception instanceof ReaktiverBrukerForenkletBrukerManglerArbeidstillatelse) {
            return BRUKER_MANGLER_ARBEIDSTILLATELSE;
        }
        else if (exception instanceof ReaktiverBrukerForenkletBrukerKanIkkeReaktiveresForenklet) {
            return BRUKER_KAN_IKKE_REAKTIVERES_FORENKLET;
        } else {
            return null;
        }
    }

    @Override
    public HealthCheckResult checkHealth() {
        try {
            behandleArbeidssoekerPing.ping();
            return HealthCheckResult.healthy();
        } catch (Exception e) {
            log.warn("Feil ved ping av BehandleArbeidssoekerV1", e);
            return HealthCheckResult.unhealthy(e);
        }
    }

}
