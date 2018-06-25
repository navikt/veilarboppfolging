package no.nav.fo.veilarboppfolging.rest;

import no.nav.brukerdialog.security.context.SubjectHandler;
import no.nav.brukerdialog.security.domain.IdentType;
import no.nav.fo.veilarboppfolging.domain.*;
import no.nav.fo.veilarboppfolging.mappers.VilkarMapper;
import no.nav.fo.veilarboppfolging.rest.api.OppfolgingController;
import no.nav.fo.veilarboppfolging.rest.api.SystemOppfolgingController;
import no.nav.fo.veilarboppfolging.rest.api.VeilederOppfolgingController;
import no.nav.fo.veilarboppfolging.rest.domain.*;
import no.nav.fo.veilarboppfolging.services.*;
import no.nav.sbl.dialogarena.common.abac.pep.exception.PepException;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;

/*
    NB:
    tilgangskontroll med abac utføres av OppfolgingOversiktService/OppfolgingResolver slik at dette blir gjort likt
    for REST- og SOAP-apiet. Dette skiller denne rest-ressursen fra andre ressurser som må ta ansvar for tilgangskontroll selv
 */
@Component
public class OppfolgingRessurs implements OppfolgingController, VeilederOppfolgingController, SystemOppfolgingController {

    @Inject
    private OppfolgingService oppfolgingService;

    @Inject
    private KvpService kvpService;

    @Inject
    private HistorikkService historikkService;

    @Inject
    private MalService malService;

    @Inject
    private Provider<HttpServletRequest> requestProvider;

    @Inject
    private AktiverBrukerService aktiverBrukerService;

    @Inject
    private AutorisasjonService autorisasjonService;

    @Override
    public Bruker hentBrukerInfo() throws Exception {
        return new Bruker()
                .setId(getUid())
                .setErVeileder(SubjectHandler.getSubjectHandler().getIdentType() == IdentType.InternBruker);
    }

    @Override
    public OppfolgingStatus hentOppfolgingsStatus() throws Exception {
        return tilDto(oppfolgingService.hentOppfolgingsStatus(getFnr()));
    }

    @Override
    public OppfolgingStatus startOppfolging() throws Exception {
        autorisasjonService.skalVereInternBruker();
        return tilDto(oppfolgingService.startOppfolging(getFnr()));
    }

    @Override
    public OppfolgingStatus hentAvslutningStatus() throws Exception {
        autorisasjonService.skalVereInternBruker();
        return tilDto(oppfolgingService.hentAvslutningStatus(getFnr()));
    }

    @Override
    public OppfolgingStatus avsluttOppfolging(VeilederBegrunnelseDTO dto) throws Exception {
        autorisasjonService.skalVereInternBruker();
        return tilDto(oppfolgingService.avsluttOppfolging(
                getFnr(),
                dto.veilederId,
                dto.begrunnelse
        ));
    }

    @Override
    public OppfolgingStatus settTilManuell(VeilederBegrunnelseDTO dto) throws Exception {
        autorisasjonService.skalVereInternBruker();
        return tilDto(oppfolgingService.oppdaterManuellStatus(getFnr(),
                true,
                dto.begrunnelse,
                KodeverkBruker.NAV,
                hentBrukerInfo().getId())
        );
    }

    @Override
    public OppfolgingStatus settTilDigital(VeilederBegrunnelseDTO dto) throws Exception {
        autorisasjonService.skalVereInternBruker();
        return tilDto(oppfolgingService.oppdaterManuellStatus(getFnr(),
                false,
                dto.begrunnelse,
                KodeverkBruker.NAV,
                hentBrukerInfo().getId())
        );
    }

    @Override
    public List<InnstillingsHistorikk> hentInnstillingsHistorikk() throws Exception {
        autorisasjonService.skalVereInternBruker();
        return historikkService.hentInstillingsHistorikk(getFnr());
    }

    @Override
    public Vilkar hentVilkar() throws Exception {
        return tilDto(oppfolgingService.hentVilkar(getFnr()));
    }

    @Override
    public List<Vilkar> hentVilkaarStatusListe() throws PepException {
        return oppfolgingService.hentHistoriskeVilkar(getFnr())
                .stream()
                .map(this::tilDto)
                .collect(Collectors.toList());
    }

    @Override
    public OppfolgingStatus godta(String hash) throws Exception {
        return tilDto(oppfolgingService.oppdaterVilkaar(hash, getFnr(), VilkarStatus.GODKJENT));
    }

    @Override
    public OppfolgingStatus avslaa(String hash) throws Exception {
        return tilDto(oppfolgingService.oppdaterVilkaar(hash, getFnr(), VilkarStatus.AVSLATT));
    }

