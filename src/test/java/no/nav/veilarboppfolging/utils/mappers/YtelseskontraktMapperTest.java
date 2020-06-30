package no.nav.veilarboppfolging.utils.mappers;

import no.nav.veilarboppfolging.controller.domain.Vedtak;
import no.nav.veilarboppfolging.controller.domain.Ytelseskontrakt;
import no.nav.veilarboppfolging.controller.domain.YtelseskontraktResponse;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.HentOppfoelgingskontraktListeSikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.ytelseskontrakt.v3.HentYtelseskontraktListeSikkerhetsbegrensning;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static no.nav.veilarboppfolging.utils.mappers.ActualYtelseskontraktResponse.getKomplettResponse;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class YtelseskontraktMapperTest {

    private static final int ANTALL_YTELSER = 2;
    private static final int ANTALL_VEDTAK = 3;

    @Test
    public void responseInneholderListeMedYtelser() throws HentOppfoelgingskontraktListeSikkerhetsbegrensning, HentYtelseskontraktListeSikkerhetsbegrensning {
        YtelseskontraktResponse response = getKomplettResponse();

        assertThat(response.getYtelser().size(), is(ANTALL_YTELSER));
    }

    @Test
    public void responseInneholderListeMedVedtak() throws HentOppfoelgingskontraktListeSikkerhetsbegrensning, HentYtelseskontraktListeSikkerhetsbegrensning {
        YtelseskontraktResponse response = getKomplettResponse();

        assertThat(response.getVedtaksliste().size(), is(ANTALL_VEDTAK));
    }

    @Test
    public void responseInneholderRiktigeYtelser() throws HentOppfoelgingskontraktListeSikkerhetsbegrensning, HentYtelseskontraktListeSikkerhetsbegrensning {
        YtelseskontraktResponse response = getKomplettResponse();

        List<Ytelseskontrakt> expectedYtelser = ExpectedYtelseskontrakt.getExpectedYtelseskontrakter();

        assertThat(response.getYtelser(), is((expectedYtelser)));
    }

    @Test
    public void ytelserIResponseHarStatus() throws HentOppfoelgingskontraktListeSikkerhetsbegrensning, HentYtelseskontraktListeSikkerhetsbegrensning {
        final YtelseskontraktResponse komplettResponse = getKomplettResponse();

        final List<String> statusListe = komplettResponse.getYtelser().stream().map(Ytelseskontrakt::getStatus).collect(toList());
        final List<String> expectedStatusListe = new ArrayList<>(asList("Aktiv", "Lukket"));

        assertThat(statusListe, is(expectedStatusListe));
    }

    @Test
    public void responseInneholderRiktigeVedtak() throws HentOppfoelgingskontraktListeSikkerhetsbegrensning, HentYtelseskontraktListeSikkerhetsbegrensning {
        YtelseskontraktResponse response = getKomplettResponse();

        List<Vedtak> expectedVedtak = ExpectedYtelseskontrakt.getExpectedVedtak();

        assertThat(response.getVedtaksliste(), is((expectedVedtak)));
    }

    @Test
    public void handtererManglendeVedtakstype() throws HentOppfoelgingskontraktListeSikkerhetsbegrensning, HentYtelseskontraktListeSikkerhetsbegrensning {
        YtelseskontraktResponse response = ActualYtelseskontraktResponse.getResponseUtenVedtakstype();

        List<Vedtak> expectedVedtak = ExpectedYtelseskontrakt.getExpectedVedtakUtenVedtaksgruppe();

        assertThat(response.getVedtaksliste(), is((expectedVedtak)));

    }

    @Test
    public void handtererManglendeAktivitetsfase() throws HentOppfoelgingskontraktListeSikkerhetsbegrensning, HentYtelseskontraktListeSikkerhetsbegrensning {
        YtelseskontraktResponse response = ActualYtelseskontraktResponse.getResponseUtenAktivitetsfase();

        List<Vedtak> expectedVedtak = ExpectedYtelseskontrakt.getExpectedVedtakUtenAktivitetsfase();

        assertThat(response.getVedtaksliste(), is((expectedVedtak)));
    }

    @Test
    public void handtererManglendeRettighetsgruppe() throws HentOppfoelgingskontraktListeSikkerhetsbegrensning, HentYtelseskontraktListeSikkerhetsbegrensning {
        YtelseskontraktResponse response = ActualYtelseskontraktResponse.getResponseUtenRettighetsgruppe();

        List<Vedtak> expectedVedtak = ExpectedYtelseskontrakt.getExpectedVedtakUtenRettighetsgruppe();

        assertThat(response.getVedtaksliste(), is((expectedVedtak)));
    }

}