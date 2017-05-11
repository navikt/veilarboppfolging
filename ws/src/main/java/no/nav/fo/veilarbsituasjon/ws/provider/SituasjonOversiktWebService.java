package no.nav.fo.veilarbsituasjon.ws.provider;

import lombok.SneakyThrows;
import lombok.val;
import no.nav.apiapp.soap.SoapTjeneste;
import no.nav.brukerdialog.security.context.SubjectHandler;
import no.nav.fo.veilarbsituasjon.domain.MalData;
import no.nav.fo.veilarbsituasjon.domain.OppfolgingStatusData;
import no.nav.fo.veilarbsituasjon.domain.VilkarData;
import no.nav.fo.veilarbsituasjon.services.SituasjonOversiktService;
import no.nav.modig.core.domain.SluttBruker;
import no.nav.tjeneste.virksomhet.behandlesituasjon.v1.binding.*;
import no.nav.tjeneste.virksomhet.behandlesituasjon.v1.informasjon.Mal;
import no.nav.tjeneste.virksomhet.behandlesituasjon.v1.informasjon.Oppfoelgingsstatus;
import no.nav.tjeneste.virksomhet.behandlesituasjon.v1.meldinger.*;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;

import java.sql.Timestamp;
import java.util.Optional;
import java.util.stream.Collectors;

import static javax.ws.rs.core.Response.Status.NOT_IMPLEMENTED;
import static no.nav.fo.veilarbsituasjon.utils.DateUtils.xmlCalendar;
import static no.nav.fo.veilarbsituasjon.utils.StringUtils.emptyIfNull;

@Service
@SoapTjeneste("/Situasjon")
public class SituasjonOversiktWebService implements BehandleSituasjonV1 {

    @Inject
    private SituasjonOversiktService situasjonOversiktService;

    @Override
    public HentOppfoelgingsstatusResponse hentOppfoelgingsstatus(HentOppfoelgingsstatusRequest hentOppfoelgingsstatusRequest) throws HentOppfoelgingsstatusSikkerhetsbegrensning {
        OppfolgingStatusData oppfolgingStatusData = null;
        try {
            oppfolgingStatusData = situasjonOversiktService.hentOppfolgingsStatus(hentOppfoelgingsstatusRequest.getPersonident());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return mapToHentOppfoelgingsstatusResponse(oppfolgingStatusData);
    }

    @Override
    public HentVilkaarsstatusResponse hentVilkaarsstatus(HentVilkaarsstatusRequest hentVilkaarsstatusRequest) throws HentVilkaarsstatusSikkerhetsbegrensning {
        throw new WebApplicationException(NOT_IMPLEMENTED);
    }

    @Override
    public HentVilkaarsstatusListeResponse hentVilkaarsstatusListe(HentVilkaarsstatusListeRequest hentVilkaarsstatusListeRequest) throws HentVilkaarsstatusListeSikkerhetsbegrensning {
        throw new WebApplicationException(NOT_IMPLEMENTED);
    }

    @Override
    public void ping() {
    }

    @Override
    @SneakyThrows
    public OpprettVilkaarsstatusResponse opprettVilkaarsstatus(OpprettVilkaarsstatusRequest opprettVilkaarsstatusRequest)
            throws OpprettVilkaarsstatusSikkerhetsbegrensning, OpprettVilkaarsstatusUgyldigInput {
        situasjonOversiktService.godtaVilkar(opprettVilkaarsstatusRequest.getHash(), opprettVilkaarsstatusRequest.getPersonident());
//        TODO: Skal vi returnere noe fornufig her? I Proxyen kaller vi hentOppfolgingStatus etter godta.
//        Vi kunne returnert HentOppfoelgingsstatusResponse her
        return new OpprettVilkaarsstatusResponse();
    }

    @Override
    @SneakyThrows
    public HentVilkaarResponse hentVilkaar(HentVilkaarRequest hentVilkaarRequest) throws HentVilkaarSikkerhetsbegrensning {
        return mapTilHentVilkaarResponse(situasjonOversiktService.hentVilkar(getSluttbrukerFnr()));
    }

    @Override
    public HentMalResponse hentMal(HentMalRequest hentMalRequest) {
        String personident = hentMalRequest.getPersonident();
        val mal = mapTilMal(situasjonOversiktService.hentMal(personident),personident);

        val res = new HentMalResponse();
        res.setMal(mal);
        return res;
    }

    @Override
    public HentMalListeResponse hentMalListe(HentMalListeRequest hentMalListeRequest) {
        String personident = hentMalListeRequest.getPersonident();
        val malListe = situasjonOversiktService.hentMalList(personident)
                .stream()
                .map(malData -> mapTilMal(malData, personident))
                .collect(Collectors.toList());

        val res = new HentMalListeResponse();
        res.getMalListe().addAll(malListe);
        return res;
    }

    @Override
    public OpprettMalResponse opprettMal(OpprettMalRequest opprettMalRequest) {
        situasjonOversiktService.oppdaterMal(opprettMalRequest.getMal().getMal(), opprettMalRequest.getPersonident(), null);
        return new OpprettMalResponse();
    }

    private String getSluttbrukerFnr() {
        return SubjectHandler
                .getSubjectHandler()
                .getSubject()
                .getPrincipals(SluttBruker.class)
                .stream()
                .findAny()
                .map(SluttBruker::getUid)
                .orElseThrow(RuntimeException::new);
    }

    private HentVilkaarResponse mapTilHentVilkaarResponse(VilkarData vilkarData) {
        val res = new HentVilkaarResponse();
        res.setVilkaarstekst(vilkarData.getText());
        res.setHash(vilkarData.getHash());
        return res;
    }

    private HentOppfoelgingsstatusResponse mapToHentOppfoelgingsstatusResponse(OppfolgingStatusData oppfolgingStatusData) {
        Oppfoelgingsstatus oppfoelgingsstatus = new Oppfoelgingsstatus();
        oppfoelgingsstatus.setErBrukerSattTilManuell(oppfolgingStatusData.isManuell());
        oppfoelgingsstatus.setErBrukerUnderOppfoelging(oppfolgingStatusData.isUnderOppfolging());
        oppfoelgingsstatus.setErReservertIKontaktOgReservasjonsregisteret(oppfolgingStatusData.isReservasjonKRR());
        oppfoelgingsstatus.setMaaVilkaarBesvares(oppfolgingStatusData.isVilkarMaBesvares());
        oppfoelgingsstatus.setPersonident(oppfolgingStatusData.getFnr());
        oppfoelgingsstatus.setOppfoelgingUtgang(xmlCalendar(oppfolgingStatusData.getOppfolgingUtgang()));

        val res = new HentOppfoelgingsstatusResponse();
        res.setOppfoelgingsstatus(oppfoelgingsstatus);

        return res;
    }

    private Mal mapTilMal(MalData malData, String personident) {
        val mal = new Mal();
        mal.setMal(emptyIfNull(malData.getMal()));
        mal.setEndretAv(malData.erEndretAvBruker() ? personident : emptyIfNull(malData.getEndretAvFormattert()));
        mal.setDato(xmlCalendar(Optional.ofNullable(malData.getDato()).orElse(new Timestamp(System.currentTimeMillis()))));
        return mal;
    }

}
