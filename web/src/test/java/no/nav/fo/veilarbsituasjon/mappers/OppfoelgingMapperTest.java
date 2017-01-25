package no.nav.fo.veilarbsituasjon.mappers;

import no.nav.fo.veilarbsituasjon.mock.OppfoelgingV1Mock;
import no.nav.fo.veilarbsituasjon.rest.domain.Oppfoelgingskontrakt;
import no.nav.fo.veilarbsituasjon.rest.domain.OppfoelgingskontraktResponse;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.HentOppfoelgingskontraktListeSikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.OppfoelgingPortType;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.informasjon.WSPeriode;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.meldinger.WSHentOppfoelgingskontraktListeRequest;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.meldinger.WSHentOppfoelgingskontraktListeResponse;
import org.junit.Test;

import javax.xml.datatype.XMLGregorianCalendar;
import java.time.LocalDate;
import java.util.*;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static no.nav.fo.veilarbsituasjon.mock.OppfoelgingV1Mock.leggTilStatusOgServicegruppePaResponse;
import static no.nav.fo.veilarbsituasjon.utils.CalendarConverter.convertDateToXMLGregorianCalendar;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class OppfoelgingMapperTest {
    private static final int MANEDER_BAK_I_TID = 2;
    private static final int MANEDER_FREM_I_TID = 1;
    private static final int ANTALL_OPPFOELGINGSKONTRAKTER = 4;

    @Test
    public void inneholderListeMedOppfoelgingskontrakter() throws HentOppfoelgingskontraktListeSikkerhetsbegrensning {
        OppfoelgingPortType oppfoelgingMock = new OppfoelgingV1Mock();

        List<Oppfoelgingskontrakt> oppfoelgingskontrakter = getOppfoelgingskontrakter(oppfoelgingMock);

        assertThat(oppfoelgingskontrakter.size(), is(ANTALL_OPPFOELGINGSKONTRAKTER));
    }

    @Test
    public void oppfoelgingskontrakterHarRiktigStatus() throws HentOppfoelgingskontraktListeSikkerhetsbegrensning {
        OppfoelgingPortType oppfoelgingMock = new OppfoelgingV1Mock();

        List<Oppfoelgingskontrakt> oppfoelgingskontrakter = getOppfoelgingskontrakter(oppfoelgingMock);

        final List statusList = oppfoelgingskontrakter.stream()
                .map(Oppfoelgingskontrakt::getStatus)
                .collect(toList());

        final List<String> expectedStatusList = new ArrayList<>(asList("Aktiv", "Aktiv", "LUKKET", "LUKKET"));

        assertThat(statusList, is(expectedStatusList));

    }

    @Test
    public void oppfoelgingskontrakterHarRiktigServicegruppe() throws HentOppfoelgingskontraktListeSikkerhetsbegrensning {
        OppfoelgingPortType oppfoelgingMock = new OppfoelgingV1Mock();

        List<Oppfoelgingskontrakt> oppfoelgingskontrakter = getOppfoelgingskontrakter(oppfoelgingMock);

        final List innsatsgrupper = oppfoelgingskontrakter.stream()
                .map(Oppfoelgingskontrakt::getInnsatsgrupper)
                .flatMap(Collection::stream)
                .collect(toList());

        final List<String> expectedInnsatsgrupper = new ArrayList<>(asList("Ikke vurdert", "Standardinnsats", "Ikke vurdert", "Standardinnsats"));

        assertThat(innsatsgrupper, is(expectedInnsatsgrupper));

    }

    @Test(expected = NullPointerException.class)
    public void manglandeStatusGirException() throws HentOppfoelgingskontraktListeSikkerhetsbegrensning {
        OppfoelgingPortType oppfoelgingPortTypeMock = mock(OppfoelgingPortType.class);
        when(oppfoelgingPortTypeMock.hentOppfoelgingskontraktListe(any(WSHentOppfoelgingskontraktListeRequest.class)))
                .thenReturn(getResponseUtenStatus());
        getOppfoelgingskontrakter(oppfoelgingPortTypeMock);
    }

    @Test(expected = NullPointerException.class)
    public void manglandeServicegruppeGirException() throws HentOppfoelgingskontraktListeSikkerhetsbegrensning {
        OppfoelgingPortType oppfoelgingPortTypeMock = mock(OppfoelgingPortType.class);
        when(oppfoelgingPortTypeMock.hentOppfoelgingskontraktListe(any(WSHentOppfoelgingskontraktListeRequest.class)))
                .thenReturn(getResponseUtenServicegruppe());
        getOppfoelgingskontrakter(oppfoelgingPortTypeMock);
    }

    private List<Oppfoelgingskontrakt> getOppfoelgingskontrakter(OppfoelgingPortType oppfoelgingMock) throws HentOppfoelgingskontraktListeSikkerhetsbegrensning {
        final WSHentOppfoelgingskontraktListeRequest request = getWsHentOppfoelgingskontraktListeRequest();

        final OppfoelgingskontraktResponse response = OppfoelgingMapper.tilOppfoelgingskontrakt(oppfoelgingMock.hentOppfoelgingskontraktListe(request));

        return response.getOppfoelgingskontrakter();
    }

    private WSHentOppfoelgingskontraktListeResponse getResponseUtenServicegruppe() {
        WSHentOppfoelgingskontraktListeResponse response = new WSHentOppfoelgingskontraktListeResponse()
                .withOppfoelgingskontraktListe(new ArrayList<>());
        final String serviceGruppe = null;
        final String status = "Aktiv";
        leggTilStatusOgServicegruppePaResponse(response, serviceGruppe, status);
        return response;
    }

    private WSHentOppfoelgingskontraktListeResponse getResponseUtenStatus() {
        WSHentOppfoelgingskontraktListeResponse response = new WSHentOppfoelgingskontraktListeResponse()
                .withOppfoelgingskontraktListe(new ArrayList<>());
        final String serviceGruppe = "Standardinnsats";
        final String status = null;
        leggTilStatusOgServicegruppePaResponse(response, serviceGruppe, status);
        return response;

    }

    private WSHentOppfoelgingskontraktListeRequest getWsHentOppfoelgingskontraktListeRequest() {
        final WSHentOppfoelgingskontraktListeRequest request = new WSHentOppfoelgingskontraktListeRequest();
        final WSPeriode periode = new WSPeriode();
        LocalDate periodeFom = LocalDate.now().minusMonths(MANEDER_BAK_I_TID);
        LocalDate periodeTom = LocalDate.now().plusMonths(MANEDER_FREM_I_TID);
        XMLGregorianCalendar fom = convertDateToXMLGregorianCalendar(periodeFom);
        XMLGregorianCalendar tom = convertDateToXMLGregorianCalendar(periodeTom);
        periode.setFom(fom);
        periode.setTom(tom);
        request.setPeriode(periode);
        String fnr = "***REMOVED***";
        request.setPersonidentifikator(fnr);
        return request;
    }

}