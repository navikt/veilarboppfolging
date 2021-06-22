package no.nav.veilarboppfolging.service;

import no.nav.common.types.identer.Fnr;
import no.nav.veilarboppfolging.client.ytelseskontrakt.YtelseskontraktClient;
import org.springframework.stereotype.Service;


@Service
public class YtelserOgAktiviteterService {

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
}
