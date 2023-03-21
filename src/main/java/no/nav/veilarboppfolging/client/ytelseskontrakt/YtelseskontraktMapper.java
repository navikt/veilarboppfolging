package no.nav.veilarboppfolging.client.ytelseskontrakt;

import no.nav.tjeneste.virksomhet.ytelseskontrakt.v3.informasjon.ytelseskontrakt.Bruker;
import no.nav.tjeneste.virksomhet.ytelseskontrakt.v3.informasjon.ytelseskontrakt.Rettighetsgruppe;
import no.nav.tjeneste.virksomhet.ytelseskontrakt.v3.informasjon.ytelseskontrakt.Vedtak;
import no.nav.tjeneste.virksomhet.ytelseskontrakt.v3.informasjon.ytelseskontrakt.Ytelseskontrakt;
import no.nav.tjeneste.virksomhet.ytelseskontrakt.v3.meldinger.HentYtelseskontraktListeResponse;

import javax.xml.datatype.XMLGregorianCalendar;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public class YtelseskontraktMapper {

    public static YtelseskontraktResponse tilYtelseskontrakt(HentYtelseskontraktListeResponse response) {
        final List<VedtakDto> vedtakDtoList = mapVedtak(response);
        final List<YtelseskontraktDto> ytelser = mapYtelser(response);
        return new YtelseskontraktResponse(vedtakDtoList, ytelser);
    }

    private static List<YtelseskontraktDto> mapYtelser(HentYtelseskontraktListeResponse response) {
        return response.getYtelseskontraktListe().stream()
                .map(YtelseskontraktToYtelseskontrakt)
                .collect(Collectors.toList());
    }

    private static List<VedtakDto> mapVedtak(HentYtelseskontraktListeResponse response) {
        final List<Vedtak> VedtakList = response.getYtelseskontraktListe().stream()
                .map(Ytelseskontrakt::getIhtVedtak)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());

        final List<VedtakDto> vedtakDtoList = VedtakList.stream()
                .map(VedtakToVedtak)
                .collect(Collectors.toList());

        setRettighetsgruppePaVedtak(response, vedtakDtoList);
        return vedtakDtoList;
    }

    private static void setRettighetsgruppePaVedtak(HentYtelseskontraktListeResponse response, List<VedtakDto> vedtakDtoList) {
        final String rettighetsgruppe = getRettighetsgruppe(response);

        vedtakDtoList.forEach(vedtak -> vedtak.setRettighetsgruppe(rettighetsgruppe));
    }

    private static String getRettighetsgruppe(HentYtelseskontraktListeResponse response) {
        return Optional.of(response)
                .map(HentYtelseskontraktListeResponse::getBruker)
                .map(Bruker::getRettighetsgruppe)
                .map(Rettighetsgruppe::getRettighetsGruppe).orElse("");
    }

    private static final Function<Vedtak, VedtakDto> VedtakToVedtak = Vedtak -> {
        final Optional<XMLGregorianCalendar> fomdato = Optional.ofNullable(Vedtak.getVedtaksperiode().getFom());
        final Optional<XMLGregorianCalendar> tomdato = Optional.ofNullable(Vedtak.getVedtaksperiode().getTom());

        final VedtakDto ytelse = new VedtakDto()
                .setVedtakstype(Vedtak.getVedtakstype())
                .setStatus(Vedtak.getStatus())
                .setAktivitetsfase(Vedtak.getAktivitetsfase());

        fomdato.ifPresent((fd) -> ytelse.setFradato(new Dato(fd.getYear(), fd.getMonth(), fd.getDay())));
        tomdato.ifPresent((td) -> ytelse.setTildato(new Dato(td.getYear(), td.getMonth(), td.getDay())));

        return ytelse;
    };

    private static final Function<Ytelseskontrakt, YtelseskontraktDto> YtelseskontraktToYtelseskontrakt = Ytelseskontrakt -> {
        final Optional<XMLGregorianCalendar> fomGyldighetsperiode = Optional.ofNullable(Ytelseskontrakt.getFomGyldighetsperiode());
        final Optional<XMLGregorianCalendar> tomGyldighetsperiode = Optional.ofNullable(Ytelseskontrakt.getTomGyldighetsperiode());

        final YtelseskontraktDto ytelseskontraktDto = new YtelseskontraktDto()
                .withYtelsestype(Ytelseskontrakt.getYtelsestype())
                .withStatus(Ytelseskontrakt.getStatus())
                .withDatoMottatt(Ytelseskontrakt.getDatoKravMottatt());

        fomGyldighetsperiode.ifPresent(ytelseskontraktDto::setDatoFra);
        tomGyldighetsperiode.ifPresent(ytelseskontraktDto::setDatoTil);

        return ytelseskontraktDto;
    };
}
