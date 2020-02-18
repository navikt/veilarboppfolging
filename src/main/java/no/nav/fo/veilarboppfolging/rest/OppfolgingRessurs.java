package no.nav.fo.veilarboppfolging.rest;

import no.nav.apiapp.security.PepClient;
import no.nav.common.auth.SubjectHandler;
import no.nav.dialogarena.aktor.AktorService;
import no.nav.fo.veilarboppfolging.domain.*;
import no.nav.fo.veilarboppfolging.rest.api.OppfolgingController;
import no.nav.fo.veilarboppfolging.rest.api.SystemOppfolgingController;
import no.nav.fo.veilarboppfolging.rest.api.VeilederOppfolgingController;
import no.nav.fo.veilarboppfolging.rest.domain.AvslutningStatus;
import no.nav.fo.veilarboppfolging.rest.domain.Bruker;
import no.nav.fo.veilarboppfolging.rest.domain.Eskaleringsvarsel;
import no.nav.fo.veilarboppfolging.rest.domain.KvpPeriodeDTO;
import no.nav.fo.veilarboppfolging.rest.domain.Mal;
import no.nav.fo.veilarboppfolging.rest.domain.OppfolgingPeriodeDTO;
import no.nav.fo.veilarboppfolging.rest.domain.OppfolgingStatus;
import no.nav.fo.veilarboppfolging.rest.domain.StartEskaleringDTO;
import no.nav.fo.veilarboppfolging.rest.domain.StartKvpDTO;
import no.nav.fo.veilarboppfolging.rest.domain.StoppEskaleringDTO;
import no.nav.fo.veilarboppfolging.rest.domain.StoppKvpDTO;
import no.nav.fo.veilarboppfolging.rest.domain.VeilederBegrunnelseDTO;
import no.nav.fo.veilarboppfolging.services.AktiverBrukerService;
import no.nav.fo.veilarboppfolging.services.HistorikkService;
import no.nav.fo.veilarboppfolging.services.KvpService;
import no.nav.fo.veilarboppfolging.services.MalService;
import no.nav.fo.veilarboppfolging.services.OppfolgingService;
import no.nav.sbl.dialogarena.common.abac.pep.exception.PepException;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static no.nav.fo.veilarboppfolging.utils.FnrUtils.getAktorIdOrElseThrow;

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
    private FnrParameterUtil fnrParameterUtil;

    @Inject
    private AktiverBrukerService aktiverBrukerService;

    @Inject
    private AutorisasjonService autorisasjonService;

    @Inject
    private PepClient pepClient;

    @Inject
    private AktorService aktorService;

    @Override
    public Bruker hentBrukerInfo() throws Exception {
        return new Bruker()
                .setId(getUid())
                .setErVeileder(AutorisasjonService.erInternBruker())
                .setErBruker(AutorisasjonService.erEksternBruker());
    }

    @Override
    public OppfolgingStatus hentOppfolgingsStatus(boolean brukArenaDirekte) throws Exception {
        return tilDto(oppfolgingService.hentOppfolgingsStatus(getFnr(), brukArenaDirekte));
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

        if (AutorisasjonService.erEksternBruker()) {
            return tilDto(oppfolgingService.settDigitalBruker(getFnr()));
        }

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
        String endretAvVeileder = FnrParameterUtil.erEksternBruker()? null : getUid();
        return tilDto(malService.oppdaterMal(mal.getMal(), getFnr(), endretAvVeileder));
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

        autorisasjonService.skalVereSystemRessurs();
        AktorId aktorId = getAktorIdOrElseThrow(aktorService, aktiverArbeidssokerData.getFnr().getFnr());
        pepClient.sjekkSkrivetilgangTilAktorId(aktorId.getAktorId());

        aktiverBrukerService.aktiverBruker(aktiverArbeidssokerData);
    }

    @Override
    public void reaktiverBruker(Fnr fnr) throws Exception {

        autorisasjonService.skalVereSystemRessurs();
        AktorId aktorId = getAktorIdOrElseThrow(aktorService, fnr.getFnr());
        pepClient.sjekkSkrivetilgangTilAktorId(aktorId.getAktorId());

        aktiverBrukerService.reaktiverBruker(fnr);
    }

    @Override
    public void aktiverSykmeldt(SykmeldtBrukerType sykmeldtBrukerType) throws Exception {
        autorisasjonService.skalVereSystemRessurs();
        aktiverBrukerService.aktiverSykmeldt(getFnr(), sykmeldtBrukerType);
    }

    private Eskaleringsvarsel tilDto(EskaleringsvarselData eskaleringsvarselData) {
        return Optional.ofNullable(eskaleringsvarselData)
                .map(eskalering -> Eskaleringsvarsel.builder()
                        .varselId(eskalering.getVarselId())
                        .aktorId(eskalering.getAktorId())
                        .opprettetAv(AutorisasjonService.erInternBruker() ? eskalering.getOpprettetAv() : null)
                        .opprettetDato(eskalering.getOpprettetDato())
                        .avsluttetDato(eskalering.getAvsluttetDato())
                        .tilhorendeDialogId(eskalering.getTilhorendeDialogId())
                        .build()
                ).orElse(null);
    }

    private String getUid() {
        return SubjectHandler.getIdent().orElseThrow(RuntimeException::new);
    }

    private String getFnr() {
        return fnrParameterUtil.getFnr();
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
        OppfolgingStatus status = new OppfolgingStatus()
                .setFnr(oppfolgingStatusData.fnr)
                .setAktorId(oppfolgingStatusData.aktorId)
                .setUnderOppfolging(oppfolgingStatusData.underOppfolging)
                .setManuell(oppfolgingStatusData.manuell)
                .setReservasjonKRR(oppfolgingStatusData.reservasjonKRR)
                .setOppfolgingUtgang(oppfolgingStatusData.getOppfolgingUtgang())
                .setKanReaktiveres(oppfolgingStatusData.kanReaktiveres)
                .setOppfolgingsPerioder(oppfolgingStatusData.oppfolgingsperioder.stream().map(this::tilDTO).collect(toList()))
                .setInaktiveringsdato(oppfolgingStatusData.inaktiveringsdato)
                .setGjeldendeEskaleringsvarsel(tilDto(oppfolgingStatusData.getGjeldendeEskaleringsvarsel()))
                .setErIkkeArbeidssokerUtenOppfolging(oppfolgingStatusData.getErSykmeldtMedArbeidsgiver())
                .setErSykmeldtMedArbeidsgiver(oppfolgingStatusData.getErSykmeldtMedArbeidsgiver())
                .setHarSkriveTilgang(true)
                .setServicegruppe(oppfolgingStatusData.getServicegruppe())
                .setFormidlingsgruppe(oppfolgingStatusData.getFormidlingsgruppe())
                .setRettighetsgruppe(oppfolgingStatusData.getRettighetsgruppe())
                .setKanVarsles(oppfolgingStatusData.kanVarsles);

        if (AutorisasjonService.erInternBruker()) {
            status
                    .setVeilederId(oppfolgingStatusData.veilederId)
                    .setUnderKvp(oppfolgingStatusData.underKvp)
                    .setKanStarteOppfolging(oppfolgingStatusData.isKanStarteOppfolging())
                    .setAvslutningStatus(
                            ofNullable(oppfolgingStatusData.getAvslutningStatusData())
                                    .map(this::tilDto)
                                    .orElse(null)
                    )
                    .setOppfolgingUtgang(oppfolgingStatusData.getOppfolgingUtgang())
                    .setHarSkriveTilgang(oppfolgingStatusData.harSkriveTilgang)
                    .setInaktivIArena(oppfolgingStatusData.inaktivIArena);
        }

        return status;
    }

    private OppfolgingPeriodeDTO tilDTO(Oppfolgingsperiode oppfolgingsperiode) {
        OppfolgingPeriodeDTO periode = new OppfolgingPeriodeDTO()
                .setSluttDato(oppfolgingsperiode.getSluttDato())
                .setStartDato(oppfolgingsperiode.getStartDato());

        if (AutorisasjonService.erInternBruker()) {
            periode.setVeileder(oppfolgingsperiode.getVeileder())
                    .setAktorId(oppfolgingsperiode.getAktorId())
                    .setBegrunnelse(oppfolgingsperiode.getBegrunnelse())
                    .setKvpPerioder(oppfolgingsperiode.getKvpPerioder().stream().map(this::tilDTO).collect(toList()))
            ;
        }

        return periode;
    }

    private KvpPeriodeDTO tilDTO(Kvp kvp) {
        return new KvpPeriodeDTO(kvp.getOpprettetDato(), kvp.getAvsluttetDato());
    }

    private Mal tilDto(MalData malData) {
        return new Mal()
                .setMal(malData.getMal())
                .setEndretAv(malData.getEndretAvFormattert())
                .setDato(malData.getDato());
    }
}
