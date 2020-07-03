package no.nav.veilarboppfolging.services;

import lombok.SneakyThrows;
import lombok.val;
import no.nav.common.featuretoggle.UnleashService;
import no.nav.veilarboppfolging.client.veilarbarena.VeilarbArenaOppfolging;
import no.nav.veilarboppfolging.client.veilarbarena.VeilarbarenaClient;
import no.nav.veilarboppfolging.controller.domain.DkifResponse;
import no.nav.veilarboppfolging.controller.domain.UnderOppfolgingDTO;
import no.nav.veilarboppfolging.domain.*;
import no.nav.veilarboppfolging.kafka.OppfolgingStatusKafkaProducer;
import no.nav.veilarboppfolging.repository.OppfolgingRepository;
import no.nav.veilarboppfolging.repository.OppfolgingsStatusRepository;
import no.nav.veilarboppfolging.services.OppfolgingResolver.OppfolgingResolverDependencies;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

import static java.lang.System.currentTimeMillis;

@Component
public class OppfolgingService {

    private final AuthService authService;
    private final OppfolgingResolverDependencies oppfolgingResolverDependencies;
    private final OppfolgingRepository oppfolgingRepository;
    private final OppfolgingsStatusRepository oppfolgingsStatusRepository;
    private final ManuellStatusService manuellStatusService;
    private final VeilarbarenaClient veilarbarenaClient;
    private final UnleashService unleashService;
    private final OppfolgingStatusKafkaProducer kafkaProducer;

    @Autowired
    public OppfolgingService(
            AuthService authService,
            OppfolgingResolverDependencies oppfolgingResolverDependencies,
            OppfolgingRepository oppfolgingRepository,
            OppfolgingsStatusRepository oppfolgingsStatusRepository,
            ManuellStatusService manuellStatusService,
            VeilarbarenaClient veilarbarenaClient,
            UnleashService unleashService,
            OppfolgingStatusKafkaProducer kafkaProducer
    ) {
        this.authService = authService;
        this.oppfolgingResolverDependencies = oppfolgingResolverDependencies;
        this.oppfolgingRepository = oppfolgingRepository;
        this.oppfolgingsStatusRepository = oppfolgingsStatusRepository;
        this.manuellStatusService = manuellStatusService;
        this.veilarbarenaClient = veilarbarenaClient;
        this.unleashService = unleashService;
        this.kafkaProducer = kafkaProducer;
    }

