package no.nav.veilarboppfolging.controller;

import no.nav.veilarboppfolging.client.oppfolging.OppfolgingClient;
import no.nav.veilarboppfolging.client.oppfolging.OppfolgingskontraktData;
import no.nav.veilarboppfolging.client.ytelseskontrakt.YtelseskontraktClient;
import no.nav.veilarboppfolging.services.AuthService;
import no.nav.veilarboppfolging.controller.domain.YtelserResponse;
import no.nav.veilarboppfolging.client.ytelseskontrakt.YtelseskontraktResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.xml.datatype.XMLGregorianCalendar;
import java.time.LocalDate;
import java.util.List;

import static no.nav.veilarboppfolging.utils.DateUtils.convertDateToXMLGregorianCalendar;

@RestController
@RequestMapping("/api/person")
public class YtelseController {
    private static final int MANEDER_BAK_I_TID = 2;
    private static final int MANEDER_FREM_I_TID = 1;

    private final OppfolgingClient oppfolgingClient;
    private final YtelseskontraktClient ytelseskontraktClient;
    private final AuthService authService;

    @Autowired
    public YtelseController(
            OppfolgingClient oppfolgingClient,
            YtelseskontraktClient ytelseskontraktClient,
            AuthService authService
    ) {
        this.oppfolgingClient = oppfolgingClient;
        this.ytelseskontraktClient = ytelseskontraktClient;
        this.authService = authService;
    }

    @GetMapping("/{fnr}/ytelser")
    public YtelserResponse getYtelser(@PathVariable("fnr") String fnr) {
        authService.skalVereInternBruker();
        authService.sjekkLesetilgangMedFnr(fnr);

        LocalDate periodeFom = LocalDate.now().minusMonths(MANEDER_BAK_I_TID);
        LocalDate periodeTom = LocalDate.now().plusMonths(MANEDER_FREM_I_TID);
        XMLGregorianCalendar fom = convertDateToXMLGregorianCalendar(periodeFom);
        XMLGregorianCalendar tom = convertDateToXMLGregorianCalendar(periodeTom);

        final YtelseskontraktResponse ytelseskontraktResponse = ytelseskontraktClient.hentYtelseskontraktListe(fom, tom, fnr);
        final List<OppfolgingskontraktData> kontrakter = oppfolgingClient.hentOppfolgingskontraktListe(fom, tom, fnr);

        return new YtelserResponse()
                .withVedtaksliste(ytelseskontraktResponse.getVedtaksliste())
                .withYtelser(ytelseskontraktResponse.getYtelser())
                .withOppfoelgingskontrakter(kontrakter);
    }
}
