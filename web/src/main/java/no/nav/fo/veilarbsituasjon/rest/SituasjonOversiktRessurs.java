package no.nav.fo.veilarbsituasjon.rest;

import lombok.SneakyThrows;
import no.nav.brukerdialog.security.context.SubjectHandler;
import no.nav.fo.veilarbsituasjon.domain.MalData;
import no.nav.fo.veilarbsituasjon.domain.OppfolgingStatusData;
import no.nav.fo.veilarbsituasjon.domain.VilkarData;
import no.nav.fo.veilarbsituasjon.rest.api.SituasjonOversikt;
import no.nav.fo.veilarbsituasjon.rest.domain.Bruker;
import no.nav.fo.veilarbsituasjon.rest.domain.Mal;
import no.nav.fo.veilarbsituasjon.rest.domain.OppfolgingStatus;
import no.nav.fo.veilarbsituasjon.rest.domain.Vilkar;
import no.nav.fo.veilarbsituasjon.services.PepClient;
import no.nav.fo.veilarbsituasjon.services.SituasjonOversiktService;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

import static java.util.stream.Collectors.toList;

@Component
public class SituasjonOversiktRessurs implements SituasjonOversikt {

    @Inject
    private SituasjonOversiktService situasjonOversiktService;

    @Inject
    private Provider<HttpServletRequest> requestProvider;

    @Inject
    private PepClient pepClient;

    @Override
    public Bruker hentBrukerInfo() throws Exception {
        return new Bruker().setId(getUid());
    }

    @Override
    public OppfolgingStatus hentOppfolgingsStatus() throws Exception {
        return tilDto(situasjonOversiktService.hentOppfolgingsStatus(getFnr()));
    }

    @Override
    public Vilkar hentVilkar() throws Exception {
        return tilDto(situasjonOversiktService.hentVilkar());
    }

    @Override
    public OppfolgingStatus godta(String hash) throws Exception {
        return tilDto(situasjonOversiktService.godtaVilkar(hash, getFnr()));
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

    private Vilkar tilDto(VilkarData vilkarData) {
        return new Vilkar()
                .setText(vilkarData.text)
                .setHash(vilkarData.hash);
    }

    private Mal tilDto(MalData malData) {
        return new Mal()
                .setMal(malData.getMal())
                .setEndretAv(malData.getEndretAvFormattert())
                .setDato(malData.getDato());
    }
}
