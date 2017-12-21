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
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.System.currentTimeMillis;
import static java.util.Collections.singletonList;
import static no.nav.fo.veilarboppfolging.domain.InnstillingsHistorikk.Type.*;
import static no.nav.fo.veilarboppfolging.domain.KodeverkBruker.NAV;

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

    public OppfolgingStatusData hentOppfolgingsStatus(AktorId aktorId) throws Exception {
        String fnr = aktorService.getFnr(aktorId.getAktorId())
                .orElseThrow(() -> new IllegalArgumentException("Fant ikke aktør for fnr: " + aktorId));
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


    public List<InnstillingsHistorikk> hentInstillingsHistorikk(String fnr) {
        val resolver = new OppfolgingResolver(fnr, oppfolgingResolverDependencies);
        String aktorId = resolver.getAktorId();

        return Stream.of(
                kvpRepository.hentKvpHistorikk(aktorId).stream().map(this::tilDTO).flatMap(List::stream), // TODO: Dette er et punkt hvor vi må filtrere ut data for brukere som ikke har tilgang til å se KVP data.
                oppfolgingRepository.hentAvsluttetOppfolgingsperioder(aktorId).stream().map(this::tilDTO),
                oppfolgingRepository.hentManuellHistorikk(aktorId).stream().map(this::tilDTO),
                oppfolgingRepository.hentEskaleringhistorikk(aktorId).stream().map(this::tilDTO).flatMap(List::stream)
        ).flatMap(s -> s).collect(Collectors.toList());
    }

    public List<EskaleringsvarselData> hentEskaleringhistorikk(String fnr) {
        val resolver = new OppfolgingResolver(fnr, oppfolgingResolverDependencies);
        String aktorId = resolver.getAktorId();

        return oppfolgingRepository.hentEskaleringhistorikk(aktorId);
    }

    public void startEskalering(String fnr, String begrunnelse, long tilhorendeDialogId) {
        val resolver = new OppfolgingResolver(fnr, oppfolgingResolverDependencies);
        resolver.startEskalering(begrunnelse, tilhorendeDialogId);
    }

    public void stoppEskalering(String fnr, String begrunnelse) {
        val resolver = new OppfolgingResolver(fnr, oppfolgingResolverDependencies);
        resolver.stoppEskalering(begrunnelse);
    }

    private List<InnstillingsHistorikk> tilDTO(Kvp kvp) {
        InnstillingsHistorikk kvpStart = InnstillingsHistorikk.builder()
                .type(KVP_STARTET)
                .begrunnelse(kvp.getOpprettetBegrunnelse())
                .dato(kvp.getOpprettetDato())
                .opprettetAv(NAV)
                .opprettetAvBrukerId(kvp.getOpprettetAv())
                .build();

        if (kvp.getAvsluttetDato() != null) {
            InnstillingsHistorikk kvpStopp = InnstillingsHistorikk.builder()
                    .type(KVP_STOPPET)
                    .begrunnelse(kvp.getAvsluttetBegrunnelse())
                    .dato(kvp.getAvsluttetDato())
                    .opprettetAv(NAV)
                    .opprettetAvBrukerId(kvp.getAvsluttetAv())
                    .build();
            return Arrays.asList(kvpStart, kvpStopp);
        }
        return singletonList(kvpStart);
    }

    private InnstillingsHistorikk tilDTO(Oppfolgingsperiode oppfolgingsperiode) {
        return InnstillingsHistorikk.builder()
                .type(AVSLUTTET_OPPFOLGINGSPERIODE)
                .begrunnelse(oppfolgingsperiode.getBegrunnelse())
                .dato(oppfolgingsperiode.getSluttDato())
                .opprettetAv(NAV)
                .opprettetAvBrukerId(oppfolgingsperiode.getVeileder())
                .build();
    }

    private InnstillingsHistorikk tilDTO(ManuellStatus historikkData) {
        return InnstillingsHistorikk.builder()
                .type(historikkData.isManuell() ? SATT_TIL_MANUELL : SATT_TIL_DIGITAL)
                .begrunnelse(historikkData.getBegrunnelse())
                .dato(historikkData.getDato())
                .opprettetAv(historikkData.getOpprettetAv())
                .opprettetAvBrukerId(historikkData.getOpprettetAvBrukerId())
                .build();
    }

    private List<InnstillingsHistorikk> tilDTO(EskaleringsvarselData data) {
        val harAvsluttetEskalering = data.getAvsluttetDato() != null;

        val startetEskalering = InnstillingsHistorikk
                .builder()
                .type(ESKALERING_STARTET)
                .dato(data.getOpprettetDato())
                .begrunnelse(data.getOpprettetBegrunnelse())
                .opprettetAv(NAV)
                .opprettetAvBrukerId(data.getOpprettetAv())
                .dialogId(data.getTilhorendeDialogId())
                .build();

        if (harAvsluttetEskalering) {
            val stoppetEskalering = InnstillingsHistorikk
                    .builder()
                    .type(ESKALERING_STOPPET)
                    .dato(data.getAvsluttetDato())
                    .begrunnelse(data.getAvsluttetBegrunnelse())
                    .opprettetAv(NAV)
                    .opprettetAvBrukerId(data.getAvsluttetAv())
                    .dialogId(data.getTilhorendeDialogId())
                    .build();
            return Arrays.asList(startetEskalering, stoppetEskalering);
        } else {
            return singletonList(startetEskalering);
        }

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
