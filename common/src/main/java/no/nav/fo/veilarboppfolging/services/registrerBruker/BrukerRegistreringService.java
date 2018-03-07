package no.nav.fo.veilarboppfolging.services.registrerBruker;

import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;
import no.nav.apiapp.security.PepClient;
import no.nav.dialogarena.aktor.AktorService;
import no.nav.fo.veilarboppfolging.config.RemoteFeatureConfig.OpprettBrukerIArenaFeature;
import no.nav.fo.veilarboppfolging.config.RemoteFeatureConfig.RegistreringFeature;
import no.nav.fo.veilarboppfolging.db.ArbeidssokerregistreringRepository;
import no.nav.fo.veilarboppfolging.db.OppfolgingRepository;
import no.nav.fo.veilarboppfolging.domain.*;
import no.nav.fo.veilarboppfolging.services.ArbeidsforholdService;
import no.nav.fo.veilarboppfolging.services.ArenaOppfolgingService;
import no.nav.fo.veilarboppfolging.utils.FnrUtils;
import no.nav.tjeneste.virksomhet.behandlearbeidssoeker.v1.binding.*;
import no.nav.tjeneste.virksomhet.behandlearbeidssoeker.v1.informasjon.Brukerident;
import no.nav.tjeneste.virksomhet.behandlearbeidssoeker.v1.meldinger.AktiverBrukerRequest;
import no.nav.tjeneste.virksomhet.behandleoppfolging.v1.binding.HentStartRegistreringStatusFeilVedHentingAvArbeidsforhold;
import no.nav.tjeneste.virksomhet.behandleoppfolging.v1.binding.HentStartRegistreringStatusFeilVedHentingAvStatusFraArena;
import no.nav.tjeneste.virksomhet.behandleoppfolging.v1.binding.RegistrerBrukerSikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.behandleoppfolging.v1.feil.Sikkerhetsbegrensning;
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

    public BrukerRegistreringService(ArbeidssokerregistreringRepository arbeidssokerregistreringRepository,
                                     OppfolgingRepository oppfolgingRepository,
                                     PepClient pepClient,
                                     AktorService aktorService,
                                     ArenaOppfolgingService arenaOppfolgingService,
                                     ArbeidsforholdService arbeidsforholdService,
                                     BehandleArbeidssoekerV1 BehandleArbeidssoekerV1,
                                     OpprettBrukerIArenaFeature opprettBrukerIArenaFeature,
                                     RegistreringFeature registreringFeature
    ) {
        this.arbeidssokerregistreringRepository = arbeidssokerregistreringRepository;
        this.aktorService = aktorService;
        this.behandleArbeidssoekerV1 = BehandleArbeidssoekerV1;
        this.opprettBrukerIArenaFeature = opprettBrukerIArenaFeature;
        this.registreringFeature = registreringFeature;
        this.oppfolgingRepository = oppfolgingRepository;

        startRegistreringStatusResolver = new StartRegistreringStatusResolver(aktorService,
                arbeidssokerregistreringRepository, pepClient, arenaOppfolgingService, arbeidsforholdService);
    }

    public StartRegistreringStatus hentStartRegistreringStatus(String fnr) throws HentStartRegistreringStatusFeilVedHentingAvStatusFraArena,
            RegistrerBrukerSikkerhetsbegrensning, HentStartRegistreringStatusFeilVedHentingAvArbeidsforhold {
        return startRegistreringStatusResolver.hentStartRegistreringStatus(fnr);
    }

    public Arbeidsforhold hentArbeidsforholdet(String fnr) throws RegistrerBrukerSikkerhetsbegrensning,
            HentStartRegistreringStatusFeilVedHentingAvArbeidsforhold {
        return startRegistreringStatusResolver.hentArbeidsforholdet(fnr);
    }

    public BrukerRegistrering registrerBruker(BrukerRegistrering bruker, String fnr) throws
            RegistrerBrukerSikkerhetsbegrensning,
            HentStartRegistreringStatusFeilVedHentingAvStatusFraArena,
            HentStartRegistreringStatusFeilVedHentingAvArbeidsforhold {

        if (!registreringFeature.erAktiv()) {
            throw new RuntimeException("Tjenesten er togglet av.");
        }

        StartRegistreringStatus status = startRegistreringStatusResolver.hentStartRegistreringStatus(fnr);

        AktorId aktorId = FnrUtils.getAktorIdOrElseThrow(aktorService, fnr);

        boolean erSelvaaende = erSelvgaaende(bruker, status);

        if (!erSelvaaende) {
            Sikkerhetsbegrensning sikkerhetsbegrensning = startRegistreringStatusResolver.getSikkerhetsbegrensning(
                    "Arena",
                    "Arena",
                    "Bruker oppfyller ikke krav for registrering.");
            throw new RegistrerBrukerSikkerhetsbegrensning("Bruker oppfyller ikke krav for registrering.", sikkerhetsbegrensning);
        }

        return opprettBruker(fnr, bruker, aktorId);
    }

    @Transactional
    BrukerRegistrering opprettBruker(String fnr, BrukerRegistrering bruker, AktorId aktorId) {
        oppfolgingRepository.opprettOppfolging(aktorId.getAktorId());
        oppfolgingRepository.startOppfolgingHvisIkkeAlleredeStartet(aktorId.getAktorId());

        BrukerRegistrering brukerRegistrering = arbeidssokerregistreringRepository.lagreBruker(bruker, aktorId);

        if (opprettBrukerIArenaFeature.erAktiv()) {
            opprettBrukerIArena(new AktiverArbeidssokerData(new Fnr(fnr), "IKVAL"));
        }

        return brukerRegistrering;
    }

    @SuppressWarnings({"unchecked"})
    private void opprettBrukerIArena(AktiverArbeidssokerData aktiverArbeidssokerData) {
        Brukerident brukerident = new Brukerident();
        brukerident.setBrukerident(aktiverArbeidssokerData.getFnr().getFnr());
        AktiverBrukerRequest request = new AktiverBrukerRequest();
        request.setIdent(brukerident);
        request.setKvalifiseringsgruppekode(aktiverArbeidssokerData.getKvalifiseringsgruppekode());


        Try.run(() -> behandleArbeidssoekerV1.aktiverBruker(request))
                .onFailure((t) -> log.warn("Feil ved aktivering av bruker i arena", t))
                .mapFailure(
                        Case($(instanceOf(AktiverBrukerBrukerFinnesIkke.class)), (t) -> new NotFoundException(t)),
                        Case($(instanceOf(AktiverBrukerBrukerIkkeReaktivert.class)), (t) -> new ServerErrorException(Response.Status.BAD_GATEWAY, t)),
                        Case($(instanceOf(AktiverBrukerBrukerKanIkkeAktiveres.class)), (t) -> new ServerErrorException(Response.Status.BAD_GATEWAY, t)),
                        Case($(instanceOf(AktiverBrukerBrukerManglerArbeidstillatelse.class)), (t) -> new ServerErrorException(Response.Status.BAD_GATEWAY, t)),
                        Case($(instanceOf(AktiverBrukerSikkerhetsbegrensning.class)), (t) -> new NotAuthorizedException(t)),
                        Case($(instanceOf(AktiverBrukerUgyldigInput.class)), (t) -> new BadRequestException(t)),
                        Case($(), (t) -> new InternalServerErrorException(t))
                )
                .get();
    }

}
