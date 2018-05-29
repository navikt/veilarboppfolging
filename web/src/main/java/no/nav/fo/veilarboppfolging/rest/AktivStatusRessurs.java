package no.nav.fo.veilarboppfolging.rest;

import io.swagger.annotations.Api;
import no.nav.apiapp.security.PepClient;
import no.nav.fo.veilarboppfolging.domain.ArenaOppfolging;
import no.nav.fo.veilarboppfolging.domain.OppfolgingStatusData;
import no.nav.fo.veilarboppfolging.rest.domain.AktivStatus;
import no.nav.fo.veilarboppfolging.services.ArenaOppfolgingService;
import no.nav.fo.veilarboppfolging.services.OppfolgingService;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static no.nav.fo.veilarboppfolging.services.ArenaUtils.erUnderOppfolging;
import static org.slf4j.LoggerFactory.getLogger;

@Component
@Api(value = "Aktivstatus")
@Path("/person/{fnr}")
@Produces(APPLICATION_JSON)
public class AktivStatusRessurs {

    private static final Logger LOG = getLogger(AktivStatusRessurs.class);

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

        LOG.info("Henter aggregert status fra Arena og Oppfølging for fnr");
        ArenaOppfolging arenaData = arenaOppfolgingService.hentArenaOppfolging(fnr);
        OppfolgingStatusData oppfolgingStatus = oppfolgingService.hentOppfolgingsStatus(fnr);
        boolean underOppfolgingIArena = erUnderOppfolging(arenaData.getFormidlingsgruppe(), arenaData.getServicegruppe(), arenaData.getHarMottaOppgaveIArena());

        return AktivStatus.builder()
                .aktiv(underOppfolgingIArena)
                .inaktiveringDato(arenaData.getInaktiveringsdato())
                .underOppfolging(oppfolgingStatus.isUnderOppfolging())
                .build();
    }

}
