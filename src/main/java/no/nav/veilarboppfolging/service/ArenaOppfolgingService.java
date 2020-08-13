package no.nav.veilarboppfolging.service;

import no.nav.veilarboppfolging.client.veilarbarena.VeilarbArenaOppfolging;
import no.nav.veilarboppfolging.client.veilarbarena.VeilarbarenaClient;
import no.nav.veilarboppfolging.domain.ArenaOppfolgingTilstand;
import no.nav.veilarboppfolging.domain.OppfolgingTable;
import no.nav.veilarboppfolging.repository.OppfolgingsStatusRepository;
import no.nav.veilarboppfolging.utils.ArenaUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ArenaOppfolgingService {

    private final AuthService authService;

    private final VeilarbarenaClient veilarbarenaClient;

    private final OppfolgingsStatusRepository oppfolgingsStatusRepository;

    @Autowired
    public ArenaOppfolgingService(AuthService authService, VeilarbarenaClient veilarbarenaClient, OppfolgingsStatusRepository oppfolgingsStatusRepository) {
        this.authService = authService;
        this.veilarbarenaClient = veilarbarenaClient;
        this.oppfolgingsStatusRepository = oppfolgingsStatusRepository;
    }

    // Bruker endepunktet i veilarbarena som henter fra database som er synket med Arena (har et delay på et par min)
    public Optional<VeilarbArenaOppfolging> hentOppfolgingFraVeilarbarena(String fnr) {
        return veilarbarenaClient.hentOppfolgingsbruker(fnr);
    }

    // Bruker endepunktet i veilarbarena som henter direkte fra Arena
    public Optional<ArenaOppfolgingTilstand> hentOppfolgingTilstandDirekteFraArena(String fnr) {
        return veilarbarenaClient.getArenaOppfolgingsstatus(fnr).map(ArenaOppfolgingTilstand::fraArenaOppfolging);
    }

    public Optional<ArenaOppfolgingTilstand> hentOppfolgingTilstand(String fnr) {
        String aktorId = authService.getAktorIdOrThrow(fnr);

        Optional<ArenaOppfolgingTilstand> maybeArenaOppfolging = veilarbarenaClient.hentOppfolgingsbruker(fnr)
                .map(ArenaOppfolgingTilstand::fraArenaBruker);

        Optional<OppfolgingTable> oppfolging = Optional.ofNullable(oppfolgingsStatusRepository.fetch(aktorId));

        boolean erUnderOppfolging = oppfolging.map(OppfolgingTable::isUnderOppfolging).orElse(false);

        boolean erUnderOppfolgingIVeilarbarena = maybeArenaOppfolging
                .map(o ->  ArenaUtils.erUnderOppfolging(o.getFormidlingsgruppe(), o.getServicegruppe()))
                .orElse(false);

        if (erUnderOppfolgingIVeilarbarena != erUnderOppfolging) {
            return hentOppfolgingTilstandDirekteFraArena(fnr);
        }

        return maybeArenaOppfolging;
    }


}
