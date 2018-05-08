package no.nav.fo.veilarboppfolging.services.registrerBruker;

import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;
import no.nav.dialogarena.aktor.AktorService;
import no.nav.fo.veilarboppfolging.config.RemoteFeatureConfig.OpprettBrukerIArenaFeature;
import no.nav.fo.veilarboppfolging.config.RemoteFeatureConfig.RegistreringFeature;
import no.nav.fo.veilarboppfolging.db.ArbeidssokerregistreringRepository;
import no.nav.fo.veilarboppfolging.db.NyeBrukereFeedRepository;
import no.nav.fo.veilarboppfolging.db.OppfolgingRepository;
import no.nav.fo.veilarboppfolging.domain.*;
import no.nav.fo.veilarboppfolging.utils.FnrUtils;
import no.nav.metrics.MetricsFactory;
import no.nav.metrics.Timer;
import no.nav.tjeneste.virksomhet.behandlearbeidssoeker.v1.binding.*;
import no.nav.tjeneste.virksomhet.behandlearbeidssoeker.v1.informasjon.Brukerident;
import no.nav.tjeneste.virksomhet.behandlearbeidssoeker.v1.meldinger.AktiverBrukerRequest;
import org.springframework.transaction.annotation.Transactional;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import static io.vavr.API.$;
import static io.vavr.API.Case;
import static io.vavr.Predicates.instanceOf;
import static no.nav.fo.veilarboppfolging.utils.SelvgaaendeUtil.erSelvgaaende;

@Slf4j
public class BrukerRegistreringService {

    private AktorService aktorService;
    private ArbeidssokerregistreringRepository arbeidssokerregistreringRepository;
    private StartRegistreringStatusResolver startRegistreringStatusResolver;
    private final BehandleArbeidssoekerV1 behandleArbeidssoekerV1;
    private RegistreringFeature registreringFeature;
    private OpprettBrukerIArenaFeature opprettBrukerIArenaFeature;

    private OppfolgingRepository oppfolgingRepository;
    private NyeBrukereFeedRepository nyeBrukereFeedRepository;


    public BrukerRegistreringService(ArbeidssokerregistreringRepository arbeidssokerregistreringRepository,
                                     OppfolgingRepository oppfolgingRepository,
                                     AktorService aktorService,
                                     BehandleArbeidssoekerV1 BehandleArbeidssoekerV1,
                                     OpprettBrukerIArenaFeature opprettBrukerIArenaFeature,
                                     RegistreringFeature registreringFeature,
                                     NyeBrukereFeedRepository nyeBrukereFeedRepository,
                                     StartRegistreringStatusResolver startRegistreringStatusResolver
    ) {
        this.arbeidssokerregistreringRepository = arbeidssokerregistreringRepository;
        this.aktorService = aktorService;
        this.behandleArbeidssoekerV1 = BehandleArbeidssoekerV1;
        this.opprettBrukerIArenaFeature = opprettBrukerIArenaFeature;
        this.registreringFeature = registreringFeature;
        this.oppfolgingRepository = oppfolgingRepository;
        this.nyeBrukereFeedRepository = nyeBrukereFeedRepository;
        this.startRegistreringStatusResolver = startRegistreringStatusResolver;
    }

    public StartRegistreringStatus hentStartRegistreringStatus(String fnr) {
        return startRegistreringStatusResolver.hentStartRegistreringStatus(fnr);
    }

    public Arbeidsforhold hentSisteArbeidsforhold(String fnr) {
        return startRegistreringStatusResolver.hentSisteArbeidsforhold(fnr);
    }

    @Transactional
    public BrukerRegistrering registrerBruker(BrukerRegistrering bruker, String fnr) {

        if (!registreringFeature.erAktiv()) {
            throw new RuntimeException("Tjenesten er togglet av.");
        }

        StartRegistreringStatus status = startRegistreringStatusResolver.hentStartRegistreringStatus(fnr);

        AktorId aktorId = FnrUtils.getAktorIdOrElseThrow(aktorService, fnr);

        boolean erSelvgaaende = erSelvgaaende(bruker, status);

        if (!erSelvgaaende) {
            throw new RuntimeException("Bruker oppfyller ikke krav for registrering.");
        }

        return opprettBruker(fnr, bruker, aktorId);
    }

    BrukerRegistrering opprettBruker(String fnr, BrukerRegistrering bruker, AktorId aktorId) {
        oppfolgingRepository.startOppfolgingHvisIkkeAlleredeStartet(
                Oppfolgingsbruker.builder()
                .aktoerId(aktorId.getAktorId())
                .selvgaende(true)
                .build()
        );

        BrukerRegistrering brukerRegistrering = arbeidssokerregistreringRepository.lagreBruker(bruker, aktorId);

        if (opprettBrukerIArenaFeature.erAktiv()) {
            opprettBrukerIArena(fnr);
        }

        nyeBrukereFeedRepository.tryLeggTilFeedIdPaAlleElementerUtenFeedId();
        return brukerRegistrering;
    }

    @SuppressWarnings({"unchecked"})
    private void opprettBrukerIArena(String fnr) {
        Brukerident brukerident = new Brukerident();
        brukerident.setBrukerident(fnr);
        AktiverBrukerRequest request = new AktiverBrukerRequest();
        request.setIdent(brukerident);
        request.setKvalifiseringsgruppekode("IKVAL");

        Timer timer = MetricsFactory.createTimer("registrering.i.arena").start();
        Try.run(() -> behandleArbeidssoekerV1.aktiverBruker(request))
                .onFailure((t) -> {
                    timer.stop()
                            .setFailed()
                            .addTagToReport("aarsak",  t.getClass().getSimpleName())
                            .report();
                    log.warn("Feil ved aktivering av bruker i arena", t);
                })
                .mapFailure(
                        Case($(instanceOf(AktiverBrukerBrukerFinnesIkke.class)), (t) -> new NotFoundException(t)),
                        Case($(instanceOf(AktiverBrukerBrukerIkkeReaktivert.class)), (t) -> new ServerErrorException(Response.Status.BAD_GATEWAY, t)),
                        Case($(instanceOf(AktiverBrukerBrukerKanIkkeAktiveres.class)), (t) -> new ServerErrorException(Response.Status.BAD_GATEWAY, t)),
                        Case($(instanceOf(AktiverBrukerBrukerManglerArbeidstillatelse.class)), (t) -> new ServerErrorException(Response.Status.BAD_GATEWAY, t)),
                        Case($(instanceOf(AktiverBrukerSikkerhetsbegrensning.class)), (t) -> new NotAuthorizedException(t)),
                        Case($(instanceOf(AktiverBrukerUgyldigInput.class)), (t) -> new BadRequestException(t)),
                        Case($(), (t) -> new InternalServerErrorException(t))
                )
                .onSuccess((event) -> timer.stop().report())
                .get();
    }

}
