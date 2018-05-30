package no.nav.fo.veilarboppfolging.rest;

import io.swagger.annotations.Api;
import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;
import no.nav.apiapp.security.PepClient;
import no.nav.fo.veilarboppfolging.domain.ArenaOppfolging;
import no.nav.fo.veilarboppfolging.domain.OppfolgingStatusData;
import no.nav.fo.veilarboppfolging.rest.domain.AktivStatus;
import no.nav.fo.veilarboppfolging.services.ArenaOppfolgingService;
import no.nav.fo.veilarboppfolging.services.OppfolgingService;
import org.springframework.stereotype.Component;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static no.nav.fo.veilarboppfolging.services.ArenaUtils.erUnderOppfolging;

@Component
@Api(value = "Aktivstatus")
@Path("/person/{fnr}")
@Produces(APPLICATION_JSON)
@Slf4j
public class AktivStatusRessurs {

    private final ArenaOppfolgingService arenaOppfolgingService;
    private final PepClient pepClient;
    private OppfolgingService oppfolgingService;

    public AktivStatusRessurs(
            ArenaOppfolgingService arenaOppfolgingService,
            PepClient pepClient,
            OppfolgingService oppfolgingService) {
        this.arenaOppfolgingService = arenaOppfolgingService;
        this.pepClient = pepClient;
        this.oppfolgingService = oppfolgingService;
    }

    @GET
    @Path("/aktivstatus")
    public AktivStatus getAggregertAktivStatus(@PathParam("fnr") String fnr) throws Exception {
        pepClient.sjekkLeseTilgangTilFnr(fnr);

        ArenaOppfolging arenaData =
                Try.of(() -> arenaOppfolgingService.hentArenaOppfolging(fnr))
                .onFailure((e) -> new ArenaOppfolging())
                .get();

        OppfolgingStatusData oppfolgingStatus = oppfolgingService.hentOppfolgingsStatus(fnr);
        boolean underOppfolgingIArena = erUnderOppfolging(arenaData.getFormidlingsgruppe(), arenaData.getServicegruppe(), arenaData.getHarMottaOppgaveIArena());

        AktivStatus aktivStatus = AktivStatus.builder()
                .aktiv(underOppfolgingIArena)
                .inaktiveringDato(arenaData.getInaktiveringsdato())
                .underOppfolging(oppfolgingStatus.isUnderOppfolging())
                .build();

        log.debug("Henter aggregert status fra Arena og Oppfolging for fnr " + fnr + " : " + aktivStatus);
        return aktivStatus;
    }

}
