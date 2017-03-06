package no.nav.fo.veilarbsituasjon.mappers;

import no.nav.fo.veilarbsituasjon.mock.OppfoelgingV1Mock;
import no.nav.fo.veilarbsituasjon.rest.domain.OppfoelgingskontraktData;
import no.nav.fo.veilarbsituasjon.rest.domain.OppfoelgingskontraktResponse;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.HentOppfoelgingskontraktListeSikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.OppfoelgingPortType;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.informasjon.WSPeriode;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.meldinger.WSHentOppfoelgingskontraktListeRequest;
import org.junit.Test;

import javax.xml.datatype.XMLGregorianCalendar;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static no.nav.fo.veilarbsituasjon.utils.CalendarConverter.convertDateToXMLGregorianCalendar;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class OppfoelgingMapperTest {

    private static final int MANEDER_BAK_I_TID = 2;
    private static final int MANEDER_FREM_I_TID = 1;
    private static final int ANTALL_OPPFOELGINGSKONTRAKTER = 4;

    @Test
    public void oppfoelgingskontrakterInneholderListeMedOppfoelgingskontrakter() throws HentOppfoelgingskontraktListeSikkerhetsbegrensning {
        OppfoelgingPortType oppfoelgingMock = new OppfoelgingV1Mock();

        List<OppfoelgingskontraktData> oppfoelgingskontrakter = getOppfoelgingskontrakter(oppfoelgingMock);

        assertThat(oppfoelgingskontrakter.size(), is(ANTALL_OPPFOELGINGSKONTRAKTER));
    }

    @Test
    public void oppfoelgingskontrakterHarRiktigStatus() throws HentOppfoelgingskontraktListeSikkerhetsbegrensning {
        OppfoelgingPortType oppfoelgingMock = new OppfoelgingV1Mock();

        List<OppfoelgingskontraktData> oppfoelgingskontrakter = getOppfoelgingskontrakter(oppfoelgingMock);

        final List statusList = oppfoelgingskontrakter.stream()
                .map(OppfoelgingskontraktData::getStatus)
                .collect(toList());

        final List<String> expectedStatusList = new ArrayList<>(asList("Aktiv", "Aktiv", "LUKKET", "LUKKET"));

        assertThat(statusList, is(expectedStatusList));

    }

    @Test
    public void oppfoelgingskontrakterHarRiktigServicegruppe() throws HentOppfoelgingskontraktListeSikkerhetsbegrensning {
        OppfoelgingPortType oppfoelgingMock = new OppfoelgingV1Mock();

        List<OppfoelgingskontraktData> oppfoelgingskontrakter = getOppfoelgingskontrakter(oppfoelgingMock);

        final List innsatsgrupper = oppfoelgingskontrakter.stream()
                .map(OppfoelgingskontraktData::getInnsatsgrupper)
                .flatMap(Collection::stream)
                .collect(toList());

        final List<String> expectedInnsatsgrupper = new ArrayList<>(asList("Ikke vurdert", "Standardinnsats", "Ikke vurdert", "Standardinnsats"));

        assertThat(innsatsgrupper, is(expectedInnsatsgrupper));

    }

    private List<OppfoelgingskontraktData> getOppfoelgingskontrakter(OppfoelgingPortType oppfoelgingMock) throws HentOppfoelgingskontraktListeSikkerhetsbegrensning {
        final WSHentOppfoelgingskontraktListeRequest request = getWsHentOppfoelgingskontraktListeRequest();

        final OppfoelgingskontraktResponse response = OppfoelgingMapper.tilOppfoelgingskontrakt(oppfoelgingMock.hentOppfoelgingskontraktListe(request));

        return response.getOppfoelgingskontrakter();
    }

    private WSHentOppfoelgingskontraktListeRequest getWsHentOppfoelgingskontraktListeRequest() {
        final WSHentOppfoelgingskontraktListeRequest request = new WSHentOppfoelgingskontraktListeRequest();
        final WSPeriode periode = new WSPeriode();
        final String fnr = "***REMOVED***";
        LocalDate periodeFom = LocalDate.now().minusMonths(MANEDER_BAK_I_TID);
        LocalDate periodeTom = LocalDate.now().plusMonths(MANEDER_FREM_I_TID);
        XMLGregorianCalendar fom = convertDateToXMLGregorianCalendar(periodeFom);
        XMLGregorianCalendar tom = convertDateToXMLGregorianCalendar(periodeTom);
        periode.withFom(fom).withTom(tom);
        request.withPeriode(periode).withPersonidentifikator(fnr);
        return request;
    }

}