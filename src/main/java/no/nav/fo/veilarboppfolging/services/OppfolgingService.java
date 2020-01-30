package no.nav.fo.veilarboppfolging.services;

import lombok.SneakyThrows;
import lombok.val;
import no.nav.apiapp.feil.IngenTilgang;
import no.nav.apiapp.security.PepClient;
import no.nav.apiapp.security.PepClientComparator;
import no.nav.apiapp.security.veilarbabac.Bruker;
import no.nav.apiapp.security.veilarbabac.VeilarbAbacPepClient;
import no.nav.dialogarena.aktor.AktorService;
import no.nav.fo.veilarboppfolging.db.OppfolgingRepository;
import no.nav.fo.veilarboppfolging.db.OppfolgingsStatusRepository;
import no.nav.fo.veilarboppfolging.domain.*;
import no.nav.fo.veilarboppfolging.mappers.VeilarbArenaOppfolging;
import no.nav.fo.veilarboppfolging.rest.AutorisasjonService;
import no.nav.fo.veilarboppfolging.rest.domain.DkifResponse;
import no.nav.fo.veilarboppfolging.rest.domain.UnderOppfolgingDTO;
import no.nav.fo.veilarboppfolging.services.OppfolgingResolver.OppfolgingResolverDependencies;
import no.nav.sbl.dialogarena.common.abac.pep.AbacPersonId;
import no.nav.sbl.dialogarena.common.abac.pep.domain.ResourceType;
import no.nav.sbl.dialogarena.common.abac.pep.domain.request.Action.ActionId;
import no.nav.sbl.featuretoggle.unleash.UnleashService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Inject;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

import static java.lang.System.currentTimeMillis;

@Component
public class OppfolgingService {

    private final OppfolgingResolverDependencies oppfolgingResolverDependencies;
    private final AktorService aktorService;
    private final OppfolgingRepository oppfolgingRepository;
    private final VeilarbAbacPepClient veilarbAbacPepClient;
    private final PepClient pepClient;
    private final OppfolgingsStatusRepository oppfolgingsStatusRepository;
    private final ManuellStatusService manuellStatusService;
    private final OppfolgingsbrukerService oppfolgingsbrukerService;
    private final UnleashService unleashService;
    private final AutorisasjonService autorisasjonService;

    @Inject
    public OppfolgingService(
            OppfolgingResolverDependencies oppfolgingResolverDependencies,
            AktorService aktorService,
            OppfolgingRepository oppfolgingRepository,
            VeilarbAbacPepClient veilarbAbacPepClient,
            PepClient pepClient,
            OppfolgingsStatusRepository oppfolgingsStatusRepository,
            ManuellStatusService manuellStatusService,
            OppfolgingsbrukerService oppfolgingsbrukerService,
            UnleashService unleashService,
            AutorisasjonService autorisasjonService) {
        this.oppfolgingResolverDependencies = oppfolgingResolverDependencies;
        this.aktorService = aktorService;
        this.oppfolgingRepository = oppfolgingRepository;
        this.veilarbAbacPepClient = veilarbAbacPepClient;
        this.pepClient = pepClient;
        this.oppfolgingsStatusRepository = oppfolgingsStatusRepository;
        this.manuellStatusService = manuellStatusService;
        this.oppfolgingsbrukerService = oppfolgingsbrukerService;
        this.unleashService = unleashService;
        this.autorisasjonService = autorisasjonService;
    }

    @SneakyThrows
    public OppfolgingResolver sjekkTilgangTilEnhet(String fnr){
        PepClientComparator.get(
                () -> autorisasjonService.sjekkLesetilgangTilBruker(fnr),
                () -> pepClient.sjekkLesetilgang(AbacPersonId.fnr(fnr))
        );

        val resolver = OppfolgingResolver.lagOppfolgingResolver(fnr, oppfolgingResolverDependencies);

        boolean harTilgangTilEnhet = PepClientComparator.get(
                () -> veilarbAbacPepClient.harTilgangTilEnhet(resolver.getOppfolgingsEnhet()),
                () -> pepClient.harTilgangTilEnhet(resolver.getOppfolgingsEnhet()));

        if(!harTilgangTilEnhet) {
            throw new IngenTilgang();
        }
        return resolver;
    }

