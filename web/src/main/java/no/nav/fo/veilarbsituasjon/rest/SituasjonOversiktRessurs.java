package no.nav.fo.veilarbsituasjon.rest;

import lombok.SneakyThrows;
import no.nav.brukerdialog.security.context.SubjectHandler;
import no.nav.brukerdialog.security.domain.IdentType;
import no.nav.fo.veilarbsituasjon.domain.*;
import no.nav.fo.veilarbsituasjon.mappers.VilkarMapper;
import no.nav.fo.veilarbsituasjon.rest.api.SituasjonOversikt;
import no.nav.fo.veilarbsituasjon.rest.api.SituasjonOversiktVeileder;
import no.nav.fo.veilarbsituasjon.rest.domain.*;
import no.nav.fo.veilarbsituasjon.services.PepClient;
import no.nav.fo.veilarbsituasjon.services.SituasjonOversiktService;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;

@Component
public class SituasjonOversiktRessurs implements SituasjonOversikt, SituasjonOversiktVeileder {

    @Inject
    private SituasjonOversiktService situasjonOversiktService;

    @Inject
    private Provider<HttpServletRequest> requestProvider;

    @Inject
    private PepClient pepClient;

    @Override
    public Bruker hentBrukerInfo() throws Exception {return new Bruker()
                .setId(getUid())
                .setErVeileder(SubjectHandler.getSubjectHandler().getIdentType() == IdentType.InternBruker);
    }

    @Override
    public OppfolgingStatus hentOppfolgingsStatus() throws Exception {
        return tilDto(situasjonOversiktService.hentOppfolgingsStatus(getFnr()));
    }

    @Override
    public AvslutningStatus hentAvslutningStatus() throws Exception {
        return tilDTO(situasjonOversiktService.hentAvslutningStatus(getFnr()));
    }

    private AvslutningStatus tilDTO(AvslutningStatusData avslutningStatusData) {
        return new AvslutningStatus(
                avslutningStatusData.kanAvslutte,
                avslutningStatusData.harYtelser,
                avslutningStatusData.underOppfolging,
                avslutningStatusData.harTiltak,
                avslutningStatusData.inaktiveringsDato
        );
    }

    @Override
    public Vilkar hentVilkar() throws Exception {
        return tilDto(situasjonOversiktService.hentVilkar(getFnr()));
    }

    @Override
    public List<Vilkar> hentVilkaarStatusListe() {
        return situasjonOversiktService.hentHistoriskeVilkar(getFnr())
                .stream()
                .map(this::tilDto)
                .collect(Collectors.toList());
    }

    @Override
    public OppfolgingStatus godta(String hash) throws Exception {
        return tilDto(situasjonOversiktService.oppdaterVilkaar(hash, getFnr(), VilkarStatus.GODKJENT));
    }

    @Override
    public OppfolgingStatus avslaa(String hash) throws Exception {
        return tilDto(situasjonOversiktService.oppdaterVilkaar(hash, getFnr(), VilkarStatus.AVSLATT));
    }

    @Override
    public Mal hentMal() {
        return tilDto(situasjonOversiktService.hentMal(getFnr()));
    }

    @Override
    public List<Mal> hentMalListe() {
        List<MalData> malDataList = situasjonOversiktService.hentMalList(getFnr());
        return malDataList.stream().map(this::tilDto).collect(toList());
    }

    @Override
    public Mal oppdaterMal(Mal mal) {
        return tilDto(situasjonOversiktService.oppdaterMal(mal.getMal(), getFnr(), getUid()));
    }

    private String getUid() {
        return SubjectHandler.getSubjectHandler().getUid();
    }

    @SneakyThrows
    private String getFnr() {
        final String fnr = requestProvider.get().getParameter("fnr");
        pepClient.isServiceCallAllowed(fnr);
        return fnr;
    }

    private OppfolgingStatus tilDto(OppfolgingStatusData oppfolgingStatusData) {
        return new OppfolgingStatus()
                .setFnr(oppfolgingStatusData.fnr)
                .setManuell(oppfolgingStatusData.manuell)
                .setReservasjonKRR(oppfolgingStatusData.reservasjonKRR)
                .setUnderOppfolging(oppfolgingStatusData.underOppfolging)
                .setVilkarMaBesvares(oppfolgingStatusData.vilkarMaBesvares)
                .setOppfolgingUtgang(oppfolgingStatusData.getOppfolgingUtgang())
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
