package no.nav.fo.veilarbsituasjon.services;

import io.swagger.annotations.Api;
import lombok.SneakyThrows;
import lombok.val;
import no.nav.brukerdialog.security.context.SubjectHandler;
import no.nav.fo.veilarbsituasjon.db.SituasjonRepository;
import no.nav.fo.veilarbsituasjon.domain.*;
import no.nav.fo.veilarbsituasjon.services.SituasjonResolver.SituasjonResolverDependencies;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Inject;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.lang.System.currentTimeMillis;
import static java.util.stream.Stream.concat;
import static no.nav.fo.veilarbsituasjon.domain.InnstillingsHistorikk.Type.*;
import static no.nav.fo.veilarbsituasjon.domain.KodeverkBruker.NAV;

@Component
@Api
public class SituasjonOversiktService {

    @Inject
    private SituasjonResolverDependencies situasjonResolverDependencies;

    @Inject
    private SituasjonRepository situasjonRepository;

    @Transactional
    public OppfolgingStatusData hentOppfolgingsStatus(String fnr) throws Exception {
        val situasjonResolver = new SituasjonResolver(fnr, situasjonResolverDependencies);

        situasjonResolver.sjekkStatusIArenaOgOppdaterSituasjon();

        return getOppfolgingStatusData(fnr, situasjonResolver);
    }

    public Brukervilkar hentVilkar(String fnr) throws Exception {
        return new SituasjonResolver(fnr, situasjonResolverDependencies).getNyesteVilkar();
    }

    public List<Brukervilkar> hentHistoriskeVilkar(String fnr) {
        return new SituasjonResolver(fnr, situasjonResolverDependencies).getHistoriskeVilkar();
    }

    @Transactional
    public OppfolgingStatusData oppdaterVilkaar(String hash, String fnr, VilkarStatus vilkarStatus) throws Exception {
        val situasjonResolver = new SituasjonResolver(fnr, situasjonResolverDependencies);

        situasjonResolver.sjekkNyesteVilkarOgOppdaterSituasjon(hash, vilkarStatus);

        return getOppfolgingStatusData(fnr, situasjonResolver);
    }

    public MalData hentMal(String fnr) {
        MalData gjeldendeMal = new SituasjonResolver(fnr, situasjonResolverDependencies).getSituasjon().getGjeldendeMal();
        return Optional.ofNullable(gjeldendeMal).orElse(new MalData());
    }

    public List<MalData> hentMalList(String fnr) {
        return new SituasjonResolver(fnr, situasjonResolverDependencies).getMalList();
    }

    public MalData oppdaterMal(String mal, String fnr, String endretAv) {
        return new SituasjonResolver(fnr, situasjonResolverDependencies).oppdaterMal(mal, endretAv);
    }

    public OppfolgingStatusData startOppfolging(String fnr) {
        val situasjonResolver = new SituasjonResolver(fnr, situasjonResolverDependencies);
        if (situasjonResolver.getKanSettesUnderOppfolging()) {
            situasjonResolver.startOppfolging();
        }

        return getOppfolgingStatusData(fnr, situasjonResolver);
    }

    public OppfolgingStatusData hentAvslutningStatus(String fnr) throws Exception {
        val situasjonResolver = new SituasjonResolver(fnr, situasjonResolverDependencies);

        return getOppfolgingStatusDataMedAvslutningStatus(fnr, situasjonResolver);
    }

    @SneakyThrows
    @Transactional
    public OppfolgingStatusData avsluttOppfolging(String fnr, String veileder, String begrunnelse) {
        val situasjonResolver = new SituasjonResolver(fnr, situasjonResolverDependencies);

        situasjonResolver.avsluttOppfolging(veileder, begrunnelse);

        situasjonResolver.reloadSituasjon();
        return getOppfolgingStatusDataMedAvslutningStatus(fnr, situasjonResolver);
    }


    public List<AvsluttetOppfolgingFeedData> hentAvsluttetOppfolgingEtterDato(Timestamp timestamp) {
        return situasjonRepository.hentAvsluttetOppfolgingEtterDato(timestamp);
    }

    public OppfolgingStatusData settDigitalBruker(String fnr) {
        val resolver = new SituasjonResolver(fnr, situasjonResolverDependencies);

        return oppdaterManuellStatus(fnr, false, "Bruker satte seg selv til digital oppfølging", KodeverkBruker.EKSTERN, resolver.getAktorId());
    }

    public OppfolgingStatusData oppdaterManuellStatus(String fnr, boolean manuell, String begrunnelse, KodeverkBruker opprettetAv, String opprettetAvBrukerId) {
        val resolver = new SituasjonResolver(fnr, situasjonResolverDependencies);

        if (resolver.getSituasjon().isOppfolging() && (resolver.manuell() != manuell) && (!resolver.reservertIKrr() || manuell)) {
            val nyStatus = new Status(resolver.getAktorId(), manuell, new Timestamp(currentTimeMillis()), begrunnelse, opprettetAv, opprettetAvBrukerId);
            situasjonRepository.opprettStatus(nyStatus);
            resolver.reloadSituasjon();
        }

        return getOppfolgingStatusData(fnr, resolver);
    }