    @Override
    public Mal hentMal() throws PepException {
        return tilDto(malService.hentMal(getFnr()));
    }

    @Override
    public List<Mal> hentMalListe() throws PepException {
        List<MalData> malDataList = malService.hentMalList(getFnr());
        return malDataList.stream()
                .map(this::tilDto)
                .collect(toList());
    }

    @Override
    public Mal oppdaterMal(Mal mal) throws PepException {
        return tilDto(malService.oppdaterMal(mal.getMal(), getFnr(), getUid()));
    }

    @Override
    public void startEskalering(StartEskaleringDTO startEskalering) throws Exception {
        autorisasjonService.skalVereInternBruker();
        oppfolgingService.startEskalering(
                getFnr(),
                startEskalering.getBegrunnelse(),
                startEskalering.getDialogId()
        );
    }

    @Override
    public void stoppEskalering(StoppEskaleringDTO stoppEskalering) throws Exception {
        autorisasjonService.skalVereInternBruker();
        oppfolgingService.stoppEskalering(getFnr(), stoppEskalering.getBegrunnelse());
    }

    @Override
    public void startKvp(StartKvpDTO startKvp) throws Exception {
        autorisasjonService.skalVereInternBruker();
        kvpService.startKvp(getFnr(), startKvp.getBegrunnelse());
    }

    @Override
    public void stoppKvp(StoppKvpDTO stoppKvp) throws Exception {
        autorisasjonService.skalVereInternBruker();
        kvpService.stopKvp(getFnr(), stoppKvp.getBegrunnelse());
    }

    @Override
    public VeilederTilgang hentVeilederTilgang() throws Exception {
        autorisasjonService.skalVereInternBruker();
        return oppfolgingService.hentVeilederTilgang(getFnr());
    }

    @Override
    public void aktiverBruker(AktiverArbeidssokerData aktiverArbeidssokerData) throws Exception {
        autorisasjonService.skalVereInternBruker();
        aktiverBrukerService.aktiverBruker(aktiverArbeidssokerData);
    }

    private Eskaleringsvarsel tilDto(EskaleringsvarselData eskaleringsvarselData) {
        return Optional.ofNullable(eskaleringsvarselData)
                .map(eskalering -> Eskaleringsvarsel.builder()
                        .varselId(eskalering.getVarselId())
                        .aktorId(eskalering.getAktorId())
                        .opprettetAv(eskalering.getOpprettetAv())
                        .opprettetDato(eskalering.getOpprettetDato())
                        .avsluttetDato(eskalering.getAvsluttetDato())
                        .tilhorendeDialogId(eskalering.getTilhorendeDialogId())
                        .build()
                ).orElse(null);
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
                avslutningStatusData.underKvp,
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
                .setUnderKvp(oppfolgingStatusData.underKvp)
                .setVilkarMaBesvares(oppfolgingStatusData.vilkarMaBesvares)
                .setKanStarteOppfolging(oppfolgingStatusData.isKanStarteOppfolging())
                .setAvslutningStatus(
                        ofNullable(oppfolgingStatusData.getAvslutningStatusData())
                                .map(this::tilDto)
                                .orElse(null)
                )
                .setOppfolgingUtgang(oppfolgingStatusData.getOppfolgingUtgang())
                .setGjeldendeEskaleringsvarsel(tilDto(oppfolgingStatusData.getGjeldendeEskaleringsvarsel()))
                .setOppfolgingsPerioder(oppfolgingStatusData.oppfolgingsperioder.stream().map(this::tilDTO).collect(toList()))
                .setHarSkriveTilgang(oppfolgingStatusData.harSkriveTilgang);
    }

    private OppfolgingPeriodeDTO tilDTO(Oppfolgingsperiode oppfolgingsperiode) {
        return new OppfolgingPeriodeDTO()
                .setAktorId(oppfolgingsperiode.getAktorId())
                .setVeileder(oppfolgingsperiode.getVeileder())
                .setSluttDato(oppfolgingsperiode.getSluttDato())
                .setStartDato(oppfolgingsperiode.getStartDato())
                .setBegrunnelse(oppfolgingsperiode.getBegrunnelse())
                .setKvpPerioder(oppfolgingsperiode.getKvpPerioder().stream().map(this::tilDTO).collect(toList()))
                ;
    }

    private KvpPeriodeDTO tilDTO(Kvp kvp) {
        return new KvpPeriodeDTO(kvp.getOpprettetDato(), kvp.getAvsluttetDato());
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
