package no.nav.fo.veilarboppfolging.services.registrerBruker;

import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;
import no.nav.apiapp.security.PepClient;
import no.nav.dialogarena.aktor.AktorService;
import no.nav.fo.veilarboppfolging.config.RemoteFeatureConfig;
import no.nav.fo.veilarboppfolging.config.RemoteFeatureConfig.SjekkRegistrereBrukerArenaFeature;
import no.nav.fo.veilarboppfolging.config.RemoteFeatureConfig.SjekkRegistrereBrukerGenerellFeature;
import no.nav.fo.veilarboppfolging.db.ArbeidssokerregistreringRepository;
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

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import static io.vavr.API.$;
import static io.vavr.API.Case;
import static io.vavr.Predicates.instanceOf;
import static no.nav.fo.veilarboppfolging.utils.StartRegistreringUtils.erBesvarelseneValidertSomIkkeSelvgaaende;

@Slf4j
public class RegistrerBrukerService {

    private AktorService aktorService;
    private ArbeidssokerregistreringRepository arbeidssokerregistreringRepository;
    private StartRegistreringStatusResolver startRegistreringStatusResolver;
    private final BehandleArbeidssoekerV1 behandleArbeidssoekerV1;

    @Inject
    private SjekkRegistrereBrukerGenerellFeature skalRegistrereBrukerGenerellFeature;

    @Inject
    private SjekkRegistrereBrukerArenaFeature skalRegistrereBrukerArenaFeature;

    public RegistrerBrukerService(ArbeidssokerregistreringRepository arbeidssokerregistreringRepository,
                                  PepClient pepClient,
                                  AktorService aktorService,
                                  ArenaOppfolgingService arenaOppfolgingService,
                                  ArbeidsforholdService arbeidsforholdService,
                                  BehandleArbeidssoekerV1 BehandleArbeidssoekerV1
    ) {
        this.arbeidssokerregistreringRepository = arbeidssokerregistreringRepository;
        this.aktorService = aktorService;
        this.behandleArbeidssoekerV1 = BehandleArbeidssoekerV1;

        startRegistreringStatusResolver = new StartRegistreringStatusResolver(aktorService,
                arbeidssokerregistreringRepository, pepClient, arenaOppfolgingService, arbeidsforholdService);
    }

    public StartRegistreringStatus hentStartRegistreringStatus(String fnr) throws HentStartRegistreringStatusFeilVedHentingAvStatusFraArena,
            RegistrerBrukerSikkerhetsbegrensning, HentStartRegistreringStatusFeilVedHentingAvArbeidsforhold {
        return startRegistreringStatusResolver.hentStartRegistreringStatus(fnr);
    }

    public RegistrertBruker registrerBruker(RegistrertBruker bruker, String fnr) throws RegistrerBrukerSikkerhetsbegrensning,
            HentStartRegistreringStatusFeilVedHentingAvStatusFraArena, HentStartRegistreringStatusFeilVedHentingAvArbeidsforhold {

        if (!skalRegistrereBrukerGenerellFeature.erAktiv()) {
            throw new RuntimeException("Tjenesten er togglet av.");
        }

        StartRegistreringStatus startRegistreringStatus = startRegistreringStatusResolver.hentStartRegistreringStatus(fnr);

        AktorId aktorId = FnrUtils.getAktorIdOrElseThrow(aktorService, fnr);

        RegistrertBruker registrertBruker = null;

        if (erSelvgaaende(bruker, startRegistreringStatus)) {
            if (!skalRegistrereBrukerArenaFeature.erAktiv()) {
                opprettBrukerIArena(new AktiverArbeidssokerData(new Fnr(fnr), "IKVAL"));
            }

            registrertBruker = arbeidssokerregistreringRepository.lagreBruker(bruker, aktorId);
        }

        return registrertBruker;
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

    private boolean erSelvgaaende(RegistrertBruker bruker, StartRegistreringStatus startRegistreringStatus) {
        return !erBesvarelseneValidertSomIkkeSelvgaaende(bruker) &&
                (!startRegistreringStatus.isUnderOppfolging() &&
                        startRegistreringStatus.isOppfyllerKravForAutomatiskRegistrering());
    }

}
