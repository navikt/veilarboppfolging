package no.nav.veilarboppfolging.service;

import no.nav.common.types.identer.Fnr;
import no.nav.veilarboppfolging.client.ytelseskontrakt.YtelseskontraktClient;
import no.nav.veilarboppfolging.client.ytelseskontrakt.YtelseskontraktResponse;
import org.springframework.stereotype.Service;

import javax.xml.datatype.XMLGregorianCalendar;
import java.time.LocalDate;

import static no.nav.veilarboppfolging.utils.DateUtils.convertDateToXMLGregorianCalendar;


@Service
public class YtelserOgAktiviteterService {

    private static final int MANEDER_BAK_I_TID = 2;

    private static final int MANEDER_FREM_I_TID = 1;

    private static final String AKTIV_YTELSE_STATUS = "Aktiv";

    private final YtelseskontraktClient ytelseskontraktClient;

    public YtelserOgAktiviteterService(YtelseskontraktClient ytelseskontraktClient) {
        this.ytelseskontraktClient = ytelseskontraktClient;
    }

    public boolean harPagaendeYtelse(Fnr fnr) {
        return ytelseskontraktClient.hentYtelseskontraktListe(fnr)
                .getYtelser()
                .stream()
                .anyMatch(ytelseskontrakt -> AKTIV_YTELSE_STATUS.equals(ytelseskontrakt.getStatus()));
    }

    public YtelseskontraktResponse hentYtelseskontrakt(Fnr fnr) {
        LocalDate periodeFom = LocalDate.now().minusMonths(MANEDER_BAK_I_TID);
        LocalDate periodeTom = LocalDate.now().plusMonths(MANEDER_FREM_I_TID);

        return hentYtelseskontrakt(fnr, periodeFom, periodeTom);
    }

    private YtelseskontraktResponse hentYtelseskontrakt(Fnr fnr, LocalDate fra, LocalDate til) {
        XMLGregorianCalendar fom = convertDateToXMLGregorianCalendar(fra);
        XMLGregorianCalendar tom = convertDateToXMLGregorianCalendar(til);
        return ytelseskontraktClient.hentYtelseskontraktListe(fom, tom, fnr);
    }

}
