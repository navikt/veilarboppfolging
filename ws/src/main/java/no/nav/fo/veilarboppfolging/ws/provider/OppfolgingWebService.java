package no.nav.fo.veilarboppfolging.ws.provider;

import lombok.SneakyThrows;
import lombok.val;
import no.nav.apiapp.soap.SoapTjeneste;
import no.nav.fo.veilarboppfolging.domain.*;
import no.nav.fo.veilarboppfolging.service.ReservertKrrService;
import no.nav.fo.veilarboppfolging.services.MalService;
import no.nav.fo.veilarboppfolging.services.OppfolgingService;
import no.nav.fo.veilarboppfolging.services.registrerBruker.RegistrerBrukerService;
import no.nav.tjeneste.virksomhet.behandleoppfolging.v1.HentReservertKrrRequest;
import no.nav.tjeneste.virksomhet.behandleoppfolging.v1.HentReservertKrrResponse;
import no.nav.tjeneste.virksomhet.behandleoppfolging.v1.binding.*;
import no.nav.tjeneste.virksomhet.behandleoppfolging.v1.informasjon.*;
import no.nav.tjeneste.virksomhet.behandleoppfolging.v1.meldinger.*;
import org.springframework.stereotype.Service;

import javax.ws.rs.WebApplicationException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.Response.Status.NOT_IMPLEMENTED;
import static no.nav.fo.veilarboppfolging.domain.VilkarStatus.*;
import static no.nav.fo.veilarboppfolging.utils.DateUtils.xmlCalendar;
import static no.nav.fo.veilarboppfolging.utils.StringUtils.emptyIfNull;

@Service
@SoapTjeneste("/Oppfolging")
public class OppfolgingWebService implements BehandleOppfolgingV1 {

    private OppfolgingService oppfolgingService;
    private RegistrerBrukerService registrerBrukerService;
    private ReservertKrrService reservertKrrService;
    private MalService malService;

    public OppfolgingWebService(
            OppfolgingService oppfolgingService,
            RegistrerBrukerService registrerBrukerService,
            ReservertKrrService reservertKrrService,
            MalService malService) {

        this.oppfolgingService = oppfolgingService;
        this.registrerBrukerService = registrerBrukerService;
        this.reservertKrrService = reservertKrrService;
        this.malService = malService;
    }

