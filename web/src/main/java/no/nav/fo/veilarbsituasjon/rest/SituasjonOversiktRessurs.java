package no.nav.fo.veilarbsituasjon.rest;

import no.nav.brukerdialog.security.context.SubjectHandler;
import no.nav.brukerdialog.security.domain.IdentType;
import no.nav.fo.veilarbsituasjon.domain.*;
import no.nav.fo.veilarbsituasjon.mappers.VilkarMapper;
import no.nav.fo.veilarbsituasjon.rest.api.SituasjonOversikt;
import no.nav.fo.veilarbsituasjon.rest.api.VeilederSituasjonOversikt;
import no.nav.fo.veilarbsituasjon.rest.domain.*;
import no.nav.fo.veilarbsituasjon.services.PepClient;
import no.nav.fo.veilarbsituasjon.services.SituasjonOversiktService;
import no.nav.sbl.dialogarena.common.abac.pep.exception.PepException;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;

@Component
public class SituasjonOversiktRessurs implements SituasjonOversikt, VeilederSituasjonOversikt {

    @Inject
    private SituasjonOversiktService situasjonOversiktService;

    @Inject
    private Provider<HttpServletRequest> requestProvider;

    @Inject
    private PepClient pepClient;

    @Override
    public Bruker hentBrukerInfo() throws Exception {
        return new Bruker()
                .setId(getUid())
                .setErVeileder(SubjectHandler.getSubjectHandler().getIdentType() == IdentType.InternBruker);
    }

    @Override
    public OppfolgingStatus hentOppfolgingsStatus() throws Exception {
        pepClient.isServiceCallAllowed(getFnr());
        return tilDto(situasjonOversiktService.hentOppfolgingsStatus(getFnr()));
    }

    @Override
    public OppfolgingStatus startOppfolging() throws Exception{
        pepClient.isServiceCallAllowed(getFnr());
        return tilDto(situasjonOversiktService.startOppfolging(getFnr()));
    }

    @Override
    public OppfolgingStatus hentAvslutningStatus() throws Exception {
        pepClient.isServiceCallAllowed(getFnr());
        return tilDto(situasjonOversiktService.hentAvslutningStatus(getFnr()));
    }

    @Override
    public OppfolgingStatus avsluttOppfolging(EndreSituasjonDTO avsluttOppfolgingsperiode) throws Exception {
        pepClient.isServiceCallAllowed(getFnr());
        return tilDto(situasjonOversiktService.avsluttOppfolging(
                getFnr(),
                avsluttOppfolgingsperiode.veilederId,
                avsluttOppfolgingsperiode.begrunnelse
        ));
    }

    @Override
    public OppfolgingStatus settTilManuell(EndreSituasjonDTO settTilManuel) throws Exception {
        pepClient.isServiceCallAllowed(getFnr());
        return tilDto(situasjonOversiktService.oppdaterManuellStatus(getFnr(), true, settTilManuel.begrunnelse, KodeverkBruker.NAV, hentBrukerInfo().getId()));
    }

    @Override
    public OppfolgingStatus settTilDigital(EndreSituasjonDTO settTilDigital) throws Exception {
        pepClient.isServiceCallAllowed(getFnr());
        return tilDto(situasjonOversiktService.oppdaterManuellStatus(getFnr(), false, settTilDigital.begrunnelse, KodeverkBruker.NAV, hentBrukerInfo().getId()));
    }

    @Override
    public List<InnstillingsHistorikk> hentInnstillingsHistorikk() throws Exception {
        pepClient.isServiceCallAllowed(getFnr());
        return situasjonOversiktService.hentInstillingsHistorikk(getFnr());
    }

    @Override
    public void startEskalering(int tilhorendeDialogId) throws Exception {
        pepClient.isServiceCallAllowed(getFnr());
        situasjonOversiktService.startEskalering(getFnr(), tilhorendeDialogId);
    }

    @Override
    public void stoppEskalering() throws Exception {
        pepClient.isServiceCallAllowed(getFnr());
        situasjonOversiktService.stoppEskalering(getFnr());
    }

    @Override
    public Vilkar hentVilkar() throws Exception {
        pepClient.isServiceCallAllowed(getFnr());
        return tilDto(situasjonOversiktService.hentVilkar(getFnr()));
    }

    @Override
    public List<Vilkar> hentVilkaarStatusListe() throws PepException {
        pepClient.isServiceCallAllowed(getFnr());
        return situasjonOversiktService.hentHistoriskeVilkar(getFnr())
                .stream()
                .map(this::tilDto)
                .collect(Collectors.toList());
    }

    @Override
    public OppfolgingStatus godta(String hash) throws Exception {
        pepClient.isServiceCallAllowed(getFnr());
        return tilDto(situasjonOversiktService.oppdaterVilkaar(hash, getFnr(), VilkarStatus.GODKJENT));
    }

