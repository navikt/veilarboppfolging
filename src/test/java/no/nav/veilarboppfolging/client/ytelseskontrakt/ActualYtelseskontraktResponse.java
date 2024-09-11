package no.nav.veilarboppfolging.client.ytelseskontrakt;

import no.nav.tjeneste.virksomhet.ytelseskontrakt.v3.HentYtelseskontraktListeSikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.ytelseskontrakt.v3.YtelseskontraktV3;
import no.nav.tjeneste.virksomhet.ytelseskontrakt.v3.informasjon.ytelseskontrakt.Periode;
import no.nav.tjeneste.virksomhet.ytelseskontrakt.v3.meldinger.HentYtelseskontraktListeRequest;
import no.nav.tjeneste.virksomhet.ytelseskontrakt.v3.meldinger.HentYtelseskontraktListeResponse;
import no.nav.veilarboppfolging.controller.response.YtelserResponse;
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

        final HentYtelseskontraktListeRequest request = getWSHentYtelseskontraktListeRequest();

        final HentYtelseskontraktListeResponse rawResponse = ytelseskontraktMock.hentYtelseskontraktListe(request);
        return YtelseskontraktMapper.tilYtelseskontrakt(rawResponse);
    }

    private static HentYtelseskontraktListeRequest getWSHentYtelseskontraktListeRequest() {
        String personId = "fnr";

        LocalDate periodeFom = LocalDate.now().minusMonths(MANEDER_BAK_I_TID);
        LocalDate periodeTom = LocalDate.now().plusMonths(MANEDER_FREM_I_TID);
        XMLGregorianCalendar fom = convertDateToXMLGregorianCalendar(periodeFom);
        XMLGregorianCalendar tom = convertDateToXMLGregorianCalendar(periodeTom);
        final Periode periode = new Periode();
        periode.setFom(fom);
        periode.setTom(tom);
        var result = new HentYtelseskontraktListeRequest();
        result.setPeriode(periode);
        result.setPersonidentifikator(personId);
        return result;
    }
}
