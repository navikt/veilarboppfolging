package no.nav.veilarboppfolging.client.ytelseskontrakt;


import javax.xml.datatype.XMLGregorianCalendar;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static no.nav.veilarboppfolging.mock.YtelseskontraktV3Mock.*;

public class ExpectedYtelseskontrakt {

    public static List<Vedtak> getExpectedVedtakUtenRettighetsgruppe() {
        final List<Vedtak> expectedVedtak = getExpectedVedtak();
        return expectedVedtak.stream().map(vedtak -> vedtak.setRettighetsgruppe(null)).collect(Collectors.toList());
    }

    public static List<Vedtak> getExpectedVedtakUtenVedtaksgruppe() {
        final List<Vedtak> expectedVedtak = getExpectedVedtak();
        return expectedVedtak.stream().map(vedtak -> vedtak.setVedtakstype(null)).collect(Collectors.toList());
    }

    public static List<Vedtak> getExpectedVedtakUtenAktivitetsfase() {
        final List<Vedtak> expectedVedtak = getExpectedVedtak();
        return expectedVedtak.stream().map(vedtak -> vedtak.setAktivitetsfase(null)).collect(Collectors.toList());
    }

    static List<Vedtak> getExpectedVedtak() {

        Vedtak vedtak1 = new Vedtak()
                .setAktivitetsfase(AKTIVITETSFASE_1)
                .setStatus(VED_STATUS_1)
                .setVedtakstype(VEDTAKSTYPE_1)
                .setRettighetsgruppe(RETTIGHETSGRUPPE)
                .setFradato(tilDato(YT_FOM_GYLDIGHETSPERIODE_1));

        Vedtak vedtak2 = new Vedtak()
                .setAktivitetsfase(AKTIVITETSFASE_2)
                .setStatus(VED_STATUS_2)
                .setVedtakstype(VEDTAKSTYPE_2)
                .setRettighetsgruppe(RETTIGHETSGRUPPE)
                .setFradato(tilDato(YT_FOM_GYLDIGHETSPERIODE_2));

        Vedtak vedtak3 = new Vedtak()
                .setAktivitetsfase(AKTIVITETSFASE_3)
                .setStatus(VED_STATUS_3)
                .setVedtakstype(VEDTAKSTYPE_3)
                .setRettighetsgruppe(RETTIGHETSGRUPPE)
                .setFradato(tilDato(YT_FOM_GYLDIGHETSPERIODE_2));

        return asList(vedtak1, vedtak2, vedtak3);
    }

    private static Dato tilDato(XMLGregorianCalendar dato) {
        return new Dato(dato.getYear(), dato.getMonth(), dato.getDay());
    }


    static List<Ytelseskontrakt> getExpectedYtelseskontrakter() {
        Ytelseskontrakt ytelseskontrakt1 = new Ytelseskontrakt()
                .withDatoMottatt(YT_DATO_KRAV_MOTTATT_1)
                .withStatus(YT_STATUS_1)
                .withYtelsestype(YTELSESTYPE_1)
                .withDatoFra(YT_FOM_GYLDIGHETSPERIODE_1);

        Ytelseskontrakt ytelseskontrakt2 = new Ytelseskontrakt()
                .withDatoMottatt(YT_DATO_KRAV_MOTTATT_2)
                .withStatus(YT_STATUS_2)
                .withYtelsestype(YTELSESTYPE_2)
                .withDatoFra(YT_FOM_GYLDIGHETSPERIODE_2)
                .withDatoTil(YT_TOM_GYLDIGHETSPERIODE_2);

        return new ArrayList<>(asList(ytelseskontrakt1, ytelseskontrakt2));
    }
}
