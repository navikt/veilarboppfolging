package no.nav.fo.veilarbsituasjon.rest.domain;

import no.nav.tjeneste.virksomhet.ytelseskontrakt.v3.informasjon.ytelseskontrakt.*;
import no.nav.tjeneste.virksomhet.ytelseskontrakt.v3.meldinger.WSHentYtelseskontraktListeResponse;

import javax.xml.datatype.XMLGregorianCalendar;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;


public class YtelseskontraktMapper {

    public static YtelseskontraktResponse mapWsResponseToResponse(WSHentYtelseskontraktListeResponse response) {

        final List<Vedtak> vedtakList = mapVedtak(response);
        final List<Ytelseskontrakt> ytelser = mapYtelser(response);

        return new YtelseskontraktResponse(vedtakList, ytelser);
    }

    private static List<Ytelseskontrakt> mapYtelser(WSHentYtelseskontraktListeResponse response) {
        return response.getYtelseskontraktListe().stream()
                .map(wsYtelseskontraktToYtelseskontrakt)
                .collect(Collectors.toList());
    }

    private static List<Vedtak> mapVedtak(WSHentYtelseskontraktListeResponse response) {
        final List<WSVedtak> wsVedtakList = response.getYtelseskontraktListe().stream()
                .map(WSYtelseskontrakt::getIhtVedtak)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());

        final List<Vedtak> vedtakList = wsVedtakList.stream().map(wsVedtakToVedtak)
                .collect(Collectors.toList());

        setRettighetsgruppePaVedtak(response, vedtakList);
        return vedtakList;
    }

    private static void setRettighetsgruppePaVedtak(WSHentYtelseskontraktListeResponse response, List<Vedtak> vedtakList) {
        final String rettighetsgruppe = getRettighetsgruppe(response);

        vedtakList.forEach(vedtak -> vedtak.setRettighetsgruppe(rettighetsgruppe));
    }

    private static String getRettighetsgruppe(WSHentYtelseskontraktListeResponse response) {
        return Optional.of(response)
                .map(WSHentYtelseskontraktListeResponse::getBruker)
                .map(WSBruker::getRettighetsgruppe)
                .map(WSRettighetsgruppe::getRettighetsGruppe).orElse("");
    }

    private static Function<WSVedtak, Vedtak> wsVedtakToVedtak = wsVedtak -> new Vedtak()
            .withVedtakstype(wsVedtak.getVedtakstype())
            .withStatus(wsVedtak.getStatus())
            .withAktivitetsfase(wsVedtak.getAktivitetsfase());

    private static Function<WSYtelseskontrakt, Ytelseskontrakt> wsYtelseskontraktToYtelseskontrakt = wsYtelseskontrakt -> {
        final Optional<XMLGregorianCalendar> fomGyldighetsperiode = Optional.ofNullable(wsYtelseskontrakt.getFomGyldighetsperiode());
        final Optional<XMLGregorianCalendar> tomGyldighetsperiode = Optional.ofNullable(wsYtelseskontrakt.getTomGyldighetsperiode());

        final Ytelseskontrakt ytelseskontrakt = new Ytelseskontrakt()
                .withYtelsestype(wsYtelseskontrakt.getYtelsestype())
                .withStatus(wsYtelseskontrakt.getStatus())
                .withDatoMottat(wsYtelseskontrakt.getDatoKravMottatt());

        fomGyldighetsperiode.ifPresent(ytelseskontrakt::setDatoFra);
        tomGyldighetsperiode.ifPresent(ytelseskontrakt::setDatoTil);

        return ytelseskontrakt;
    };
}
