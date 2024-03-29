package no.nav.veilarboppfolging.client.ytelseskontrakt;

import no.nav.tjeneste.virksomhet.ytelseskontrakt.v3.HentYtelseskontraktListeSikkerhetsbegrensning;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static no.nav.veilarboppfolging.client.ytelseskontrakt.ActualYtelseskontraktResponse.getKomplettResponse;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class YtelseskontraktDtoMapperTest {

    private static final int ANTALL_YTELSER = 2;
    private static final int ANTALL_VEDTAK = 3;

    @Test
    public void responseInneholderListeMedYtelser() throws HentYtelseskontraktListeSikkerhetsbegrensning {
        YtelseskontraktResponse response = getKomplettResponse();

        assertThat(response.getYtelser().size(), is(ANTALL_YTELSER));
    }

    @Test
    public void responseInneholderListeMedVedtak() throws HentYtelseskontraktListeSikkerhetsbegrensning {
        YtelseskontraktResponse response = getKomplettResponse();

        assertThat(response.getVedtaksliste().size(), is(ANTALL_VEDTAK));
    }

    @Test
    public void responseInneholderRiktigeYtelser() throws HentYtelseskontraktListeSikkerhetsbegrensning {
        YtelseskontraktResponse response = getKomplettResponse();

        List<YtelseskontraktDto> expectedYtelser = ExpectedYtelseskontrakt.getExpectedYtelseskontrakter();

        assertThat(response.getYtelser(), is(expectedYtelser));
    }

    @Test
    public void ytelserIResponseHarStatus() throws HentYtelseskontraktListeSikkerhetsbegrensning {
        final YtelseskontraktResponse komplettResponse = getKomplettResponse();

        final List<String> statusListe = komplettResponse.getYtelser().stream().map(YtelseskontraktDto::getStatus).collect(toList());
        final List<String> expectedStatusListe = new ArrayList<>(asList("Aktiv", "Lukket"));

        assertThat(statusListe, is(expectedStatusListe));
    }

    @Test
    public void responseInneholderRiktigeVedtak() throws HentYtelseskontraktListeSikkerhetsbegrensning {
        YtelseskontraktResponse response = getKomplettResponse();

        List<VedtakDto> expectedVedtakDto = ExpectedYtelseskontrakt.getExpectedVedtak();

        assertThat(response.getVedtaksliste(), is((expectedVedtakDto)));
    }

    @Test
    public void handtererManglendeVedtakstype() throws HentYtelseskontraktListeSikkerhetsbegrensning {
        YtelseskontraktResponse response = ActualYtelseskontraktResponse.getResponseUtenVedtakstype();

        List<VedtakDto> expectedVedtakDto = ExpectedYtelseskontrakt.getExpectedVedtakUtenVedtaksgruppe();

        assertThat(response.getVedtaksliste(), is((expectedVedtakDto)));

    }

    @Test
    public void handtererManglendeAktivitetsfase() throws HentYtelseskontraktListeSikkerhetsbegrensning {
        YtelseskontraktResponse response = ActualYtelseskontraktResponse.getResponseUtenAktivitetsfase();

        List<VedtakDto> expectedVedtakDto = ExpectedYtelseskontrakt.getExpectedVedtakUtenAktivitetsfase();

        assertThat(response.getVedtaksliste(), is((expectedVedtakDto)));
    }

    @Test
    public void handtererManglendeRettighetsgruppe() throws HentYtelseskontraktListeSikkerhetsbegrensning {
        YtelseskontraktResponse response = ActualYtelseskontraktResponse.getResponseUtenRettighetsgruppe();

        List<VedtakDto> expectedVedtakDto = ExpectedYtelseskontrakt.getExpectedVedtakUtenRettighetsgruppe();

        assertThat(response.getVedtaksliste(), is((expectedVedtakDto)));
    }

}
