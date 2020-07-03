package no.nav.veilarboppfolging.controller;

import lombok.extern.slf4j.Slf4j;
import no.nav.common.client.norg2.Norg2Client;
import no.nav.common.featuretoggle.UnleashService;
import no.nav.veilarboppfolging.client.veilarbarena.ArenaOppfolging;
import no.nav.veilarboppfolging.client.veilarbarena.VeilarbarenaClient;
import no.nav.veilarboppfolging.db.VeilederTilordningerRepository;
import no.nav.veilarboppfolging.domain.Oppfolgingsenhet;
import no.nav.veilarboppfolging.services.AuthService;
import no.nav.veilarboppfolging.client.veilarbarena.VeilarbArenaOppfolging;
import no.nav.veilarboppfolging.controller.domain.OppfolgingEnhetMedVeileder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.ws.rs.*;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/person")
public class ArenaOppfolgingController {

    private final AuthService authService;

    private final Norg2Client norg2Client;

    private final VeilederTilordningerRepository veilederTilordningerRepository;

    private final VeilarbarenaClient veilarbarenaClient;

    private final UnleashService unleash;

    @Autowired
    public ArenaOppfolgingController (
            AuthService authService,
            Norg2Client norg2Client,
            VeilederTilordningerRepository veilederTilordningerRepository,
            VeilarbarenaClient veilarbarenaClient,
            UnleashService unleash
    ) {
        this.authService = authService;
        this.norg2Client = norg2Client;
        this.veilederTilordningerRepository = veilederTilordningerRepository;
        this.veilarbarenaClient = veilarbarenaClient;
        this.unleash = unleash;
    }

    /*
     API used by veilarbmaofs. Contains only the necessary information
     */
    @GetMapping("/{fnr}/oppfolgingsstatus")
    public OppfolgingEnhetMedVeileder getOppfolginsstatus(@PathVariable("fnr") String fnr) {

        authService.sjekkLesetilgangMedFnr(fnr);

        OppfolgingEnhetMedVeileder res;
        if(unleash.isEnabled("veilarboppfolging.oppfolgingsstatus.fra.veilarbarena")) {
            VeilarbArenaOppfolging veilarbArenaOppfolging = veilarbarenaClient.hentOppfolgingsbruker(fnr).orElseThrow(() -> new NotFoundException("Bruker ikke funnet"));
            res = new OppfolgingEnhetMedVeileder()
                    .setServicegruppe(veilarbArenaOppfolging.getKvalifiseringsgruppekode())
                    .setFormidlingsgruppe(veilarbArenaOppfolging.getFormidlingsgruppekode())
                    .setOppfolgingsenhet(hentEnhet(veilarbArenaOppfolging.getNav_kontor()))
                    .setHovedmaalkode(veilarbArenaOppfolging.getHovedmaalkode());

        } else {
            ArenaOppfolging arenaData = veilarbarenaClient.getArenaOppfolgingsstatus(fnr);
            Optional<VeilarbArenaOppfolging> oppfolgingsbrukerStatus = veilarbarenaClient.hentOppfolgingsbruker(fnr);
            res = new OppfolgingEnhetMedVeileder()
                .setServicegruppe(arenaData.getServicegruppe())
                .setFormidlingsgruppe(arenaData.getFormidlingsgruppe())
                .setOppfolgingsenhet(hentEnhet(arenaData.getOppfolgingsenhet()))
                .setHovedmaalkode(oppfolgingsbrukerStatus.map(VeilarbArenaOppfolging::getHovedmaalkode).orElse(null));
        }

        if (authService.erInternBruker()) {
            String brukersAktoerId = authService.getAktorIdOrThrow(fnr);
            log.info("Henter tilordning for bruker med aktørId {}", brukersAktoerId);
            String veilederIdent = veilederTilordningerRepository.hentTilordningForAktoer(brukersAktoerId);
            res.setVeilederId(veilederIdent);
        }

        return res;
    }

    private Oppfolgingsenhet hentEnhet(String oppfolgingsenhetId) {
        Oppfolgingsenhet enhet = new Oppfolgingsenhet()
                .withEnhetId(oppfolgingsenhetId);

        try {
            return enhet.withNavn(norg2Client.hentEnhet(oppfolgingsenhetId).getNavn());
        } catch (Exception e) {
            log.warn("Fant ikke navn på enhet", e);
            return enhet.withNavn("");
        }
    }

}
