package no.nav.fo.veilarbsituasjon.rest.domain;

import no.nav.tjeneste.virksomhet.ytelseskontrakt.v3.informasjon.ytelseskontrakt.WSVedtak;
import no.nav.tjeneste.virksomhet.ytelseskontrakt.v3.informasjon.ytelseskontrakt.WSYtelseskontrakt;
import no.nav.tjeneste.virksomhet.ytelseskontrakt.v3.meldinger.WSHentYtelseskontraktListeResponse;

import javax.xml.datatype.XMLGregorianCalendar;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;


public class YtelseskontraktMapper {

    public static YtelseskontraktResponse mapWsResponseToResponse(WSHentYtelseskontraktListeResponse response) {
        final List<WSVedtak> wsVedtakList = response.getYtelseskontraktListe().stream()
                .map(WSYtelseskontrakt::getIhtVedtak)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());


        final String rettighetsGruppe = response.getBruker().getRettighetsgruppe().getRettighetsGruppe();

        final List<Vedtak> vedtakList = wsVedtakList.stream().map(wsVedtakToVedtak)
                .collect(Collectors.toList());
        vedtakList.forEach(vedtak -> vedtak.setRettighetsgruppe(rettighetsGruppe));

        final List<Ytelseskontrakt> ytelser = response.getYtelseskontraktListe().stream().map(wsYtelseskontraktToYtelseskontrakt).collect(Collectors.toList());

        return new YtelseskontraktResponse(vedtakList, ytelser);
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
