package no.nav.veilarboppfolging.controller;

import lombok.extern.slf4j.Slf4j;
import no.nav.common.client.norg2.Norg2Client;
import no.nav.veilarboppfolging.client.veilarbarena.VeilarbArenaOppfolging;
import no.nav.veilarboppfolging.client.veilarbarena.VeilarbarenaClient;
import no.nav.veilarboppfolging.controller.domain.OppfolgingEnhetMedVeileder;
import no.nav.veilarboppfolging.domain.Oppfolgingsenhet;
import no.nav.veilarboppfolging.repository.VeilederTilordningerRepository;
import no.nav.veilarboppfolging.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@RestController
@RequestMapping("/api/person")
public class ArenaOppfolgingController {

    private final AuthService authService;

    private final Norg2Client norg2Client;

    private final VeilederTilordningerRepository veilederTilordningerRepository;

    private final VeilarbarenaClient veilarbarenaClient;

    @Autowired
    public ArenaOppfolgingController (
            AuthService authService,
            Norg2Client norg2Client,
            VeilederTilordningerRepository veilederTilordningerRepository,
            VeilarbarenaClient veilarbarenaClient
    ) {
        this.authService = authService;
        this.norg2Client = norg2Client;
        this.veilederTilordningerRepository = veilederTilordningerRepository;
        this.veilarbarenaClient = veilarbarenaClient;
    }

    /*
     API used by veilarbmaofs. Contains only the necessary information
     */
    @GetMapping("/{fnr}/oppfolgingsstatus")
    public OppfolgingEnhetMedVeileder getOppfolginsstatus(@PathVariable("fnr") String fnr) {

        authService.sjekkLesetilgangMedFnr(fnr);

        VeilarbArenaOppfolging veilarbArenaOppfolging = veilarbarenaClient.hentOppfolgingsbruker(fnr)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bruker ikke funnet"));

        OppfolgingEnhetMedVeileder oppfolgingEnhetMedVeileder = new OppfolgingEnhetMedVeileder()
                .setServicegruppe(veilarbArenaOppfolging.getKvalifiseringsgruppekode())
                .setFormidlingsgruppe(veilarbArenaOppfolging.getFormidlingsgruppekode())
                .setOppfolgingsenhet(hentEnhet(veilarbArenaOppfolging.getNav_kontor()))
                .setHovedmaalkode(veilarbArenaOppfolging.getHovedmaalkode());

        if (authService.erInternBruker()) {
            String brukersAktoerId = authService.getAktorIdOrThrow(fnr);
            log.info("Henter tilordning for bruker med aktørId {}", brukersAktoerId);
            String veilederIdent = veilederTilordningerRepository.hentTilordningForAktoer(brukersAktoerId);
            oppfolgingEnhetMedVeileder.setVeilederId(veilederIdent);
        }

        return oppfolgingEnhetMedVeileder;
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
