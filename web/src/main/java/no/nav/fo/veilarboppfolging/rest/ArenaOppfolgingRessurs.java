package no.nav.fo.veilarboppfolging.rest;


import io.swagger.annotations.Api;
import no.nav.apiapp.security.PepClient;
import no.nav.dialogarena.aktor.AktorService;
import no.nav.fo.veilarboppfolging.db.VeilederTilordningerRepository;
import no.nav.fo.veilarboppfolging.domain.Oppfolgingsenhet;
import no.nav.fo.veilarboppfolging.domain.OppfolgingskontraktResponse;
import no.nav.fo.veilarboppfolging.mappers.OppfolgingMapper;
import no.nav.fo.veilarboppfolging.rest.domain.ArenaOppfolging;
import no.nav.fo.veilarboppfolging.rest.domain.OppfolgingEnhetMedVeileder;
import no.nav.fo.veilarboppfolging.services.ArenaOppfolgingService;
import no.nav.fo.veilarboppfolging.services.OrganisasjonEnhetService;
import no.nav.sbl.dialogarena.common.abac.pep.exception.PepException;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.xml.datatype.XMLGregorianCalendar;
import java.time.LocalDate;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static no.nav.fo.veilarboppfolging.utils.CalendarConverter.convertDateToXMLGregorianCalendar;
import static org.slf4j.LoggerFactory.getLogger;

@Component
@Api(value = "Oppfølging")
@Path("/person/{fnr}")
@Produces(APPLICATION_JSON)
public class ArenaOppfolgingRessurs {
    private static final Logger LOG = getLogger(ArenaOppfolgingRessurs.class);
    private static final int MANEDER_BAK_I_TID = 2;
    private static final int MANEDER_FREM_I_TID = 1;

    private final ArenaOppfolgingService arenaOppfolgingService;
    private final OppfolgingMapper oppfolgingMapper;
    private final PepClient pepClient;
    private final OrganisasjonEnhetService organisasjonEnhetService;
    private AktorService aktorService;
    private VeilederTilordningerRepository veilederTilordningerRepository;

    public ArenaOppfolgingRessurs(
            ArenaOppfolgingService arenaOppfolgingService,
            OppfolgingMapper oppfolgingMapper,
            PepClient pepClient,
            OrganisasjonEnhetService organisasjonEnhetService,
            AktorService aktorService,
            VeilederTilordningerRepository veilederTilordningerRepository
    ) {
        this.arenaOppfolgingService = arenaOppfolgingService;
        this.oppfolgingMapper = oppfolgingMapper;
        this.pepClient = pepClient;
        this.organisasjonEnhetService = organisasjonEnhetService;
        this.aktorService = aktorService;
        this.veilederTilordningerRepository = veilederTilordningerRepository;
    }

    @GET
    @Path("/oppfoelging")
    public OppfolgingskontraktResponse getOppfoelging(@PathParam("fnr") String fnr) throws PepException {
        pepClient.sjekkTilgangTilFnr(fnr);
        LocalDate periodeFom = LocalDate.now().minusMonths(MANEDER_BAK_I_TID);
        LocalDate periodeTom = LocalDate.now().plusMonths(MANEDER_FREM_I_TID);
        XMLGregorianCalendar fom = convertDateToXMLGregorianCalendar(periodeFom);
        XMLGregorianCalendar tom = convertDateToXMLGregorianCalendar(periodeTom);

        LOG.info("Henter oppfoelging for fnr");
        return oppfolgingMapper.tilOppfolgingskontrakt(arenaOppfolgingService.hentOppfolgingskontraktListe(fom, tom, fnr));
    }

    @GET
    @Path("/oppfoelgingsstatus")
    @Deprecated
    public ArenaOppfolging getOppfoelginsstatus(@PathParam("fnr") String fnr) throws PepException {
        pepClient.sjekkTilgangTilFnr(fnr);

        LOG.info("Henter oppfølgingsstatus for fnr");
        no.nav.fo.veilarboppfolging.domain.ArenaOppfolging arenaData = arenaOppfolgingService.hentArenaOppfolging(fnr);
        Oppfolgingsenhet enhet = organisasjonEnhetService.hentEnhet(arenaData.getOppfolgingsenhet());

        return toRestDto(arenaData, enhet);
    }

    private ArenaOppfolging toRestDto(no.nav.fo.veilarboppfolging.domain.ArenaOppfolging hentArenaOppfolging, Oppfolgingsenhet enhet) {
        Oppfolgingsenhet oppfolgingsenhet = new Oppfolgingsenhet().withEnhetId(enhet.getEnhetId()).withNavn(enhet.getNavn());

        return new ArenaOppfolging()
                .setFormidlingsgruppe(hentArenaOppfolging.getFormidlingsgruppe())
                .setInaktiveringsdato(hentArenaOppfolging.getInaktiveringsdato())
                .setOppfolgingsenhet(oppfolgingsenhet)
                .setRettighetsgruppe(hentArenaOppfolging.getRettighetsgruppe())
                .setServicegruppe(hentArenaOppfolging.getServicegruppe());
    }

    /*
     API used by veilarbmaofs. Contains only the neccasary information
     */
    @GET
    @Path("/oppfolgingsstatus")
    public OppfolgingEnhetMedVeileder getOppfolginsstatus(@PathParam("fnr") String fnr) throws PepException {
        pepClient.sjekkLeseTilgangTilFnr(fnr);

        LOG.info("Henter oppfølgingsstatus for fnr");
        no.nav.fo.veilarboppfolging.domain.ArenaOppfolging arenaData = arenaOppfolgingService.hentArenaOppfolging(fnr);
        Oppfolgingsenhet enhet = organisasjonEnhetService.hentEnhet(arenaData.getOppfolgingsenhet());

        Oppfolgingsenhet oppfolgingsenhet = new Oppfolgingsenhet().withEnhetId(enhet.getEnhetId()).withNavn(enhet.getNavn());

        String brukersAktoerId = aktorService.getAktorId(fnr)
                .orElseThrow(() -> new IllegalArgumentException("Fant ikke aktør for fnr: " + fnr));
        String veilederIdent = veilederTilordningerRepository.hentTilordningForAktoer(brukersAktoerId);

        return new OppfolgingEnhetMedVeileder()
                .setOppfolgingsenhet(oppfolgingsenhet)
                .setVeilederId(veilederIdent)
                .setFormidlingsgruppe(arenaData.getFormidlingsgruppe())
                .setServicegruppe(arenaData.getServicegruppe());
    }
}
