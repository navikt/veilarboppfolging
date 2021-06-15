package no.nav.veilarboppfolging.controller;

import lombok.RequiredArgsConstructor;
import no.nav.common.types.identer.Fnr;
import no.nav.veilarboppfolging.client.ytelseskontrakt.YtelseskontraktClient;
import no.nav.veilarboppfolging.client.ytelseskontrakt.YtelseskontraktResponse;
import no.nav.veilarboppfolging.controller.response.YtelserResponse;
import no.nav.veilarboppfolging.service.AuthService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.xml.datatype.XMLGregorianCalendar;
import java.time.LocalDate;

import static no.nav.veilarboppfolging.utils.DateUtils.convertDateToXMLGregorianCalendar;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/person")
public class YtelseController {
    private static final int MANEDER_BAK_I_TID = 2;
    private static final int MANEDER_FREM_I_TID = 1;

    private final YtelseskontraktClient ytelseskontraktClient;
    private final AuthService authService;

    @GetMapping("/{fnr}/ytelser")
    public YtelserResponse getYtelser(@PathVariable("fnr") Fnr fnr) {
        authService.skalVereInternBruker();
        authService.sjekkLesetilgangMedFnr(fnr);

        LocalDate periodeFom = LocalDate.now().minusMonths(MANEDER_BAK_I_TID);
        LocalDate periodeTom = LocalDate.now().plusMonths(MANEDER_FREM_I_TID);
        XMLGregorianCalendar fom = convertDateToXMLGregorianCalendar(periodeFom);
        XMLGregorianCalendar tom = convertDateToXMLGregorianCalendar(periodeTom);

        final YtelseskontraktResponse ytelseskontraktResponse = ytelseskontraktClient.hentYtelseskontraktListe(fom, tom, fnr);

        return new YtelserResponse()
                .withVedtaksliste(ytelseskontraktResponse.getVedtaksliste())
                .withYtelser(ytelseskontraktResponse.getYtelser());
    }
}
