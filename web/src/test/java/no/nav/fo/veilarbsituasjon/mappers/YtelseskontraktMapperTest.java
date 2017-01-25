package no.nav.fo.veilarbsituasjon.mappers;

import no.nav.fo.veilarbsituasjon.mock.YtelseskontraktV3Mock;
import no.nav.fo.veilarbsituasjon.rest.domain.Ytelseskontrakt;
import no.nav.fo.veilarbsituasjon.rest.domain.YtelseskontraktResponse;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.HentOppfoelgingskontraktListeSikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.ytelseskontrakt.v3.HentYtelseskontraktListeSikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.ytelseskontrakt.v3.YtelseskontraktV3;
import no.nav.tjeneste.virksomhet.ytelseskontrakt.v3.informasjon.ytelseskontrakt.WSPeriode;
import no.nav.tjeneste.virksomhet.ytelseskontrakt.v3.meldinger.WSHentYtelseskontraktListeRequest;
import no.nav.tjeneste.virksomhet.ytelseskontrakt.v3.meldinger.WSHentYtelseskontraktListeResponse;
import org.junit.Before;
import org.junit.Test;

import javax.xml.datatype.XMLGregorianCalendar;
import java.time.LocalDate;

import static java.util.stream.Collectors.toList;
import static no.nav.fo.veilarbsituasjon.utils.CalendarConverter.convertDateToXMLGregorianCalendar;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;


public class YtelseskontraktMapperTest {

    private static final int MANEDER_BAK_I_TID = 2;
    private static final int MANEDER_FREM_I_TID = 1;
    private static final int ANTALL_YTELSER = 2;
    private static final int ANTALL_VEDTAK = 3;

    private YtelseskontraktResponse response;


    @Before
    public void setup() throws HentOppfoelgingskontraktListeSikkerhetsbegrensning, HentYtelseskontraktListeSikkerhetsbegrensning {
        YtelseskontraktV3 ytelseskontraktMock = new YtelseskontraktV3Mock();

        final WSHentYtelseskontraktListeRequest request = getWSHentYtelseskontraktListeRequest();

        final WSHentYtelseskontraktListeResponse rawResponse = ytelseskontraktMock.hentYtelseskontraktListe(request);
        this.response = YtelseskontraktMapper.tilYtelseskontrakt(rawResponse);

    }

    @Test
    public void inneholderListeMedYtelser() throws Exception {
        assertThat(response.getYtelser().size(), is(ANTALL_YTELSER));
    }

    @Test
    public void inneholderListeMedVedtak() {
        assertThat(response.getVedtaksliste().size(), is(ANTALL_VEDTAK));
    }

    @Test
    public void ytelserHarStatus() {
        response.getYtelser().stream().map(Ytelseskontrakt::getStatus).collect(toList());

    }

    private WSHentYtelseskontraktListeRequest getWSHentYtelseskontraktListeRequest() {
        String personId = "***REMOVED***";

        LocalDate periodeFom = LocalDate.now().minusMonths(MANEDER_BAK_I_TID);
        LocalDate periodeTom = LocalDate.now().plusMonths(MANEDER_FREM_I_TID);
        XMLGregorianCalendar fom = convertDateToXMLGregorianCalendar(periodeFom);
        XMLGregorianCalendar tom = convertDateToXMLGregorianCalendar(periodeTom);
        final WSPeriode periode = new WSPeriode();
        periode.setFom(fom);
        periode.setTom(tom);
        return new WSHentYtelseskontraktListeRequest()
                .withPeriode(periode)
                .withPersonidentifikator(personId);
    }

}