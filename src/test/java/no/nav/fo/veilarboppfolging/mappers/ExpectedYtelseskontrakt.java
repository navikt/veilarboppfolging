package no.nav.fo.veilarboppfolging.mappers;


import no.nav.fo.veilarboppfolging.rest.domain.Vedtak;
import no.nav.fo.veilarboppfolging.rest.domain.Ytelseskontrakt;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static no.nav.fo.veilarboppfolging.mock.YtelseskontraktV3Mock.*;

class ExpectedYtelseskontrakt {

    static List<Vedtak> getExpectedVedtakUtenRettighetsgruppe() {
        final List<Vedtak> expectedVedtak = getExpectedVedtak();
        return expectedVedtak.stream().map(vedtak -> vedtak.withRettighetsgruppe(null)).collect(Collectors.toList());
    }

    static List<Vedtak> getExpectedVedtakUtenVedtaksgruppe() {
        final List<Vedtak> expectedVedtak = getExpectedVedtak();
        return expectedVedtak.stream().map(vedtak -> vedtak.withVedtakstype(null)).collect(Collectors.toList());
    }

    static List<Vedtak> getExpectedVedtakUtenAktivitetsfase() {
        final List<Vedtak> expectedVedtak = getExpectedVedtak();
        return expectedVedtak.stream().map(vedtak -> vedtak.withAktivitetsfase(null)).collect(Collectors.toList());
    }



    static List<Vedtak> getExpectedVedtak() {

        Vedtak vedtak1 = new Vedtak()
                .withAktivitetsfase(AKTIVITETSFASE_1)
                .withStatus(VED_STATUS_1)
                .withVedtakstype(VEDTAKSTYPE_1)
                .withRettighetsgruppe(RETTIGHETSGRUPPE);

        Vedtak vedtak2 = new Vedtak()
                .withAktivitetsfase(AKTIVITETSFASE_2)
                .withStatus(VED_STATUS_2)
                .withVedtakstype(VEDTAKSTYPE_2)
                .withRettighetsgruppe(RETTIGHETSGRUPPE);

        Vedtak vedtak3 = new Vedtak()
                .withAktivitetsfase(AKTIVITETSFASE_3)
                .withStatus(VED_STATUS_3)
                .withVedtakstype(VEDTAKSTYPE_3)
                .withRettighetsgruppe(RETTIGHETSGRUPPE);
        return asList(vedtak1, vedtak2, vedtak3);
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
