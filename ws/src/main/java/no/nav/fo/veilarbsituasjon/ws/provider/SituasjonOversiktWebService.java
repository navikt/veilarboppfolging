package no.nav.fo.veilarbsituasjon.ws.provider;

import lombok.SneakyThrows;
import lombok.val;
import no.nav.apiapp.soap.SoapTjeneste;
import no.nav.fo.veilarbsituasjon.domain.*;
import no.nav.fo.veilarbsituasjon.services.SituasjonOversiktService;
import no.nav.fo.veilarbsituasjon.utils.CalendarConverter;
import no.nav.fo.veilarbsituasjon.utils.DateUtils;
import no.nav.tjeneste.virksomhet.behandlesituasjon.v1.binding.*;
import no.nav.tjeneste.virksomhet.behandlesituasjon.v1.informasjon.*;
import no.nav.tjeneste.virksomhet.behandlesituasjon.v1.meldinger.*;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import javax.xml.datatype.DatatypeFactory;
import java.sql.Timestamp;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.toList;
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
        return mapToHentVilkaarsstatusListeResponse(situasjonOversiktService.hentHistoriskeVilkar(hentVilkaarsstatusListeRequest.getPersonident()), hentVilkaarsstatusListeRequest.getPersonident());
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
                .collect(toList());

        val res = new HentMalListeResponse();
        res.getMalListe().addAll(malListe);
        return res;
    }

    @Override
    public OpprettMalResponse opprettMal(OpprettMalRequest opprettMalRequest) {
        situasjonOversiktService.oppdaterMal(opprettMalRequest.getMal().getMal(), opprettMalRequest.getPersonident(), null);
        return new OpprettMalResponse();
    }

    @Override
    public SettDigitalResponse settDigital(SettDigitalRequest settDigitalRequest) {
        val oppfolgingStatusData = situasjonOversiktService.settDigitalBruker(settDigitalRequest.getPersonident());

        if (oppfolgingStatusData.isManuell()) {
            throw new RuntimeException("Klarte ikke å sette digital oppfølging");
        }

        val settDigitalResponse = new SettDigitalResponse();
        settDigitalResponse.setOppfoelgingsstatus(mapTilOppfoelgingstatus(oppfolgingStatusData));
        return settDigitalResponse;
    }

    @SneakyThrows
    private Oppfoelgingsstatus mapTilOppfoelgingstatus(OppfolgingStatusData oppfolgingStatusData) {
        GregorianCalendar gregorianCalendarOppfolgingUtgang = new GregorianCalendar();
        gregorianCalendarOppfolgingUtgang.setTime(oppfolgingStatusData.getOppfolgingUtgang());

        val oppfoelgingstatus = new Oppfoelgingsstatus();
        oppfoelgingstatus.setErBrukerUnderOppfoelging(oppfolgingStatusData.isUnderOppfolging());
        oppfoelgingstatus.setErBrukerSattTilManuell(oppfolgingStatusData.isManuell());
        oppfoelgingstatus.setErReservertIKontaktOgReservasjonsregisteret(oppfolgingStatusData.isReservasjonKRR());
        oppfoelgingstatus.setMaaVilkaarBesvares(oppfolgingStatusData.isVilkarMaBesvares());
        oppfoelgingstatus.setOppfoelgingUtgang(DatatypeFactory.newInstance().newXMLGregorianCalendar(gregorianCalendarOppfolgingUtgang));
        oppfoelgingstatus.setPersonident(oppfolgingStatusData.getFnr());
        return oppfoelgingstatus;
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
        oppfoelgingsstatus.getOppfoelgingsPerioder().addAll(
                oppfolgingStatusData.getOppfolgingsperioder().stream().map(this::mapOppfoelgingsPeriode).collect(toList())
        );

        val res = new HentOppfoelgingsstatusResponse();
        res.setOppfoelgingsstatus(oppfoelgingsstatus);

        return res;
    }

    private OppfoelgingsPeriode mapOppfoelgingsPeriode(Oppfolgingsperiode oppfolgingsperiode) {
        OppfoelgingsPeriode oppfoelgingsPeriode = new OppfoelgingsPeriode();
        oppfoelgingsPeriode.setSluttDato(DateUtils.xmlCalendar(oppfolgingsperiode.getSluttDato()));
        return oppfoelgingsPeriode;
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

    private HentVilkaarsstatusListeResponse mapToHentVilkaarsstatusListeResponse(List<Brukervilkar> brukervilkarList, String ident) {
        HentVilkaarsstatusListeResponse hentVilkaarsstatusListeResponse = new HentVilkaarsstatusListeResponse();
        hentVilkaarsstatusListeResponse.getVilkaarsstatusListe().addAll(
                brukervilkarList.stream()
                        .map((Brukervilkar brukervilkar) -> mapBrukervilkarToVilkaarstatus(brukervilkar, ident))
                        .collect(toList())
        );
        return hentVilkaarsstatusListeResponse;
    }

    private static Vilkaarsstatus mapBrukervilkarToVilkaarstatus(Brukervilkar brukervilkar, String ident) {
        Vilkaarsstatus vilkaarsstatus = new Vilkaarsstatus();
        vilkaarsstatus.setPersonident(ident);
        vilkaarsstatus.setDato(CalendarConverter.convertDateToXMLGregorianCalendar(brukervilkar.getDato()));
        vilkaarsstatus.setVilkaarstekst(brukervilkar.getTekst());
        vilkaarsstatus.setHash(brukervilkar.getHash());
        vilkaarsstatus.setStatus(mapVilkarStatusTilVilkaarstatuser(brukervilkar.getVilkarstatus()));
        return vilkaarsstatus;
    }
}