    @Override
    public HentOppfoelgingsstatusResponse hentOppfoelgingsstatus(HentOppfoelgingsstatusRequest hentOppfoelgingsstatusRequest) throws HentOppfoelgingsstatusSikkerhetsbegrensning {
        OppfolgingStatusData oppfolgingStatusData = null;
        try {
            oppfolgingStatusData = oppfolgingService.hentOppfolgingsStatus(hentOppfoelgingsstatusRequest.getPersonident());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        val res = new HentOppfoelgingsstatusResponse();
        res.setOppfoelgingsstatus(mapTilOppfoelgingstatus(oppfolgingStatusData));
        return res;
    }


    @Override
    public HentVilkaarsstatusResponse hentVilkaarsstatus(HentVilkaarsstatusRequest hentVilkaarsstatusRequest) {
        throw new WebApplicationException(NOT_IMPLEMENTED);
    }

    @Override
    public HentVilkaarsstatusListeResponse hentVilkaarsstatusListe(HentVilkaarsstatusListeRequest hentVilkaarsstatusListeRequest) throws HentVilkaarsstatusListeSikkerhetsbegrensning {
        return mapToHentVilkaarsstatusListeResponse(oppfolgingService.hentHistoriskeVilkar(hentVilkaarsstatusListeRequest.getPersonident()), hentVilkaarsstatusListeRequest.getPersonident());
    }

    @Override
    public void ping() {
    }

    @Override
    @SneakyThrows
    public OpprettVilkaarsstatusResponse opprettVilkaarsstatus(OpprettVilkaarsstatusRequest opprettVilkaarsstatusRequest)
            throws OpprettVilkaarsstatusSikkerhetsbegrensning, OpprettVilkaarsstatusUgyldigInput {
        oppfolgingService.oppdaterVilkaar(
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
        return mapTilHentVilkaarResponse(oppfolgingService.hentVilkar(hentVilkaarRequest.getPersonident()));
    }

    @Override
    public HentMalResponse hentMal(HentMalRequest hentMalRequest) {
        String personident = hentMalRequest.getPersonident();
        val mal = mapTilMal(malService.hentMal(personident), personident);

        val res = new HentMalResponse();
        res.setMal(mal);
        return res;
    }

    @Override
    public HentMalListeResponse hentMalListe(HentMalListeRequest hentMalListeRequest) {
        String personident = hentMalListeRequest.getPersonident();
        val malListe = malService.hentMalList(personident)
                .stream()
                .map(malData -> mapTilMal(malData, personident))
                .collect(toList());

        val res = new HentMalListeResponse();
        res.getMalListe().addAll(malListe);
        return res;
    }

    @Override
    public OpprettMalResponse opprettMal(OpprettMalRequest opprettMalRequest) {
        malService.oppdaterMal(opprettMalRequest.getMal().getMal(), opprettMalRequest.getPersonident(), null);
        return new OpprettMalResponse();
    }

    @Override
    public SlettMalResponse slettMal(SlettMalRequest slettMalRequest) {
        malService.slettMal(slettMalRequest.getPersonident());
        return new SlettMalResponse();
    }

    @Override
    public SettDigitalResponse settDigital(SettDigitalRequest settDigitalRequest) {
        val oppfolgingStatusData = oppfolgingService.settDigitalBruker(settDigitalRequest.getPersonident());
        oppfolgingStatusData.setVilkarMaBesvares(true);


        if (oppfolgingStatusData.isManuell()) {
            throw new RuntimeException("Klarte ikke å sette digital oppfølging");
        }

        val settDigitalResponse = new SettDigitalResponse();
        settDigitalResponse.setOppfoelgingsstatus(mapTilOppfoelgingstatus(oppfolgingStatusData));
        return settDigitalResponse;
    }

    @SneakyThrows
    private Oppfoelgingsstatus mapTilOppfoelgingstatus(OppfolgingStatusData oppfolgingStatusData) {
        val oppfoelgingstatus = new Oppfoelgingsstatus();
        oppfoelgingstatus.setErBrukerSattTilManuell(oppfolgingStatusData.isManuell());
        oppfoelgingstatus.setErBrukerUnderOppfoelging(oppfolgingStatusData.isUnderOppfolging());
        oppfoelgingstatus.setErReservertIKontaktOgReservasjonsregisteret(oppfolgingStatusData.isReservasjonKRR());
        oppfoelgingstatus.setMaaVilkaarBesvares(oppfolgingStatusData.isVilkarMaBesvares());
        oppfoelgingstatus.setPersonident(oppfolgingStatusData.getFnr());
        oppfoelgingstatus.setOppfoelgingUtgang(xmlCalendar(oppfolgingStatusData.getOppfolgingUtgang()));
        oppfoelgingstatus.getOppfoelgingsPerioder().addAll(
                oppfolgingStatusData.getOppfolgingsperioder().stream().map(this::mapOppfoelgingsPeriode).collect(toList())
        );
        Optional.ofNullable(oppfolgingStatusData.getGjeldendeEskaleringsvarsel())
                .map(this::mapEskaleringsVarsel)
                .ifPresent(oppfoelgingstatus::setEskaleringsvarsel);

        return oppfoelgingstatus;
    }

    private HentVilkaarResponse mapTilHentVilkaarResponse(Brukervilkar brukervilkar) {
        val res = new HentVilkaarResponse();
        res.setVilkaarstekst(brukervilkar.getTekst());
        res.setHash(brukervilkar.getHash());
        return res;
    }

    private OppfoelgingsPeriode mapOppfoelgingsPeriode(Oppfolgingsperiode oppfolgingsperiode) {
        OppfoelgingsPeriode oppfoelgingsPeriode = new OppfoelgingsPeriode();
        oppfoelgingsPeriode.setStartDato(xmlCalendar(oppfolgingsperiode.getStartDato()));
        oppfoelgingsPeriode.setSluttDato(xmlCalendar(oppfolgingsperiode.getSluttDato()));
        return oppfoelgingsPeriode;
    }

    private Eskaleringsvarsel mapEskaleringsVarsel(EskaleringsvarselData eskalering) {
        val soapEskalering = new Eskaleringsvarsel();

        soapEskalering.setAvsluttetDato(xmlCalendar(eskalering.getAvsluttetDato()));
        soapEskalering.setOpprettetDato(xmlCalendar(eskalering.getOpprettetDato()));
        soapEskalering.setOpprettetAv(eskalering.getOpprettetAv());
        soapEskalering.setTilhorendeDialogId(Long.toString(eskalering.getTilhorendeDialogId()));
        soapEskalering.setVarselId(Long.toString(eskalering.getVarselId()));

        return soapEskalering;
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
        vilkaarsstatus.setDato(xmlCalendar(brukervilkar.getDato()));
        vilkaarsstatus.setVilkaarstekst(brukervilkar.getTekst());
        vilkaarsstatus.setHash(brukervilkar.getHash());
        vilkaarsstatus.setStatus(mapVilkarStatusTilVilkaarstatuser(brukervilkar.getVilkarstatus()));
        return vilkaarsstatus;
    }

    @Override
    public StartRegistreringStatusResponse hentStartRegistreringStatus(StartRegistreringStatusRequest startRegistreringStatusRequest) throws RegistrerBrukerSikkerhetsbegrensning,
            HentStartRegistreringStatusFeilVedHentingAvStatusFraArena,
            HentStartRegistreringStatusFeilVedHentingAvArbeidsforhold {

        StartRegistreringStatus status = registrerBrukerService.hentStartRegistreringStatus(startRegistreringStatusRequest.getFnr());

        StartRegistreringStatusResponse response = new StartRegistreringStatusResponse();
        response.setErUnderOppfolging(status.isUnderOppfolging());
        response.setOppfyllerKrav(status.isOppfyllerKravForAutomatiskRegistrering());
        return response;
    }

    @Override
    public HentReservertKrrResponse hentReservertKrr(HentReservertKrrRequest request) {
        return reservertKrrService.hentReservertKrr(request.getFnr());
    }

    @Override
    public RegistrerBrukerResponse registrerBruker(RegistrerBrukerRequest request) throws RegistrerBrukerSikkerhetsbegrensning {
        RegistrertBruker registrertBruker = mapRegistreringBruker(request);
        RegistrertBruker bruker;
        try {
            bruker = registrerBrukerService.registrerBruker(registrertBruker, request.getFnr());
        } catch (HentStartRegistreringStatusFeilVedHentingAvStatusFraArena | HentStartRegistreringStatusFeilVedHentingAvArbeidsforhold e) {
            throw new RuntimeException(e);
        }
        return mapRegistrerBrukerResponse(bruker);
    }

    private RegistrerBrukerResponse mapRegistrerBrukerResponse(RegistrertBruker bruker) {
        RegistrerBrukerResponse response = new RegistrerBrukerResponse();
        response.setNusKode(bruker.getNusKode());
        response.setYrkesPraksis(bruker.getYrkesPraksis());
        response.setEnigIOppsummering(bruker.isEnigIOppsummering());
        response.setOppsummering(bruker.getOppsummering());
        response.setUtdanningBestatt(bruker.isUtdanningBestatt());
        response.setUtdanningGodkjentNorge(bruker.isUtdanningGodkjentNorge());
        response.setHarHelseutfordringer(bruker.isHarHelseutfordringer());
        response.setSituasjon(bruker.getSituasjon());
        return response;
    }
    private RegistrertBruker mapRegistreringBruker(RegistrerBrukerRequest request) {
        RegistrertBruker bruker = new RegistrertBruker(
                request.getNusKode(),
                request.getYrkesPraksis(),
                null,
                request.isEnigIOppsummering(),
                request.getOppsummering(),
                request.isUtdanningBestatt(),
                request.isUtdanningGodkjentNorge(),
                request.isHarHelseutfordringer(),
                request.getSituasjon()
        );
        return bruker;
    }
}
