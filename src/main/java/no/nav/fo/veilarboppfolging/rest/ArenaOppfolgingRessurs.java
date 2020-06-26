package no.nav.fo.veilarboppfolging.rest;


import io.swagger.annotations.Api;
import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;
import no.nav.apiapp.security.PepClient;
import no.nav.dialogarena.aktor.AktorService;
import no.nav.fo.veilarboppfolging.db.VeilederTilordningerRepository;
import no.nav.fo.veilarboppfolging.domain.AktorId;
import no.nav.fo.veilarboppfolging.domain.Oppfolgingsenhet;
import no.nav.fo.veilarboppfolging.mappers.VeilarbArenaOppfolging;
import no.nav.fo.veilarboppfolging.rest.domain.OppfolgingEnhetMedVeileder;
import no.nav.fo.veilarboppfolging.services.ArenaOppfolgingService;
import no.nav.fo.veilarboppfolging.services.OppfolgingsbrukerService;
import no.nav.fo.veilarboppfolging.services.OrganisasjonEnhetService;
import no.nav.sbl.dialogarena.common.abac.pep.exception.PepException;
import no.nav.sbl.featuretoggle.unleash.UnleashService;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import java.util.Optional;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static no.nav.fo.veilarboppfolging.utils.FnrUtils.getAktorIdOrElseThrow;

@Slf4j
@Component
@Api(value = "Oppfølging")
@Path("/person/{fnr}")
@Produces(APPLICATION_JSON)
public class ArenaOppfolgingRessurs {

    private final ArenaOppfolgingService arenaOppfolgingService;
    private final PepClient pepClient;
    private final OrganisasjonEnhetService organisasjonEnhetService;
    private final AktorService aktorService;
    private final VeilederTilordningerRepository veilederTilordningerRepository;
    private final OppfolgingsbrukerService oppfolgingsbrukerService;
    private final UnleashService unleash;

    public ArenaOppfolgingRessurs(
            ArenaOppfolgingService arenaOppfolgingService,
            PepClient pepClient,
            OrganisasjonEnhetService organisasjonEnhetService,
            AktorService aktorService,
            VeilederTilordningerRepository veilederTilordningerRepository,
            OppfolgingsbrukerService oppfolgingsbrukerService,
            UnleashService unleash
    ) {
        this.arenaOppfolgingService = arenaOppfolgingService;
        this.pepClient = pepClient;
        this.organisasjonEnhetService = organisasjonEnhetService;
        this.aktorService = aktorService;
        this.veilederTilordningerRepository = veilederTilordningerRepository;
        this.oppfolgingsbrukerService = oppfolgingsbrukerService;
        this.unleash = unleash;
    }

    /*
     API used by veilarbmaofs. Contains only the necessary information
     */
    @GET
    @Path("/oppfolgingsstatus")
    public OppfolgingEnhetMedVeileder getOppfolginsstatus(@PathParam("fnr") String fnr) throws PepException {

        AktorId aktorId = getAktorIdOrElseThrow(aktorService, fnr);

        pepClient.sjekkLesetilgangTilAktorId(aktorId.getAktorId());

        OppfolgingEnhetMedVeileder res;
        if(unleash.isEnabled("veilarboppfolging.oppfolgingsstatus.fra.veilarbarena")) {
            VeilarbArenaOppfolging veilarbArenaOppfolging = oppfolgingsbrukerService.hentOppfolgingsbruker(fnr).orElseThrow(() -> new NotFoundException("Bruker ikke funnet"));
            res = new OppfolgingEnhetMedVeileder()
                    .setServicegruppe(veilarbArenaOppfolging.getKvalifiseringsgruppekode())
                    .setFormidlingsgruppe(veilarbArenaOppfolging.getFormidlingsgruppekode())
                    .setOppfolgingsenhet(hentEnhet(veilarbArenaOppfolging.getNav_kontor()))
                    .setHovedmaalkode(veilarbArenaOppfolging.getHovedmaalkode());

        } else {
            no.nav.fo.veilarboppfolging.domain.ArenaOppfolging arenaData = arenaOppfolgingService.hentArenaOppfolging(fnr);
            Optional<VeilarbArenaOppfolging> oppfolgingsbrukerStatus = oppfolgingsbrukerService.hentOppfolgingsbruker(fnr);
            res = new OppfolgingEnhetMedVeileder()
                .setServicegruppe(arenaData.getServicegruppe())
                .setFormidlingsgruppe(arenaData.getFormidlingsgruppe())
                .setOppfolgingsenhet(hentEnhet(arenaData.getOppfolgingsenhet()))
                .setHovedmaalkode(oppfolgingsbrukerStatus.map(VeilarbArenaOppfolging::getHovedmaalkode).orElse(null));
        }

        if (AutorisasjonService.erInternBruker()) {
            String brukersAktoerId = aktorService.getAktorId(fnr)
                    .orElseThrow(() -> new IllegalArgumentException("Fant ikke aktør for fnr: " + fnr));

            log.info("Henter tilordning for bruker med aktørId {}", brukersAktoerId);
            String veilederIdent = veilederTilordningerRepository.hentTilordningForAktoer(brukersAktoerId);
            res.setVeilederId(veilederIdent);
        }
        return res;
    }

    private Oppfolgingsenhet hentEnhet(String oppfolgingsenhetId) {
        Optional<String> enhetNavn = Try.of(() -> organisasjonEnhetService.hentEnhet(oppfolgingsenhetId).getNavn()).toJavaOptional();
        return new Oppfolgingsenhet().withEnhetId(oppfolgingsenhetId).withNavn(enhetNavn.orElse(""));
    }

}
