package no.nav.veilarboppfolging.mock;

import no.nav.tjeneste.virksomhet.ytelseskontrakt.v3.HentYtelseskontraktListeSikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.ytelseskontrakt.v3.YtelseskontraktV3;
import no.nav.tjeneste.virksomhet.ytelseskontrakt.v3.informasjon.ytelseskontrakt.*;
import no.nav.tjeneste.virksomhet.ytelseskontrakt.v3.meldinger.HentYtelseskontraktListeRequest;
import no.nav.tjeneste.virksomhet.ytelseskontrakt.v3.meldinger.HentYtelseskontraktListeResponse;
import no.nav.veilarboppfolging.utils.DateUtils;

import javax.xml.datatype.XMLGregorianCalendar;
import java.time.LocalDate;
import java.util.List;


public class YtelseskontraktV3Mock implements YtelseskontraktV3 {

    public static final String RETTIGHETSGRUPPE = "Dagpenger";
    private static final int FAGSYSTEM_SAK_ID_1 = 111111111;
    public static final String YT_STATUS_1 = "Aktiv";
    public static final String YTELSESTYPE_1 = "Arbeidsavklaringspenger";
    public static final XMLGregorianCalendar YT_FOM_GYLDIGHETSPERIODE_1 = DateUtils.convertDateToXMLGregorianCalendar(LocalDate.of(2016, 10, 18));
    public static final XMLGregorianCalendar YT_DATO_KRAV_MOTTATT_1 = DateUtils.convertDateToXMLGregorianCalendar(LocalDate.of(2016, 10, 19));
    public static final XMLGregorianCalendar YT_FOM_GYLDIGHETSPERIODE_2 = DateUtils.convertDateToXMLGregorianCalendar(LocalDate.of(2016, 4, 1));
    public static final XMLGregorianCalendar YT_TOM_GYLDIGHETSPERIODE_2 = DateUtils.convertDateToXMLGregorianCalendar(LocalDate.of(2016, 8, 29));
    public static final XMLGregorianCalendar YT_DATO_KRAV_MOTTATT_2 = DateUtils.convertDateToXMLGregorianCalendar(LocalDate.of(2016, 4, 1));
    private static final int FAGSYSTEM_SAK_ID_2 = 222222222;

    public static final String YTELSESTYPE_2 = "Arbeidsavklaringspenger";
    public static final String YT_STATUS_2 = "Lukket";
    public static final String VED_STATUS_1 = "Innstilt";
    public static final String VEDTAKSTYPE_1 = "Arbeidsavklaringspenger / Ny rettighet";
    public static final String AKTIVITETSFASE_1 = "Under arbeidsavklaring";
    public static final String VED_STATUS_2 = "Iverksatt";
    public static final String VEDTAKSTYPE_2 = "Ung ufør / Stans";
    public static final String AKTIVITETSFASE_2 = "Ikke spesif. aktivitetsfase";
    public static final String VED_STATUS_3 = "Iverksatt";
    public static final String VEDTAKSTYPE_3 = "Arbeidsavklaringspenger / Stans";
    public static final String AKTIVITETSFASE_3 = "Arbeidsutprøving";

    @Override
    public void ping() {

    }