    public List<InnstillingsHistorikk> hentInstillingsHistorikk(String fnr) {
        val resolver = new SituasjonResolver(fnr, situasjonResolverDependencies);
        String aktorId = resolver.getAktorId();

        return concat(
                concat(
                        situasjonRepository.hentAvsluttetOppfolgingsperioder(aktorId).stream().map(this::tilDTO),
                        situasjonRepository.hentManuellHistorikk(aktorId).stream().map(this::tilDTO)),
                situasjonRepository.hentEskaleringhistorikk(aktorId).stream().map(this::tilDTO).flatMap(List::stream)
        ).collect(Collectors.toList());
    }

    public List<EskaleringsvarselData> hentEskaleringhistorikk(String fnr) {
        val resolver = new SituasjonResolver(fnr, situasjonResolverDependencies);
        String aktorId = resolver.getAktorId();

        return situasjonRepository.hentEskaleringhistorikk(aktorId);
    }

    // TODO: Si ifra til VarselOppgave om at nytt eskaleringsvarsel er opprettet.
    public void startEskalering(String fnr, String begrunnelse, long tilhorendeDialogId) {
        val resolver = new SituasjonResolver(fnr, situasjonResolverDependencies);
        String aktorId = resolver.getAktorId();
        String veilederId = SubjectHandler.getSubjectHandler().getUid();

        situasjonRepository.startEskalering(aktorId, veilederId, begrunnelse, tilhorendeDialogId);
    }

    public void stoppEskalering(String fnr, String begrunnelse) {
        val resolver = new SituasjonResolver(fnr, situasjonResolverDependencies);
        String aktorId = resolver.getAktorId();
        String veilederId = SubjectHandler.getSubjectHandler().getUid();

        situasjonRepository.stoppEskalering(aktorId, veilederId, begrunnelse);
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

    private InnstillingsHistorikk tilDTO(InnstillingsHistorikkData historikkData) {
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
                .opprettetAv(KodeverkBruker.NAV)
                .opprettetAvBrukerId(data.getOpprettetAv())
                .dialogId(data.getTilhorendeDialogId())
                .build();

        if (harAvsluttetEskalering) {
            val stoppetEskalering = InnstillingsHistorikk
                    .builder()
                    .type(ESKALERING_STOPPET)
                    .dato(data.getAvsluttetDato())
                    .begrunnelse(data.getAvsluttetBegrunnelse())
                    .opprettetAv(KodeverkBruker.NAV)
                    .opprettetAvBrukerId(data.getAvsluttetAv())
                    .dialogId(data.getTilhorendeDialogId())
                    .build();
            return Arrays.asList(startetEskalering, stoppetEskalering);
        } else {
            return Collections.singletonList(startetEskalering);
        }

    }

    private OppfolgingStatusData getOppfolgingStatusData(String fnr, SituasjonResolver situasjonResolver) {
        return getOppfolgingStatusData(fnr, situasjonResolver, null);
    }

    private OppfolgingStatusData getOppfolgingStatusData(String fnr, SituasjonResolver situasjonResolver, AvslutningStatusData avslutningStatusData) {
        Situasjon situasjon = situasjonResolver.getSituasjon();
        return new OppfolgingStatusData()
                .setFnr(fnr)
                .setVeilederId(situasjon.getVeilederId())
                .setUnderOppfolging(situasjon.isOppfolging())
                .setReservasjonKRR(situasjonResolver.reservertIKrr())
                .setManuell(situasjonResolver.manuell())
                .setVilkarMaBesvares(situasjonResolver.maVilkarBesvares())
                .setKanStarteOppfolging(situasjonResolver.getKanSettesUnderOppfolging())
                .setAvslutningStatusData(avslutningStatusData)
                .setGjeldendeEskaleringsvarsel(situasjon.getGjeldendeEskaleringsvarsel())
                .setOppfolgingsperioder(situasjon.getOppfolgingsperioder())
                ;
    }

    private OppfolgingStatusData getOppfolgingStatusDataMedAvslutningStatus(String fnr, SituasjonResolver situasjonResolver) {
        val avslutningStatusData = AvslutningStatusData.builder()
                .kanAvslutte(situasjonResolver.kanAvslutteOppfolging())
                .underOppfolging(situasjonResolver.erUnderOppfolgingIArena())
                .harYtelser(situasjonResolver.harPagaendeYtelse())
                .harTiltak(situasjonResolver.harAktiveTiltak())
                .inaktiveringsDato(situasjonResolver.getInaktiveringsDato())
                .build();

        return getOppfolgingStatusData(fnr, situasjonResolver, avslutningStatusData);
    }
}
