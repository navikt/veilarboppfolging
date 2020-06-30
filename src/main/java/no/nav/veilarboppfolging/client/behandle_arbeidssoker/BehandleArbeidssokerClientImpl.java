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
            log.error("Klarte ikke å aktivere bruker i Arena", e);
//            Case($(instanceOf(AktiverBrukerBrukerFinnesIkke.class)), (t) -> new Feil(new AktiverBrukerService.ArenaFeilType("BRUKER_ER_UKJENT"))),
//                    Case($(instanceOf(AktiverBrukerBrukerIkkeReaktivert.class)), (t) -> new Feil(new AktiverBrukerService.ArenaFeilType("BRUKER_KAN_IKKE_REAKTIVERES"))),
//                    Case($(instanceOf(AktiverBrukerBrukerKanIkkeAktiveres.class)), (t) -> new Feil(new AktiverBrukerService.ArenaFeilType("BRUKER_ER_DOD_UTVANDRET_ELLER_FORSVUNNET"))),
//                    Case($(instanceOf(AktiverBrukerBrukerManglerArbeidstillatelse.class)), (t) -> new Feil(new AktiverBrukerService.ArenaFeilType("BRUKER_MANGLER_ARBEIDSTILLATELSE"))),
//                    Case($(instanceOf(AktiverBrukerSikkerhetsbegrensning.class)), (t) -> new NotAuthorizedException(t)),
//                    Case($(instanceOf(AktiverBrukerUgyldigInput.class)), (t) -> new BadRequestException(t)),
//                    Case($(), (t) -> new InternalServerErrorException(t))
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
            log.error("Klarte ikke å reaktivere bruker i Arena", e);
//            Case($(instanceOf(ReaktiverBrukerForenkletBrukerFinnesIkke.class)), (t) -> new Feil(new AktiverBrukerService.ArenaFeilType("BRUKER_ER_UKJENT"))),
//                    Case($(instanceOf(ReaktiverBrukerForenkletBrukerKanIkkeAktiveres.class)), (t) -> new Feil(new AktiverBrukerService.ArenaFeilType("BRUKER_ER_DOD_UTVANDRET_ELLER_FORSVUNNET"))),
//                    Case($(instanceOf(ReaktiverBrukerForenkletBrukerKanIkkeReaktiveresForenklet.class)), (t) -> new Feil(new AktiverBrukerService.ArenaFeilType("BRUKER_KAN_IKKE_REAKTIVERES_FORENKLET"))),
//                    Case($(instanceOf(ReaktiverBrukerForenkletBrukerManglerArbeidstillatelse.class)), (t) -> new Feil(new AktiverBrukerService.ArenaFeilType("BRUKER_MANGLER_ARBEIDSTILLATELSE"))),
//                    Case($(instanceOf(ReaktiverBrukerForenkletSikkerhetsbegrensning.class)), (t) -> new NotAuthorizedException(t)),
//                    Case($(instanceOf(ReaktiverBrukerForenkletUgyldigInput.class)), (t) -> new BadRequestException(t)),
//                    Case($(), (t) -> new InternalServerErrorException(t))
        }
    }

    @Override
    public HealthCheckResult checkHealth() {
        try {
            behandleArbeidssoekerPing.ping();
            return HealthCheckResult.healthy();
        } catch (Exception e) {
            return HealthCheckResult.unhealthy(e);
        }
    }

}