    @Transactional
    public OppfolgingStatusData hentOppfolgingsStatus(String fnr, boolean brukArenaDirekte) {
        PepClientComparator.get(
                () -> autorisasjonService.sjekkLesetilgangTilBruker(fnr),
                () -> pepClient.sjekkLesetilgang(AbacPersonId.fnr(fnr))
        );

        val resolver = OppfolgingResolver.lagOppfolgingResolver(fnr, oppfolgingResolverDependencies, brukArenaDirekte);

        resolver.sjekkStatusIArenaOgOppdaterOppfolging();

        return getOppfolgingStatusData(fnr, resolver);
    }

    @SneakyThrows
    public OppfolgingStatusData startOppfolging(String fnr) {
        val resolver = sjekkTilgangTilEnhet(fnr);
        if (resolver.getKanSettesUnderOppfolging()) {
            resolver.startOppfolging();
        }

        return getOppfolgingStatusData(fnr, resolver);
    }

    public OppfolgingStatusData hentAvslutningStatus(String fnr) {
        autorisasjonService.sjekkLesetilgangTilBruker(fnr);
        val resolver = OppfolgingResolver.lagOppfolgingResolver(fnr, oppfolgingResolverDependencies);
        return getOppfolgingStatusDataMedAvslutningStatus(fnr, resolver);
    }

    @SneakyThrows
    @Transactional
    public OppfolgingStatusData avsluttOppfolging(String fnr, String veileder, String begrunnelse) {
        val resolver = sjekkTilgangTilEnhet(fnr);

        resolver.avsluttOppfolging(veileder, begrunnelse);
        resolver.reloadOppfolging();

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
        autorisasjonService.sjekkLesetilgangTilBruker(fnr);
        if(unleashService.isEnabled("veilarboppfolging.hentVeilederTilgang.fra.veilarbarena")) {
            Optional<VeilarbArenaOppfolging> arenaBruker = oppfolgingsbrukerService.hentOppfolgingsbruker(fnr);
            String oppfolgingsenhet = arenaBruker.map(VeilarbArenaOppfolging::getNav_kontor).orElse(null);
            return new VeilederTilgang().setTilgangTilBrukersKontor(veilarbAbacPepClient.harTilgangTilEnhet(oppfolgingsenhet));
        } else {
            val resolver = OppfolgingResolver.lagOppfolgingResolver(fnr, oppfolgingResolverDependencies);
            return new VeilederTilgang().setTilgangTilBrukersKontor(veilarbAbacPepClient.harTilgangTilEnhet(resolver.getOppfolgingsEnhet()));
        }
    }

    private Optional<OppfolgingTable> getOppfolgingStatus(String fnr) {
        Bruker bruker = Bruker.fraFnr(fnr)
                .medAktoerIdSupplier(() -> aktorService.getAktorId(fnr)
                        .orElseThrow(() -> new IllegalArgumentException("Fant ikke aktørid")));
        veilarbAbacPepClient.sjekkLesetilgangTilBruker(bruker);
        return Optional.ofNullable(oppfolgingsStatusRepository.fetch(bruker.getAktoerId()));
    }

    public UnderOppfolgingDTO oppfolgingData(String fnr) {
        autorisasjonService.sjekkLesetilgangTilBruker(fnr);

        return getOppfolgingStatus(fnr)
                .map(oppfolgingsstatus -> {
                    boolean isUnderOppfolging = oppfolgingsstatus.isUnderOppfolging();
                    return new UnderOppfolgingDTO().setUnderOppfolging(isUnderOppfolging).setErManuell(isUnderOppfolging && manuellStatusService.erManuell(oppfolgingsstatus));
                })
                .orElse(new UnderOppfolgingDTO().setUnderOppfolging(false).setErManuell(false));
    }

    @Transactional
    public boolean underOppfolgingNiva3(String fnr) throws Exception {
        Bruker bruker = Bruker.fraFnr(fnr)
                .medAktoerIdSupplier(() -> aktorService.getAktorId(fnr)
                        .orElseThrow(() -> new IllegalArgumentException("Fant ikke aktørid")));

        VeilarbAbacPepClient veilarbAbacPepClientMedNiva3 = veilarbAbacPepClient
                .endre()
                .medResourceTypeUnderOppfolgingNiva3()
                .bygg();

        PepClientComparator.get(
                () -> veilarbAbacPepClientMedNiva3.sjekkLesetilgangTilBruker(bruker),
                () -> pepClient.sjekkTilgang(AbacPersonId.fnr(fnr), ActionId.READ, ResourceType.VeilArbUnderOppfolging)
        );

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