    @SneakyThrows
    public OppfolgingResolver sjekkTilgangTilEnhet(String fnr){
        authService.sjekkLesetilgangMedFnr(fnr);

        val resolver = OppfolgingResolver.lagOppfolgingResolver(fnr, oppfolgingResolverDependencies);

        if(!authService.harTilgangTilEnhet(resolver.getOppfolgingsEnhet())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        return resolver;
    }

    @Transactional
    public OppfolgingStatusData hentOppfolgingsStatus(String fnr) {

        authService.sjekkLesetilgangMedFnr(fnr);

        val resolver = OppfolgingResolver.lagOppfolgingResolver(fnr, oppfolgingResolverDependencies);

        resolver.sjekkStatusIArenaOgOppdaterOppfolging();

        return getOppfolgingStatusData(fnr, resolver);
    }

    @SneakyThrows
    public OppfolgingStatusData startOppfolging(String fnr) {
        val resolver = sjekkTilgangTilEnhet(fnr);
        if (resolver.getKanSettesUnderOppfolging()) {
            resolver.startOppfolging();
        }

        kafkaProducer.send(new Fnr(fnr));
        return getOppfolgingStatusData(fnr, resolver);
    }

    public OppfolgingStatusData hentAvslutningStatus(String fnr) {
        authService.sjekkLesetilgangMedFnr(fnr);
        val resolver = OppfolgingResolver.lagOppfolgingResolver(fnr, oppfolgingResolverDependencies);
        return getOppfolgingStatusDataMedAvslutningStatus(fnr, resolver);
    }

    @SneakyThrows
    @Transactional
    public OppfolgingStatusData avsluttOppfolging(String fnr, String veileder, String begrunnelse) {
        val resolver = sjekkTilgangTilEnhet(fnr);

        resolver.avsluttOppfolging(veileder, begrunnelse);
        resolver.reloadOppfolging();

        kafkaProducer.send(new Fnr(fnr));

        return getOppfolgingStatusDataMedAvslutningStatus(fnr, resolver);
    }

    @SneakyThrows
    @Transactional
    public boolean avsluttOppfolgingForSystemBruker(String fnr, String veileder, String begrunnelse) {
        val resolver = sjekkTilgangTilEnhet(fnr);
        return resolver.avsluttOppfolging(veileder, begrunnelse);
    }

    public List<AvsluttetOppfolgingFeedData> hentAvsluttetOppfolgingEtterDato(Timestamp timestamp, int pageSize) {
        return oppfolgingRepository.hentAvsluttetOppfolgingEtterDato(timestamp, pageSize);
    }

    @SneakyThrows
    public OppfolgingStatusData settDigitalBruker(String fnr) {
        val resolver = sjekkTilgangTilEnhet(fnr);
        return oppdaterManuellStatus(fnr, false, "Brukeren endret til digital oppfølging", KodeverkBruker.EKSTERN, resolver.getAktorId());
    }

    @SneakyThrows
    public OppfolgingStatusData oppdaterManuellStatus(String fnr, boolean manuell, String begrunnelse, KodeverkBruker opprettetAv, String opprettetAvBrukerId) {
        val resolver = sjekkTilgangTilEnhet(fnr);

        if (resolver.getOppfolging().isUnderOppfolging() && (resolver.manuell() != manuell) && (!resolver.reservertIKrr().isKrr() || manuell)) {
            val nyStatus = new ManuellStatus()
                    .setAktorId(resolver.getAktorId())
                    .setManuell(manuell)
                    .setDato(new Timestamp(currentTimeMillis()))
                    .setBegrunnelse(begrunnelse)
                    .setOpprettetAv(opprettetAv)
                    .setOpprettetAvBrukerId(opprettetAvBrukerId);
            oppfolgingRepository.opprettManuellStatus(nyStatus);
            resolver.reloadOppfolging();
        }

        kafkaProducer.send(new Fnr(fnr));
        return getOppfolgingStatusData(fnr, resolver);
    }

    @SneakyThrows
    public void startEskalering(String fnr, String begrunnelse, long tilhorendeDialogId) {
        val resolver = sjekkTilgangTilEnhet(fnr);
        resolver.startEskalering(begrunnelse, tilhorendeDialogId);
    }

    @SneakyThrows
    public void stoppEskalering(String fnr, String begrunnelse) {
        val resolver = sjekkTilgangTilEnhet(fnr);
        resolver.stoppEskalering(begrunnelse);
    }

    @SneakyThrows
    public VeilederTilgang hentVeilederTilgang(String fnr) {
        authService.sjekkLesetilgangMedFnr(fnr);
        if(unleashService.isEnabled("veilarboppfolging.hentVeilederTilgang.fra.veilarbarena")) {
            Optional<VeilarbArenaOppfolging> arenaBruker = veilarbarenaClient.hentOppfolgingsbruker(fnr);
            String oppfolgingsenhet = arenaBruker.map(VeilarbArenaOppfolging::getNav_kontor).orElse(null);
            boolean tilgangTilEnhet = authService.harTilgangTilEnhet(oppfolgingsenhet);
            return new VeilederTilgang().setTilgangTilBrukersKontor(tilgangTilEnhet);
        } else {
            val resolver = OppfolgingResolver.lagOppfolgingResolver(fnr, oppfolgingResolverDependencies);
            boolean tilgangTilEnhet = authService.harTilgangTilEnhet(resolver.getOppfolgingsEnhet());
            return new VeilederTilgang().setTilgangTilBrukersKontor(tilgangTilEnhet);
        }
    }

    private Optional<OppfolgingTable> getOppfolgingStatus(String fnr) {
        String aktorId = authService.getAktorIdOrThrow(fnr);
        authService.sjekkLesetilgangMedAktorId(aktorId);
        return Optional.ofNullable(oppfolgingsStatusRepository.fetch(aktorId));
    }

    public UnderOppfolgingDTO oppfolgingData(String fnr) {
        authService.sjekkLesetilgangMedFnr(fnr);

        return getOppfolgingStatus(fnr)
                .map(oppfolgingsstatus -> {
                    boolean isUnderOppfolging = oppfolgingsstatus.isUnderOppfolging();
                    return new UnderOppfolgingDTO().setUnderOppfolging(isUnderOppfolging).setErManuell(isUnderOppfolging && manuellStatusService.erManuell(oppfolgingsstatus));
                })
                .orElse(new UnderOppfolgingDTO().setUnderOppfolging(false).setErManuell(false));
    }

    @Transactional
    public boolean underOppfolgingNiva3(String fnr) {
        String aktorId = authService.getAktorIdOrThrow(fnr);

        // TODO: Bruk denne sjekken inntil videre
        authService.sjekkLesetilgangMedAktorId(aktorId);
        // TODO: Trengs det en spesiell sjekk for dette?
//        pepClient.sjekkTilgangTilPerson(AbacPersonId.aktorId(aktorId.getAktorId()), ActionId.READ, ResourceType.VeilArbUnderOppfolging);

        val resolver = OppfolgingResolver.lagOppfolgingResolver(fnr, oppfolgingResolverDependencies);
        resolver.sjekkStatusIArenaOgOppdaterOppfolging();

        return getOppfolgingStatusData(fnr, resolver).isUnderOppfolging();
    }

    private OppfolgingStatusData getOppfolgingStatusData(String fnr, OppfolgingResolver oppfolgingResolver) {
        return getOppfolgingStatusData(fnr, oppfolgingResolver, null);
    }

    private OppfolgingStatusData getOppfolgingStatusData(String fnr, OppfolgingResolver oppfolgingResolver, AvslutningStatusData avslutningStatusData) {
        Oppfolging oppfolging = oppfolgingResolver.getOppfolging();

        DkifResponse dkifResponse = oppfolgingResolver.reservertIKrr();
        boolean krr = dkifResponse.isKrr();
        boolean manuell = oppfolgingResolver.manuell();

        return new OppfolgingStatusData()
                .setFnr(fnr)
                .setAktorId(oppfolging.getAktorId())
                .setVeilederId(oppfolging.getVeilederId())
                .setUnderOppfolging(oppfolging.isUnderOppfolging())
                .setUnderKvp(oppfolging.getGjeldendeKvp() != null)
                .setReservasjonKRR(krr)
                .setManuell(manuell || krr)
                .setKanStarteOppfolging(oppfolgingResolver.getKanSettesUnderOppfolging())
                .setAvslutningStatusData(avslutningStatusData)
                .setGjeldendeEskaleringsvarsel(oppfolging.getGjeldendeEskaleringsvarsel())
                .setOppfolgingsperioder(oppfolging.getOppfolgingsperioder())
                .setHarSkriveTilgang(oppfolgingResolver.harSkrivetilgangTilBruker())
                .setInaktivIArena(oppfolgingResolver.getInaktivIArena())
                .setKanReaktiveres(oppfolgingResolver.getKanReaktiveres())
                .setErSykmeldtMedArbeidsgiver(oppfolgingResolver.getErSykmeldtMedArbeidsgiver())
                .setErIkkeArbeidssokerUtenOppfolging(oppfolgingResolver.getErSykmeldtMedArbeidsgiver())
                .setInaktiveringsdato(oppfolgingResolver.getInaktiveringsDato())
                .setServicegruppe(oppfolgingResolver.getServicegruppe())
                .setFormidlingsgruppe(oppfolgingResolver.getFormidlingsgruppe())
                .setRettighetsgruppe(oppfolgingResolver.getRettighetsgruppe())
                .setKanVarsles(!manuell && dkifResponse.isKanVarsles());
    }

    private OppfolgingStatusData getOppfolgingStatusDataMedAvslutningStatus(String fnr, OppfolgingResolver oppfolgingResolver) {
        val avslutningStatusData = AvslutningStatusData.builder()
                .kanAvslutte(oppfolgingResolver.kanAvslutteOppfolging())
                .underOppfolging(oppfolgingResolver.erUnderOppfolgingIArena())
                .harYtelser(oppfolgingResolver.harPagaendeYtelse())
                .harTiltak(oppfolgingResolver.harAktiveTiltak())
                .underKvp(oppfolgingResolver.erUnderKvp())
                .inaktiveringsDato(oppfolgingResolver.getInaktiveringsDato())
                .build();

        return getOppfolgingStatusData(fnr, oppfolgingResolver, avslutningStatusData);
    }

}
