package no.nav.fo.veilarboppfolging.services;

import io.swagger.annotations.Api;
import lombok.SneakyThrows;
import lombok.val;
import no.nav.dialogarena.aktor.AktorService;
import no.nav.fo.veilarboppfolging.db.KvpRepository;
import no.nav.fo.veilarboppfolging.db.OppfolgingRepository;
import no.nav.fo.veilarboppfolging.domain.*;
import no.nav.fo.veilarboppfolging.services.OppfolgingResolver.OppfolgingResolverDependencies;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Inject;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

import static java.lang.System.currentTimeMillis;

@Component
@Api
public class OppfolgingService {

    @Inject
    private OppfolgingResolverDependencies oppfolgingResolverDependencies;

    @Inject
    private AktorService aktorService;

    @Inject
    private OppfolgingRepository oppfolgingRepository;

    @Inject
    private KvpRepository kvpRepository;

    @Inject
    private EnhetPepClient enhetPepClient;

    public OppfolgingStatusData hentOppfolgingsStatus(AktorId aktorId) throws Exception {
        String fnr = aktorService.getFnr(aktorId.getAktorId())
                .orElseThrow(() -> new IllegalArgumentException("Fant ikke fnr for aktørid: " + aktorId));
        return hentOppfolgingsStatus(fnr);
    }

    @Transactional
    public OppfolgingStatusData hentOppfolgingsStatus(String fnr) throws Exception {
        val resolver = new OppfolgingResolver(fnr, oppfolgingResolverDependencies);

        resolver.sjekkStatusIArenaOgOppdaterOppfolging();

        return getOppfolgingStatusData(fnr, resolver);
    }

    public Brukervilkar hentVilkar(String fnr) throws Exception {
        return new OppfolgingResolver(fnr, oppfolgingResolverDependencies).getNyesteVilkar();
    }

    public List<Brukervilkar> hentHistoriskeVilkar(String fnr) {
        return new OppfolgingResolver(fnr, oppfolgingResolverDependencies).getHistoriskeVilkar();
    }

    @Transactional
    public OppfolgingStatusData oppdaterVilkaar(String hash, String fnr, VilkarStatus vilkarStatus) throws Exception {
        val resolver = new OppfolgingResolver(fnr, oppfolgingResolverDependencies);

        resolver.sjekkNyesteVilkarOgOppdaterOppfolging(hash, vilkarStatus);

        return getOppfolgingStatusData(fnr, resolver);
    }

    public MalData hentMal(String fnr) {
        MalData gjeldendeMal = new OppfolgingResolver(fnr, oppfolgingResolverDependencies).getOppfolging().getGjeldendeMal();
        return Optional.ofNullable(gjeldendeMal).orElse(new MalData());
    }

    public List<MalData> hentMalList(String fnr) {
        return new OppfolgingResolver(fnr, oppfolgingResolverDependencies).getMalList();
    }

    public MalData oppdaterMal(String mal, String fnr, String endretAv) {
        return new OppfolgingResolver(fnr, oppfolgingResolverDependencies).oppdaterMal(mal, endretAv);
    }

    public void slettMal(String fnr) {
        new OppfolgingResolver(fnr, oppfolgingResolverDependencies).slettMal();
    }

    public OppfolgingStatusData startOppfolging(String fnr) {
        val resolver = new OppfolgingResolver(fnr, oppfolgingResolverDependencies);
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
        val resolver = new OppfolgingResolver(fnr, oppfolgingResolverDependencies);

        resolver.avsluttOppfolging(veileder, begrunnelse);

        resolver.reloadOppfolging();
        return getOppfolgingStatusDataMedAvslutningStatus(fnr, resolver);
    }


    public List<AvsluttetOppfolgingFeedData> hentAvsluttetOppfolgingEtterDato(Timestamp timestamp, int pageSize) {
        return oppfolgingRepository.hentAvsluttetOppfolgingEtterDato(timestamp, pageSize);
    }

    public OppfolgingStatusData settDigitalBruker(String fnr) {
        val resolver = new OppfolgingResolver(fnr, oppfolgingResolverDependencies);

        return oppdaterManuellStatus(fnr, false, "Brukeren endret til digital oppfølging", KodeverkBruker.EKSTERN, resolver.getAktorId());
    }

    public OppfolgingStatusData oppdaterManuellStatus(String fnr, boolean manuell, String begrunnelse, KodeverkBruker opprettetAv, String opprettetAvBrukerId) {
        val resolver = new OppfolgingResolver(fnr, oppfolgingResolverDependencies);

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

    public void startEskalering(String fnr, String begrunnelse, long tilhorendeDialogId) {
        val resolver = new OppfolgingResolver(fnr, oppfolgingResolverDependencies);
        resolver.startEskalering(begrunnelse, tilhorendeDialogId);
    }

    public void stoppEskalering(String fnr, String begrunnelse) {
        val resolver = new OppfolgingResolver(fnr, oppfolgingResolverDependencies);
        resolver.stoppEskalering(begrunnelse);
    }

    public VeilederTilgang hentVeilederTilgang(String fnr) {
        val resolver = new OppfolgingResolver(fnr, oppfolgingResolverDependencies);
        return new VeilederTilgang().setTilgangTilBrukersKontor(enhetPepClient.harTilgang(resolver.getOppfolgingsEnhet()));
    }

    private OppfolgingStatusData getOppfolgingStatusData(String fnr, OppfolgingResolver oppfolgingResolver) {
        return getOppfolgingStatusData(fnr, oppfolgingResolver, null);
    }

    private OppfolgingStatusData getOppfolgingStatusData(String fnr, OppfolgingResolver oppfolgingResolver, AvslutningStatusData avslutningStatusData) {
        Oppfolging oppfolging = oppfolgingResolver.getOppfolging();
        return new OppfolgingStatusData()
                .setFnr(fnr)
                .setVeilederId(oppfolging.getVeilederId())
                .setUnderOppfolging(oppfolging.isUnderOppfolging())
                .setUnderKvp(oppfolging.getGjeldendeKvp() != null)
                .setReservasjonKRR(oppfolgingResolver.reservertIKrr())
                .setManuell(oppfolgingResolver.manuell())
                .setVilkarMaBesvares(oppfolgingResolver.maVilkarBesvares())
                .setKanStarteOppfolging(oppfolgingResolver.getKanSettesUnderOppfolging())
                .setAvslutningStatusData(avslutningStatusData)
                .setGjeldendeEskaleringsvarsel(oppfolging.getGjeldendeEskaleringsvarsel())
                .setOppfolgingsperioder(oppfolging.getOppfolgingsperioder())
                ;
    }

    private OppfolgingStatusData getOppfolgingStatusDataMedAvslutningStatus(String fnr, OppfolgingResolver oppfolgingResolver) {
        val avslutningStatusData = AvslutningStatusData.builder()
                .kanAvslutte(oppfolgingResolver.kanAvslutteOppfolging())
                .underOppfolging(oppfolgingResolver.erUnderOppfolgingIArena())
                .harYtelser(oppfolgingResolver.harPagaendeYtelse())
                .harTiltak(oppfolgingResolver.harAktiveTiltak())
                .inaktiveringsDato(oppfolgingResolver.getInaktiveringsDato())
                .build();

        return getOppfolgingStatusData(fnr, oppfolgingResolver, avslutningStatusData);
    }
}
