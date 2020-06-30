package no.nav.veilarboppfolging.services;

import no.nav.veilarboppfolging.domain.arena.ArenaAktivitetDTO;
import no.nav.sbl.dialogarena.restclient.RestClient;

import java.util.List;

public class VeilarbaktivtetService {

    private final RestClient restClient;

    public VeilarbaktivtetService(RestClient restClient) {
        this.restClient = restClient;
    }

    public List<ArenaAktivitetDTO> hentArenaAktiviteter(String fnr) {
        return restClient.request("/aktivitet/arena")
                .queryParam("fnr", fnr)
                .getList(ArenaAktivitetDTO.class);
    }

}
