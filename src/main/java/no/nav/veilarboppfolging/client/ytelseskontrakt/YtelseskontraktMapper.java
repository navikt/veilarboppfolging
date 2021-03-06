package no.nav.veilarboppfolging.client.ytelseskontrakt;

import no.nav.tjeneste.virksomhet.ytelseskontrakt.v3.informasjon.ytelseskontrakt.WSBruker;
import no.nav.tjeneste.virksomhet.ytelseskontrakt.v3.informasjon.ytelseskontrakt.WSRettighetsgruppe;
import no.nav.tjeneste.virksomhet.ytelseskontrakt.v3.informasjon.ytelseskontrakt.WSVedtak;
import no.nav.tjeneste.virksomhet.ytelseskontrakt.v3.informasjon.ytelseskontrakt.WSYtelseskontrakt;
import no.nav.tjeneste.virksomhet.ytelseskontrakt.v3.meldinger.WSHentYtelseskontraktListeResponse;

import javax.xml.datatype.XMLGregorianCalendar;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public class YtelseskontraktMapper {

    public static YtelseskontraktResponse tilYtelseskontrakt(WSHentYtelseskontraktListeResponse response) {
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

        final List<Vedtak> vedtakList = wsVedtakList.stream()
                .map(wsVedtakToVedtak)
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

    private static final Function<WSVedtak, Vedtak> wsVedtakToVedtak = wsVedtak -> {
        final Optional<XMLGregorianCalendar> fomdato = Optional.ofNullable(wsVedtak.getVedtaksperiode().getFom());
        final Optional<XMLGregorianCalendar> tomdato = Optional.ofNullable(wsVedtak.getVedtaksperiode().getTom());

        final Vedtak ytelse = new Vedtak()
                .setVedtakstype(wsVedtak.getVedtakstype())
                .setStatus(wsVedtak.getStatus())
                .setAktivitetsfase(wsVedtak.getAktivitetsfase());

        fomdato.ifPresent((fd) -> ytelse.setFradato(new Dato(fd.getYear(), fd.getMonth(), fd.getDay())));
        tomdato.ifPresent((td) -> ytelse.setTildato(new Dato(td.getYear(), td.getMonth(), td.getDay())));

        return ytelse;
    };

    private static final Function<WSYtelseskontrakt, Ytelseskontrakt> wsYtelseskontraktToYtelseskontrakt = wsYtelseskontrakt -> {
        final Optional<XMLGregorianCalendar> fomGyldighetsperiode = Optional.ofNullable(wsYtelseskontrakt.getFomGyldighetsperiode());
        final Optional<XMLGregorianCalendar> tomGyldighetsperiode = Optional.ofNullable(wsYtelseskontrakt.getTomGyldighetsperiode());

        final Ytelseskontrakt ytelseskontrakt = new Ytelseskontrakt()
                .withYtelsestype(wsYtelseskontrakt.getYtelsestype())
                .withStatus(wsYtelseskontrakt.getStatus())
                .withDatoMottatt(wsYtelseskontrakt.getDatoKravMottatt());

        fomGyldighetsperiode.ifPresent(ytelseskontrakt::setDatoFra);
        tomGyldighetsperiode.ifPresent(ytelseskontrakt::setDatoTil);

        return ytelseskontrakt;
    };
}