    @Override
    public HentYtelseskontraktListeResponse hentYtelseskontraktListe(HentYtelseskontraktListeRequest request) {
        HentYtelseskontraktListeResponse response = new HentYtelseskontraktListeResponse();
        final Bruker bruker = new Bruker();
        final Rettighetsgruppe rettighetsgruppe = new Rettighetsgruppe();
        rettighetsgruppe.setRettighetsGruppe(RETTIGHETSGRUPPE);
        bruker.setRettighetsgruppe(rettighetsgruppe);
        response.setBruker(bruker);

        final List<Ytelseskontrakt> ytelseskontraktListe = response.getYtelseskontraktListe();
        final Ytelseskontrakt ytelseskontrakt1 = new Ytelseskontrakt();
        ytelseskontrakt1.setFomGyldighetsperiode(YT_FOM_GYLDIGHETSPERIODE_1);
        ytelseskontrakt1.setDatoKravMottatt(YT_DATO_KRAV_MOTTATT_1);
        ytelseskontrakt1.setFagsystemSakId(FAGSYSTEM_SAK_ID_1);
        ytelseskontrakt1.setStatus(YT_STATUS_1);
        ytelseskontrakt1.setYtelsestype(YTELSESTYPE_1);
        leggTilVedtak1(ytelseskontrakt1);
        ytelseskontraktListe.add(ytelseskontrakt1);

        final Ytelseskontrakt ytelseskontrakt2 = new Ytelseskontrakt();
        ytelseskontrakt2.setFomGyldighetsperiode(YT_FOM_GYLDIGHETSPERIODE_2);
        ytelseskontrakt2.setTomGyldighetsperiode(YT_TOM_GYLDIGHETSPERIODE_2);
        ytelseskontrakt2.setDatoKravMottatt(YT_DATO_KRAV_MOTTATT_2);
        ytelseskontrakt2.setFagsystemSakId(FAGSYSTEM_SAK_ID_2);
        ytelseskontrakt2.setYtelsestype(YTELSESTYPE_2);
        ytelseskontrakt2.setStatus(YT_STATUS_2);
        leggTilVedtak2(ytelseskontrakt2);
        leggTilVedtak3(ytelseskontrakt2);
        ytelseskontraktListe.add(ytelseskontrakt2);


        return response;
    }

    private void leggTilVedtak1(Ytelseskontrakt ytelseskontrakt) {
        final List<Vedtak> vedtak = ytelseskontrakt.getIhtVedtak();
        final Vedtak wsVedtak = new Vedtak();
        wsVedtak.setPeriodetypeForYtelse("Ny rettighet");
        wsVedtak.setUttaksgrad(100);

        final Periode wsPeriode = new Periode();
        wsPeriode.setFom(DateUtils.convertDateToXMLGregorianCalendar(LocalDate.of(2016, 10, 18)));
        wsVedtak.setVedtaksperiode(wsPeriode);

        wsVedtak.setStatus(VED_STATUS_1);
        wsVedtak.setVedtakstype(VEDTAKSTYPE_1);
        wsVedtak.setAktivitetsfase(AKTIVITETSFASE_1);
        vedtak.add(wsVedtak);
    }

    private void leggTilVedtak2(Ytelseskontrakt ytelseskontrakt) {
        final List<Vedtak> vedtak = ytelseskontrakt.getIhtVedtak();
        final Vedtak wsVedtak = new Vedtak();
        wsVedtak.setBeslutningsdato(DateUtils.convertDateToXMLGregorianCalendar(LocalDate.of(2016, 4, 2)));
        wsVedtak.setPeriodetypeForYtelse("Stans");
        wsVedtak.setUttaksgrad(100);

        final Periode wsPeriode = new Periode();
        wsVedtak.setVedtaksperiode(wsPeriode);
        wsPeriode.setFom(DateUtils.convertDateToXMLGregorianCalendar(LocalDate.of(2016, 4, 1)));

        wsVedtak.setStatus(VED_STATUS_2);
        wsVedtak.setVedtakstype(VEDTAKSTYPE_2);
        wsVedtak.setAktivitetsfase(AKTIVITETSFASE_2);
        vedtak.add(wsVedtak);
    }

    private void leggTilVedtak3(Ytelseskontrakt ytelseskontrakt) {
        final List<Vedtak> vedtak = ytelseskontrakt.getIhtVedtak();
        final Vedtak wsVedtak = new Vedtak();
        wsVedtak.setBeslutningsdato(DateUtils.convertDateToXMLGregorianCalendar(LocalDate.of(2016, 4, 2)));
        wsVedtak.setPeriodetypeForYtelse("Stans");
        wsVedtak.setUttaksgrad(100);
        wsVedtak.setVedtakBruttoBeloep(500000);
        wsVedtak.setVedtakNettoBeloep(500000);

        final Periode wsPeriode = new Periode();
        wsPeriode.setFom(DateUtils.convertDateToXMLGregorianCalendar(LocalDate.of(2016, 4, 1)));
        wsVedtak.setVedtaksperiode(wsPeriode);

        wsVedtak.setStatus(VED_STATUS_3);
        wsVedtak.setVedtakstype(VEDTAKSTYPE_3);
        wsVedtak.setAktivitetsfase(AKTIVITETSFASE_3);
        wsVedtak.setDagsats(1269);
        vedtak.add(wsVedtak);
    }
}
