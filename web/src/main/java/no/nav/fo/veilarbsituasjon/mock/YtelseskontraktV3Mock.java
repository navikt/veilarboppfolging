package no.nav.fo.veilarbsituasjon.mock;

import no.nav.fo.veilarbsituasjon.utils.CalendarConverter;
import no.nav.tjeneste.virksomhet.ytelseskontrakt.v3.HentYtelseskontraktListeSikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.ytelseskontrakt.v3.YtelseskontraktV3;
import no.nav.tjeneste.virksomhet.ytelseskontrakt.v3.informasjon.ytelseskontrakt.*;
import no.nav.tjeneste.virksomhet.ytelseskontrakt.v3.meldinger.WSHentYtelseskontraktListeRequest;
import no.nav.tjeneste.virksomhet.ytelseskontrakt.v3.meldinger.WSHentYtelseskontraktListeResponse;

import java.time.LocalDate;
import java.util.List;


public class YtelseskontraktV3Mock implements YtelseskontraktV3 {
    @Override
    public void ping() {

    }

    @Override
    public WSHentYtelseskontraktListeResponse hentYtelseskontraktListe(WSHentYtelseskontraktListeRequest request) throws HentYtelseskontraktListeSikkerhetsbegrensning {
        WSHentYtelseskontraktListeResponse response = new WSHentYtelseskontraktListeResponse();
        final WSBruker bruker = new WSBruker();
        final WSRettighetsgruppe rettighetsgruppe = new WSRettighetsgruppe();
        rettighetsgruppe.setRettighetsGruppe("Dagpenger");
        bruker.setRettighetsgruppe(rettighetsgruppe);
        response.setBruker(bruker);

        final List<WSYtelseskontrakt> ytelseskontraktListe = response.getYtelseskontraktListe();
        final WSYtelseskontrakt ytelseskontrakt1 = new WSYtelseskontrakt();
        ytelseskontrakt1.setFomGyldighetsperiode(CalendarConverter.convertDateToXMLGregorianCalendar(LocalDate.of(2016, 10, 18)));
        ytelseskontrakt1.setDatoKravMottatt(CalendarConverter.convertDateToXMLGregorianCalendar(LocalDate.of(2016, 10, 19)));
        ytelseskontrakt1.setFagsystemSakId(2016608971);
        ytelseskontrakt1.setStatus("Aktiv");
        ytelseskontrakt1.setYtelsestype("Arbeidsavklaringspenger");
        leggTilVedtak1(ytelseskontrakt1);
        ytelseskontraktListe.add(ytelseskontrakt1);

        final WSYtelseskontrakt ytelseskontrakt2 = new WSYtelseskontrakt();
        ytelseskontrakt2.setFomGyldighetsperiode(CalendarConverter.convertDateToXMLGregorianCalendar(LocalDate.of(2016, 4, 1)));
        ytelseskontrakt2.setTomGyldighetsperiode(CalendarConverter.convertDateToXMLGregorianCalendar(LocalDate.of(2016, 8, 29)));
        ytelseskontrakt2.setDatoKravMottatt(CalendarConverter.convertDateToXMLGregorianCalendar(LocalDate.of(2016, 4, 1)));
        ytelseskontrakt2.setFagsystemSakId(2015591345);
        ytelseskontrakt2.setYtelsestype("Arbeidsavklaringspenger");
        ytelseskontrakt2.setStatus("Lukket");
        leggTilVedtak2(ytelseskontrakt2);
        leggTilVedtak3(ytelseskontrakt2);
        ytelseskontraktListe.add(ytelseskontrakt2);


        return response;
    }

    private void leggTilVedtak1(WSYtelseskontrakt ytelseskontrakt) {
        final List<WSVedtak> vedtak = ytelseskontrakt.getIhtVedtak();
        final WSVedtak wsVedtak = new WSVedtak();
        wsVedtak.setPeriodetypeForYtelse("Ny rettighet");
        wsVedtak.setUttaksgrad(100);

        final WSPeriode wsPeriode = new WSPeriode();
        wsPeriode.setFom(CalendarConverter.convertDateToXMLGregorianCalendar(LocalDate.of(2016, 10, 18)));
        wsVedtak.setVedtaksperiode(wsPeriode);

        wsVedtak.setStatus("Innstilt");
        wsVedtak.setVedtakstype("Arbeidsavklaringspenger / Ny rettighet");
        wsVedtak.setAktivitetsfase("Under arbeidsavklaring");
        vedtak.add(wsVedtak);
    }

    private void leggTilVedtak2(WSYtelseskontrakt ytelseskontrakt) {
        final List<WSVedtak> vedtak = ytelseskontrakt.getIhtVedtak();
        final WSVedtak wsVedtak = new WSVedtak();
        wsVedtak.setBeslutningsdato(CalendarConverter.convertDateToXMLGregorianCalendar(LocalDate.of(2016, 4, 2)));
        wsVedtak.setPeriodetypeForYtelse("Stans");
        wsVedtak.setUttaksgrad(100);

        final WSPeriode wsPeriode = new WSPeriode();
        wsVedtak.setVedtaksperiode(wsPeriode);
        wsPeriode.setFom(CalendarConverter.convertDateToXMLGregorianCalendar(LocalDate.of(2016, 4, 1)));

        wsVedtak.setStatus("Iverksatt");
        wsVedtak.setVedtakstype("Ung ufør / Stans");
        wsVedtak.setAktivitetsfase("Ikke spesif. aktivitetsfase");
        vedtak.add(wsVedtak);
    }

    private void leggTilVedtak3(WSYtelseskontrakt ytelseskontrakt) {
        final List<WSVedtak> vedtak = ytelseskontrakt.getIhtVedtak();
        final WSVedtak wsVedtak = new WSVedtak();
        wsVedtak.setBeslutningsdato(CalendarConverter.convertDateToXMLGregorianCalendar(LocalDate.of(2016, 4, 2)));
        wsVedtak.setPeriodetypeForYtelse("Stans");
        wsVedtak.setUttaksgrad(100);
        wsVedtak.setVedtakBruttoBeloep(500000);
        wsVedtak.setVedtakNettoBeloep(500000);

        final WSPeriode wsPeriode = new WSPeriode();
        wsPeriode.setFom(CalendarConverter.convertDateToXMLGregorianCalendar(LocalDate.of(2016, 4, 1)));
        wsVedtak.setVedtaksperiode(wsPeriode);

        wsVedtak.setStatus("Iverksatt");
        wsVedtak.setVedtakstype("Arbeidsavklaringspenger / Stans");
        wsVedtak.setAktivitetsfase("Arbeidsutprøving");
        wsVedtak.setDagsats(1269);
        vedtak.add(wsVedtak);
    }
}
