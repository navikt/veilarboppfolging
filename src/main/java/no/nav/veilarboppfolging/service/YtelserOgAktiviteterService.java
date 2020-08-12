package no.nav.veilarboppfolging.service;

import no.nav.veilarboppfolging.client.veilarbaktivitet.ArenaAktivitetDTO;
import no.nav.veilarboppfolging.client.veilarbaktivitet.VeilarbaktivitetClient;
import no.nav.veilarboppfolging.client.ytelseskontrakt.YtelseskontraktClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static no.nav.veilarboppfolging.domain.arena.AktivitetStatus.AVBRUTT;
import static no.nav.veilarboppfolging.domain.arena.AktivitetStatus.FULLFORT;

@Service
public class YtelserOgAktiviteterService {

    private static final String AKTIV_YTELSE_STATUS = "Aktiv";

    private final YtelseskontraktClient ytelseskontraktClient;

    private final VeilarbaktivitetClient veilarbaktivitetClient;

    @Autowired
    public YtelserOgAktiviteterService(YtelseskontraktClient ytelseskontraktClient, VeilarbaktivitetClient veilarbaktivitetClient) {
        this.ytelseskontraktClient = ytelseskontraktClient;
        this.veilarbaktivitetClient = veilarbaktivitetClient;
    }

    public boolean harPagaendeYtelse(String fnr) {
        return ytelseskontraktClient.hentYtelseskontraktListe(fnr)
                .getYtelser()
                .stream()
                .anyMatch(ytelseskontrakt -> AKTIV_YTELSE_STATUS.equals(ytelseskontrakt.getStatus()));
    }

    public boolean harAktiveTiltak(String fnr) {
        return veilarbaktivitetClient.hentArenaAktiviteter(fnr)
                .stream()
                .map(ArenaAktivitetDTO::getStatus)
                .anyMatch(status -> status != AVBRUTT && status != FULLFORT);
    }

}
