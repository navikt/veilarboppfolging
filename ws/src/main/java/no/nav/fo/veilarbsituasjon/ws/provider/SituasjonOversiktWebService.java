package no.nav.fo.veilarbsituasjon.ws.provider;

import lombok.SneakyThrows;
import lombok.val;
import no.nav.apiapp.soap.SoapTjeneste;
import no.nav.fo.veilarbsituasjon.domain.Brukervilkar;
import no.nav.fo.veilarbsituasjon.domain.MalData;
import no.nav.fo.veilarbsituasjon.domain.OppfolgingStatusData;
import no.nav.fo.veilarbsituasjon.domain.VilkarStatus;
import no.nav.fo.veilarbsituasjon.services.SituasjonOversiktService;
import no.nav.fo.veilarbsituasjon.utils.CalendarConverter;
import no.nav.tjeneste.virksomhet.behandlesituasjon.v1.binding.*;
import no.nav.tjeneste.virksomhet.behandlesituasjon.v1.informasjon.Mal;
import no.nav.tjeneste.virksomhet.behandlesituasjon.v1.informasjon.Oppfoelgingsstatus;
import no.nav.tjeneste.virksomhet.behandlesituasjon.v1.informasjon.Vilkaarsstatus;
import no.nav.tjeneste.virksomhet.behandlesituasjon.v1.informasjon.Vilkaarsstatuser;
import no.nav.tjeneste.virksomhet.behandlesituasjon.v1.meldinger.*;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static javax.ws.rs.core.Response.Status.NOT_IMPLEMENTED;
import static no.nav.fo.veilarbsituasjon.domain.VilkarStatus.*;
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
        return mapToHentVilkaarsstatusListeResponse(situasjonOversiktService.hentHistoriskeVilkar(hentVilkaarsstatusListeRequest.getPersonident()));
    }

    @Override
    public void ping() {
    }

    @Override
    @SneakyThrows
    public OpprettVilkaarsstatusResponse opprettVilkaarsstatus(OpprettVilkaarsstatusRequest opprettVilkaarsstatusRequest)
            throws OpprettVilkaarsstatusSikkerhetsbegrensning, OpprettVilkaarsstatusUgyldigInput {
        situasjonOversiktService.oppdaterVilkaar(
                opprettVilkaarsstatusRequest.getHash(),
                opprettVilkaarsstatusRequest.getPersonident(),
                mapVilkaarstatuserTilVilkarStatus(opprettVilkaarsstatusRequest.getStatus())
        );
//        TODO: Skal vi returnere noe fornufig her? I Proxyen kaller vi hentOppfolgingStatus etter godta.
//        Vi kunne returnert HentOppfoelgingsstatusResponse her
        return new OpprettVilkaarsstatusResponse();
    }

    @Override
    @SneakyThrows
    public HentVilkaarResponse hentVilkaar(HentVilkaarRequest hentVilkaarRequest) throws HentVilkaarSikkerhetsbegrensning {
        return mapTilHentVilkaarResponse(situasjonOversiktService.hentVilkar(hentVilkaarRequest.getPersonident()));
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

    private HentVilkaarResponse mapTilHentVilkaarResponse(Brukervilkar brukervilkar) {
        val res = new HentVilkaarResponse();
        res.setVilkaarstekst(brukervilkar.getTekst());
        res.setHash(brukervilkar.getHash());
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

    private static VilkarStatus mapVilkaarstatuserTilVilkarStatus(Vilkaarsstatuser vilkaarStatuser) {
        switch (vilkaarStatuser) {
            case AVSLAATT:
                return AVSLATT;
            case GODKJENT:
                return GODKJENT;
            default:
                return IKKE_BESVART;
        }
    }

    private static Vilkaarsstatuser mapVilkarStatusTilVilkaarstatuser(VilkarStatus vilkarStatus) {
        switch (vilkarStatus) {
            case AVSLATT:
                return Vilkaarsstatuser.AVSLAATT;
            case GODKJENT:
                return Vilkaarsstatuser.GODKJENT;
            default:
                return Vilkaarsstatuser.IKKE_BESVART;
        }
    }

    private HentVilkaarsstatusListeResponse mapToHentVilkaarsstatusListeResponse(List<Brukervilkar> brukervilkarList) {
        HentVilkaarsstatusListeResponse hentVilkaarsstatusListeResponse = new HentVilkaarsstatusListeResponse();
        hentVilkaarsstatusListeResponse.getVilkaarsstatusListe().addAll(
                brukervilkarList.stream()
                        .map(SituasjonOversiktWebService::mapBrukervilkarToVilkaarstatus)
                        .collect(Collectors.toList())
        );
        return hentVilkaarsstatusListeResponse;
    }

    private static Vilkaarsstatus mapBrukervilkarToVilkaarstatus(Brukervilkar brukervilkar) {
        Vilkaarsstatus vilkaarsstatus = new Vilkaarsstatus();
        vilkaarsstatus.setDato(CalendarConverter.convertTimestampToXMLGregorianCalendar((brukervilkar.getDato())));
        vilkaarsstatus.setVilkaarstekst(brukervilkar.getTekst());
        vilkaarsstatus.setHash(brukervilkar.getHash());
        vilkaarsstatus.setStatus(mapVilkarStatusTilVilkaarstatuser(brukervilkar.getVilkarstatus()));
        return vilkaarsstatus;
    }
}