    @Override
    public OppfolgingStatus avslaa(String hash) throws Exception {
        pepClient.isServiceCallAllowed(getFnr());
        return tilDto(situasjonOversiktService.oppdaterVilkaar(hash, getFnr(), VilkarStatus.AVSLATT));
    }

    @Override
    public Mal hentMal() throws PepException {
        pepClient.isServiceCallAllowed(getFnr());
        return tilDto(situasjonOversiktService.hentMal(getFnr()));
    }

    @Override
    public List<Mal> hentMalListe() throws PepException {
        pepClient.isServiceCallAllowed(getFnr());
        List<MalData> malDataList = situasjonOversiktService.hentMalList(getFnr());
        return malDataList.stream()
                .map(this::tilDto)
                .collect(toList());
    }

    @Override
    public Mal oppdaterMal(Mal mal) throws PepException {
        pepClient.isServiceCallAllowed(getFnr());
        return tilDto(situasjonOversiktService.oppdaterMal(mal.getMal(), getFnr(), getUid()));
    }

    @Override
    public Eskaleringstatus hentEskaleringstatus() throws Exception {
        pepClient.isServiceCallAllowed(getFnr());
        return tilDto(situasjonOversiktService.hentEskaleringstatus(getFnr()));
    }

    @Override
    public List<Eskaleringstatus> hentEskaleringhistorikk() throws Exception {
        pepClient.isServiceCallAllowed(getFnr());
        List<EskaleringstatusData> eskaleringstatusDataList = situasjonOversiktService.hentEskaleringhistorikk(getFnr());
        return eskaleringstatusDataList.stream()
                .map(this::tilDto)
                .collect(toList());
    }

    private Eskaleringstatus tilDto(EskaleringstatusData eskaleringstatusData) {
        return Eskaleringstatus.builder()
                .varselId(eskaleringstatusData.getVarselId())
                .aktorId(eskaleringstatusData.getAktorId())
                .opprettetAv(eskaleringstatusData.getOpprettetAv())
                .opprettetDato(eskaleringstatusData.getOpprettetDato())
                .avsluttetDato(eskaleringstatusData.getAvsluttetDato())
                .tilhorendeDialogId(eskaleringstatusData.getTilhorendeDialogId())
                .build();
    }

    private String getUid() {
        return SubjectHandler.getSubjectHandler().getUid();
    }

    private String getFnr() {
        return requestProvider.get().getParameter("fnr");
    }

    private AvslutningStatus tilDto(AvslutningStatusData avslutningStatusData) {
        return new AvslutningStatus(
                avslutningStatusData.kanAvslutte,
                avslutningStatusData.underOppfolging,
                avslutningStatusData.harYtelser,
                avslutningStatusData.harTiltak,
                avslutningStatusData.inaktiveringsDato
        );
    }

    private OppfolgingStatus tilDto(OppfolgingStatusData oppfolgingStatusData) {
        return new OppfolgingStatus()
                .setFnr(oppfolgingStatusData.fnr)
                .setVeilederId(oppfolgingStatusData.veilederId)
                .setManuell(oppfolgingStatusData.manuell)
                .setReservasjonKRR(oppfolgingStatusData.reservasjonKRR)
                .setUnderOppfolging(oppfolgingStatusData.underOppfolging)
                .setVilkarMaBesvares(oppfolgingStatusData.vilkarMaBesvares)
                .setOppfolgingUtgang(oppfolgingStatusData.getOppfolgingUtgang())
                .setKanStarteOppfolging(oppfolgingStatusData.isKanStarteOppfolging())
                .setAvslutningStatus(
                        ofNullable(oppfolgingStatusData.getAvslutningStatusData())
                                .map(this::tilDto)
                                .orElse(null)
                )
                .setOppfolgingsPerioder(oppfolgingStatusData.oppfolgingsperioder.stream().map(this::tilDTO).collect(toList()));
    }

    private OppfolgingPeriodeDTO tilDTO(Oppfolgingsperiode oppfolgingsperiode) {
        return new OppfolgingPeriodeDTO()
                .setAktorId(oppfolgingsperiode.getAktorId())
                .setVeileder(oppfolgingsperiode.getVeileder())
                .setSluttDato(oppfolgingsperiode.getSluttDato())
                .setStartDato(oppfolgingsperiode.getStartDato())
                .setBegrunnelse(oppfolgingsperiode.getBegrunnelse())
                ;
    }

    private Vilkar tilDto(Brukervilkar brukervilkar) {
        return new Vilkar()
                .setTekst(brukervilkar.getTekst())
                .setHash(brukervilkar.getHash())
                .setDato(brukervilkar.getDato())
                .setVilkarstatus(
                        VilkarMapper.mapCommonVilkarStatusToVilkarStatusApi(
                                ofNullable(brukervilkar.getVilkarstatus()).orElse(VilkarStatus.IKKE_BESVART)
                        )
                );
    }

    private Mal tilDto(MalData malData) {
        return new Mal()
                .setMal(malData.getMal())
                .setEndretAv(malData.getEndretAvFormattert())
                .setDato(malData.getDato());
    }
}
