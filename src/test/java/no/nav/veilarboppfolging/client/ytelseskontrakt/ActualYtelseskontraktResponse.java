package no.nav.veilarboppfolging.client.ytelseskontrakt;

import no.nav.tjeneste.virksomhet.ytelseskontrakt.v3.HentYtelseskontraktListeSikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.ytelseskontrakt.v3.YtelseskontraktV3;
import no.nav.tjeneste.virksomhet.ytelseskontrakt.v3.informasjon.ytelseskontrakt.WSPeriode;
import no.nav.tjeneste.virksomhet.ytelseskontrakt.v3.meldinger.WSHentYtelseskontraktListeRequest;
import no.nav.tjeneste.virksomhet.ytelseskontrakt.v3.meldinger.WSHentYtelseskontraktListeResponse;
import no.nav.veilarboppfolging.mock.YtelseskontraktV3Mock;

import javax.xml.datatype.XMLGregorianCalendar;
import java.time.LocalDate;

import static no.nav.veilarboppfolging.utils.DateUtils.convertDateToXMLGregorianCalendar;


public class ActualYtelseskontraktResponse {

    private static final int MANEDER_BAK_I_TID = 2;
    private static final int MANEDER_FREM_I_TID = 1;

    public static YtelseskontraktResponse getResponseUtenRettighetsgruppe() throws HentYtelseskontraktListeSikkerhetsbegrensning {
        final YtelseskontraktResponse komplettResponse = getKomplettResponse();
        komplettResponse.getVedtaksliste().forEach(vedtak -> vedtak.setRettighetsgruppe(null));
        return komplettResponse;
    }

    public static YtelseskontraktResponse getResponseUtenVedtakstype() throws HentYtelseskontraktListeSikkerhetsbegrensning {
        final YtelseskontraktResponse komplettResponse = getKomplettResponse();
        komplettResponse.getVedtaksliste().forEach(vedtak -> vedtak.setVedtakstype(null));
        return komplettResponse;
    }

    public static YtelseskontraktResponse getResponseUtenAktivitetsfase() throws HentYtelseskontraktListeSikkerhetsbegrensning {
        final YtelseskontraktResponse komplettResponse = getKomplettResponse();
        komplettResponse.getVedtaksliste().forEach(vedtak -> vedtak.setAktivitetsfase(null));
        return komplettResponse;
    }

    public static YtelseskontraktResponse getKomplettResponse() throws HentYtelseskontraktListeSikkerhetsbegrensning {
        YtelseskontraktV3 ytelseskontraktMock = new YtelseskontraktV3Mock();

        final WSHentYtelseskontraktListeRequest request = getWSHentYtelseskontraktListeRequest();

        final WSHentYtelseskontraktListeResponse rawResponse = ytelseskontraktMock.hentYtelseskontraktListe(request);
        return new YtelseskontraktMapper().tilYtelseskontrakt(rawResponse);

    }

    private static WSHentYtelseskontraktListeRequest getWSHentYtelseskontraktListeRequest() {
        String personId = "fnr";

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
