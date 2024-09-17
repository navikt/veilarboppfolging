package no.nav.veilarboppfolging.service;

import lombok.extern.slf4j.Slf4j;
import no.nav.common.client.aktoroppslag.AktorOppslagClient;
import no.nav.common.client.norg2.Norg2Client;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto_schema.enums.arena.Formidlingsgruppe;
import no.nav.pto_schema.enums.arena.Kvalifiseringsgruppe;
import no.nav.veilarboppfolging.NotFoundException;
import no.nav.veilarboppfolging.client.veilarbarena.ArenaOppfolgingTilstand;
import no.nav.veilarboppfolging.client.veilarbarena.VeilarbArenaOppfolging;
import no.nav.veilarboppfolging.client.veilarbarena.VeilarbarenaClient;
import no.nav.veilarboppfolging.controller.response.OppfolgingEnhetMedVeilederResponse;
import no.nav.veilarboppfolging.repository.OppfolgingsStatusRepository;
import no.nav.veilarboppfolging.repository.VeilederTilordningerRepository;
import no.nav.veilarboppfolging.repository.entity.OppfolgingEntity;
import no.nav.veilarboppfolging.utils.ArenaUtils;
import no.nav.veilarboppfolging.utils.EnumUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

import static no.nav.veilarboppfolging.utils.SecureLog.secureLog;

@Slf4j
@Service
public class ArenaOppfolgingService {

    // Bruker AktorregisterClient istedenfor authService for å unngå sirkulær avhengighet
    private final AktorOppslagClient aktorOppslagClient;

    private final VeilarbarenaClient veilarbarenaClient;

    private final OppfolgingsStatusRepository oppfolgingsStatusRepository;

    private final AuthService authService;

    private final Norg2Client norg2Client;

    private final VeilederTilordningerRepository veilederTilordningerRepository;

    @Autowired
    public ArenaOppfolgingService(
            AktorOppslagClient aktorOppslagClient,
            VeilarbarenaClient veilarbarenaClient,
            OppfolgingsStatusRepository oppfolgingsStatusRepository,
            AuthService authService,
            Norg2Client norg2Client,
            VeilederTilordningerRepository veilederTilordningerRepository
    ) {
        this.aktorOppslagClient = aktorOppslagClient;
        this.veilarbarenaClient = veilarbarenaClient;
        this.oppfolgingsStatusRepository = oppfolgingsStatusRepository;
        this.authService = authService;
        this.norg2Client = norg2Client;
        this.veilederTilordningerRepository = veilederTilordningerRepository;
    }

    // Bruker endepunktet i veilarbarena som henter fra database som er synket med Arena (har et delay på et par min)
    public Optional<VeilarbArenaOppfolging> hentOppfolgingFraVeilarbarena(Fnr fnr) {
        return veilarbarenaClient.hentOppfolgingsbruker(fnr);
    }

    // Bruker endepunktet i veilarbarena som henter direkte fra Arena
    public Optional<ArenaOppfolgingTilstand> hentOppfolgingTilstandDirekteFraArena(Fnr fnr) {
        return veilarbarenaClient.getArenaOppfolgingsstatus(fnr).map(ArenaOppfolgingTilstand::fraArenaOppfolging);
    }

    public Optional<ArenaOppfolgingTilstand> hentOppfolgingTilstand(Fnr fnr) {
        AktorId aktorId = aktorOppslagClient.hentAktorId(fnr);

        Optional<ArenaOppfolgingTilstand> maybeArenaOppfolging = veilarbarenaClient.hentOppfolgingsbruker(fnr)
                .map(ArenaOppfolgingTilstand::fraArenaBruker);

        // TODO: Kan hente erUnderOppfolging gjennom OppfolgingService
        Optional<OppfolgingEntity> oppfolging = oppfolgingsStatusRepository.hentOppfolging(aktorId);
        boolean erUnderOppfolging = oppfolging.map(OppfolgingEntity::isUnderOppfolging).orElse(false);

        boolean erUnderOppfolgingIVeilarbarena = maybeArenaOppfolging
                .map(o -> ArenaUtils.erUnderOppfolging(EnumUtils.valueOf(Formidlingsgruppe.class, o.getFormidlingsgruppe()) , EnumUtils.valueOf(Kvalifiseringsgruppe.class, o.getServicegruppe())))
                .orElse(false);

        if (erUnderOppfolgingIVeilarbarena != erUnderOppfolging) {
            var oppfolgingTilstand = hentOppfolgingTilstandDirekteFraArena(fnr);

            maybeArenaOppfolging.ifPresent(arenaOppfolging -> {
                log.info(
                        "Differanse mellom oppfolging fra veilarbarena og direkte fra Arena."
                                + " veilarbarena.formidlingsgruppe={} veilarbarena.servicegruppe={}"
                                + " arena.formidlingsgruppe={} arena.servicegruppe={}",
                        arenaOppfolging.getFormidlingsgruppe(),
                        arenaOppfolging.getServicegruppe(),
                        oppfolgingTilstand.map(ArenaOppfolgingTilstand::getFormidlingsgruppe).orElse(null),
                        oppfolgingTilstand.map(ArenaOppfolgingTilstand::getServicegruppe).orElse(null)
                );
            });

            return oppfolgingTilstand;
        }

        return maybeArenaOppfolging;
    }

    public OppfolgingEnhetMedVeilederResponse getOppfolginsstatus(Fnr fnr) {

        VeilarbArenaOppfolging veilarbArenaOppfolging = veilarbarenaClient.hentOppfolgingsbruker(fnr)
                .orElseThrow(() -> new NotFoundException("Bruker ikke funnet"));

        OppfolgingEnhetMedVeilederResponse oppfolgingEnhetMedVeileder = new OppfolgingEnhetMedVeilederResponse()
                .setServicegruppe(veilarbArenaOppfolging.getKvalifiseringsgruppekode())
                .setFormidlingsgruppe(veilarbArenaOppfolging.getFormidlingsgruppekode())
                .setOppfolgingsenhet(hentEnhet(veilarbArenaOppfolging.getNav_kontor()))
                .setHovedmaalkode(veilarbArenaOppfolging.getHovedmaalkode());

        if (authService.erInternBruker()) {
            AktorId brukersAktoerId = authService.getAktorIdOrThrow(fnr);
            secureLog.info("Henter tilordning for bruker med aktørId {}", brukersAktoerId);
            String veilederIdent = veilederTilordningerRepository.hentTilordningForAktoer(brukersAktoerId);
            oppfolgingEnhetMedVeileder.setVeilederId(veilederIdent);
        }

        return oppfolgingEnhetMedVeileder;
    }

    private OppfolgingEnhetMedVeilederResponse.Oppfolgingsenhet hentEnhet(String enhetId) {
        String enhetNavn = "";

        try {
            enhetNavn = norg2Client.hentEnhet(enhetId).getNavn();
        } catch (Exception e) {
            log.warn("Fant ikke navn på enhet", e);
        }

        return new OppfolgingEnhetMedVeilederResponse.Oppfolgingsenhet(enhetNavn, enhetId);
    }


}
