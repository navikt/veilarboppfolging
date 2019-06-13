package no.nav.fo.veilarboppfolging.services;

import lombok.SneakyThrows;
import lombok.val;
import no.nav.apiapp.feil.IngenTilgang;
import no.nav.apiapp.security.veilarbabac.Bruker;
import no.nav.apiapp.security.veilarbabac.VeilarbAbacPepClient;
import no.nav.dialogarena.aktor.AktorService;
import no.nav.fo.veilarboppfolging.db.OppfolgingRepository;
import no.nav.fo.veilarboppfolging.db.OppfolgingsStatusRepository;
import no.nav.fo.veilarboppfolging.domain.*;
import no.nav.fo.veilarboppfolging.mappers.ArenaBruker;
import no.nav.fo.veilarboppfolging.rest.domain.UnderOppfolgingDTO;
import no.nav.fo.veilarboppfolging.services.OppfolgingResolver.OppfolgingResolverDependencies;
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
    private final VeilarbAbacPepClient pepClient;
    private final OppfolgingsStatusRepository oppfolgingsStatusRepository;
    private final ManuellStatusService manuellStatusService;
    private final OppfolgingsbrukerService oppfolgingsbrukerService;

    @Inject
    public OppfolgingService(
            OppfolgingResolverDependencies oppfolgingResolverDependencies,
            AktorService aktorService,
            OppfolgingRepository oppfolgingRepository,
            VeilarbAbacPepClient pepClient,
            OppfolgingsStatusRepository oppfolgingsStatusRepository,
            ManuellStatusService manuellStatusService,
            OppfolgingsbrukerService oppfolgingsbrukerService
    ) {
        this.oppfolgingResolverDependencies = oppfolgingResolverDependencies;
        this.aktorService = aktorService;
        this.oppfolgingRepository = oppfolgingRepository;
        this.pepClient = pepClient;
        this.oppfolgingsStatusRepository = oppfolgingsStatusRepository;
        this.manuellStatusService = manuellStatusService;
        this.oppfolgingsbrukerService = oppfolgingsbrukerService;
    }

    @SneakyThrows
    public OppfolgingResolver sjekkTilgangTilEnhet(String fnr){
        val resolver = new OppfolgingResolver(fnr, oppfolgingResolverDependencies);
        if(!pepClient.harTilgangTilEnhet(resolver.getOppfolgingsEnhet())) {
            throw new IngenTilgang();
        }
        return resolver;
    }

    @Transactional
    public OppfolgingStatusData hentOppfolgingsStatus(String fnr) throws Exception {
        val resolver = new OppfolgingResolver(fnr, oppfolgingResolverDependencies);

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

    public OppfolgingStatusData hentAvslutningStatus(String fnr) throws Exception {
        val resolver = new OppfolgingResolver(fnr, oppfolgingResolverDependencies);
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

        if (resolver.getOppfolging().isUnderOppfolging() && (resolver.manuell() != manuell) && (!resolver.reservertIKrr() || manuell)) {
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
        Optional<ArenaBruker> arenaBruker = oppfolgingsbrukerService.hentOppfolgingsbruker(fnr);
        String oppfolgingsenhet = arenaBruker.map(ArenaBruker::getNav_kontor).orElse(null);
        return new VeilederTilgang().setTilgangTilBrukersKontor(pepClient.harTilgangTilEnhet(oppfolgingsenhet));
    }

    private Optional<OppfolgingTable> getOppfolgingStatus(String fnr) {
        Bruker bruker = Bruker.fraFnr(fnr)
                .medAktoerIdSupplier(() -> aktorService.getAktorId(fnr)
                        .orElseThrow(() -> new IllegalArgumentException("Fant ikke aktørid")));
        pepClient.sjekkLesetilgangTilBruker(bruker);
        return Optional.ofNullable(oppfolgingsStatusRepository.fetch(bruker.getAktoerId()));
    }

    public boolean underOppfolging(String fnr) {
        return getOppfolgingStatus(fnr)
                .map(OppfolgingTable::isUnderOppfolging)
                .orElse(false);
    }


    public UnderOppfolgingDTO oppfolgingData(String fnr) {
        return getOppfolgingStatus(fnr)
                .map(oppfolgingsstatus -> {
                    boolean isUnderOppfolging = oppfolgingsstatus.isUnderOppfolging();
                    return new UnderOppfolgingDTO().setUnderOppfolging(isUnderOppfolging).setErManuell(isUnderOppfolging && manuellStatusService.erManuell(oppfolgingsstatus));
                })
                .orElse(new UnderOppfolgingDTO().setUnderOppfolging(false).setErManuell(false));
    }

    private OppfolgingStatusData getOppfolgingStatusData(String fnr, OppfolgingResolver oppfolgingResolver) {
        return getOppfolgingStatusData(fnr, oppfolgingResolver, null);
    }

    private OppfolgingStatusData getOppfolgingStatusData(String fnr, OppfolgingResolver oppfolgingResolver, AvslutningStatusData avslutningStatusData) {
        Oppfolging oppfolging = oppfolgingResolver.getOppfolging();

        boolean krr = oppfolgingResolver.reservertIKrr();
        return new OppfolgingStatusData()
                .setFnr(fnr)
                .setVeilederId(oppfolging.getVeilederId())
                .setUnderOppfolging(oppfolging.isUnderOppfolging())
                .setUnderKvp(oppfolging.getGjeldendeKvp() != null)
                .setReservasjonKRR(krr)
                .setManuell(oppfolgingResolver.manuell() || krr)
                .setKanStarteOppfolging(oppfolgingResolver.getKanSettesUnderOppfolging())
                .setAvslutningStatusData(avslutningStatusData)
                .setGjeldendeEskaleringsvarsel(oppfolging.getGjeldendeEskaleringsvarsel())
                .setOppfolgingsperioder(oppfolging.getOppfolgingsperioder())
                .setHarSkriveTilgang(oppfolgingResolver.harSkrivetilgangTilBruker())
                .setInaktivIArena(oppfolgingResolver.getInaktivIArena())
                .setKanReaktiveres(oppfolgingResolver.getKanReaktiveres())
                .setErSykmeldtMedArbeidsgiver(oppfolgingResolver.getErSykmeldtMedArbeidsgiver())
                .setErIkkeArbeidssokerUtenOppfolging(oppfolgingResolver.getErSykmeldtMedArbeidsgiver())
                .setInaktiveringsdato(oppfolgingResolver.getInaktiveringsDato());
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
