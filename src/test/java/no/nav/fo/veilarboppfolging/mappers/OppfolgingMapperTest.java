package no.nav.fo.veilarboppfolging.mappers;

import no.nav.fo.veilarboppfolging.domain.OppfolgingskontraktData;
import no.nav.fo.veilarboppfolging.domain.OppfolgingskontraktResponse;
import no.nav.fo.veilarboppfolging.mock.OppfoelgingV1Mock;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.HentOppfoelgingskontraktListeSikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.OppfoelgingPortType;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.informasjon.Periode;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.meldinger.HentOppfoelgingskontraktListeRequest;
import org.junit.Test;

import javax.xml.datatype.XMLGregorianCalendar;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static no.nav.fo.veilarboppfolging.utils.CalendarConverter.convertDateToXMLGregorianCalendar;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class OppfolgingMapperTest {

    private static final int MANEDER_BAK_I_TID = 2;
    private static final int MANEDER_FREM_I_TID = 1;
    private static final int ANTALL_OPPFOELGINGSKONTRAKTER = 4;
    private OppfolgingMapper oppfolgingMapper = new OppfolgingMapper();

    @Test
    public void oppfoelgingskontrakterInneholderListeMedOppfoelgingskontrakter() throws HentOppfoelgingskontraktListeSikkerhetsbegrensning {
        OppfoelgingPortType oppfoelgingMock = new OppfoelgingV1Mock();

        List<OppfolgingskontraktData> oppfoelgingskontrakter = getOppfoelgingskontrakter(oppfoelgingMock);

        assertThat(oppfoelgingskontrakter.size(), is(ANTALL_OPPFOELGINGSKONTRAKTER));
    }

    @Test
    public void oppfoelgingskontrakterHarRiktigStatus() throws HentOppfoelgingskontraktListeSikkerhetsbegrensning {
        OppfoelgingPortType oppfoelgingMock = new OppfoelgingV1Mock();

        List<OppfolgingskontraktData> oppfoelgingskontrakter = getOppfoelgingskontrakter(oppfoelgingMock);

        final List statusList = oppfoelgingskontrakter.stream()
                .map(OppfolgingskontraktData::getStatus)
                .collect(toList());

        final List<String> expectedStatusList = new ArrayList<>(asList("Aktiv", "Aktiv", "LUKKET", "LUKKET"));

        assertThat(statusList, is(expectedStatusList));

    }

    @Test
    public void oppfoelgingskontrakterHarRiktigServicegruppe() throws HentOppfoelgingskontraktListeSikkerhetsbegrensning {
        OppfoelgingPortType oppfoelgingMock = new OppfoelgingV1Mock();

        List<OppfolgingskontraktData> oppfoelgingskontrakter = getOppfoelgingskontrakter(oppfoelgingMock);

        final List innsatsgrupper = oppfoelgingskontrakter.stream()
                .map(OppfolgingskontraktData::getInnsatsgrupper)
                .flatMap(Collection::stream)
                .collect(toList());

        final List<String> expectedInnsatsgrupper = new ArrayList<>(asList("Ikke vurdert", "Standardinnsats", "Ikke vurdert", "Standardinnsats"));

        assertThat(innsatsgrupper, is(expectedInnsatsgrupper));

    }

    private List<OppfolgingskontraktData> getOppfoelgingskontrakter(OppfoelgingPortType oppfoelgingMock) throws HentOppfoelgingskontraktListeSikkerhetsbegrensning {
        final HentOppfoelgingskontraktListeRequest request = getWsHentOppfoelgingskontraktListeRequest();

        final OppfolgingskontraktResponse response = oppfolgingMapper.tilOppfolgingskontrakt(oppfoelgingMock.hentOppfoelgingskontraktListe(request));

        return response.getOppfoelgingskontrakter();
    }

    private HentOppfoelgingskontraktListeRequest getWsHentOppfoelgingskontraktListeRequest() {
        final HentOppfoelgingskontraktListeRequest request = new HentOppfoelgingskontraktListeRequest();
        final Periode periode = new Periode();
        final String fnr = "fnr";
        LocalDate periodeFom = LocalDate.now().minusMonths(MANEDER_BAK_I_TID);
        LocalDate periodeTom = LocalDate.now().plusMonths(MANEDER_FREM_I_TID);
        XMLGregorianCalendar fom = convertDateToXMLGregorianCalendar(periodeFom);
        XMLGregorianCalendar tom = convertDateToXMLGregorianCalendar(periodeTom);
        periode.setFom(fom);
        periode.setTom(tom);
        request.setPeriode(periode);
        request.setPersonidentifikator(fnr);
        return request;
    }

}