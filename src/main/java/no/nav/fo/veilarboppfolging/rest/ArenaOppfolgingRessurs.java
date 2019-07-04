package no.nav.fo.veilarboppfolging.rest;


import io.swagger.annotations.Api;
import io.swagger.annotations.ApiParam;
import io.vavr.control.Try;
import no.nav.apiapp.feil.IngenTilgang;
import no.nav.apiapp.security.veilarbabac.Bruker;
import no.nav.apiapp.security.veilarbabac.VeilarbAbacPepClient;
import no.nav.dialogarena.aktor.AktorService;
import no.nav.fo.veilarboppfolging.db.VeilederTilordningerRepository;
import no.nav.fo.veilarboppfolging.domain.Oppfolgingsenhet;
import no.nav.fo.veilarboppfolging.domain.OppfolgingskontraktResponse;
import no.nav.fo.veilarboppfolging.mappers.VeilarbArenaOppfolging;
import no.nav.fo.veilarboppfolging.mappers.OppfolgingMapper;
import no.nav.fo.veilarboppfolging.rest.domain.*;
import no.nav.fo.veilarboppfolging.services.*;
import no.nav.sbl.dialogarena.common.abac.pep.exception.PepException;
import no.nav.sbl.featuretoggle.unleash.UnleashService;

import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.xml.datatype.XMLGregorianCalendar;
import java.time.LocalDate;
import java.util.Optional;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static no.nav.fo.veilarboppfolging.utils.CalendarConverter.convertDateToXMLGregorianCalendar;

@Component
@Api(value = "Oppfølging")
@Path("/person/{fnr}")
@Produces(APPLICATION_JSON)
public class ArenaOppfolgingRessurs {
    private static final int MANEDER_BAK_I_TID = 2;
    private static final int MANEDER_FREM_I_TID = 1;

    private final ArenaOppfolgingService arenaOppfolgingService;
    private final OppfolgingMapper oppfolgingMapper;
    private final VeilarbAbacPepClient pepClient;
    private final OrganisasjonEnhetService organisasjonEnhetService;
    private final AktorService aktorService;
    private final VeilederTilordningerRepository veilederTilordningerRepository;
    private final OppfolgingsbrukerService oppfolgingsbrukerService;
    private final UnleashService unleash;

    public ArenaOppfolgingRessurs(
            ArenaOppfolgingService arenaOppfolgingService,
            OppfolgingMapper oppfolgingMapper,
            VeilarbAbacPepClient pepClient,
            OrganisasjonEnhetService organisasjonEnhetService,
            AktorService aktorService,
            VeilederTilordningerRepository veilederTilordningerRepository,
            OppfolgingsbrukerService oppfolgingsbrukerService,
            UnleashService unleash
    ) {
        this.arenaOppfolgingService = arenaOppfolgingService;
        this.oppfolgingMapper = oppfolgingMapper;
        this.pepClient = pepClient;
        this.organisasjonEnhetService = organisasjonEnhetService;
        this.aktorService = aktorService;
        this.veilederTilordningerRepository = veilederTilordningerRepository;
        this.oppfolgingsbrukerService = oppfolgingsbrukerService;
        this.unleash = unleash;
    }

    @GET
    @Path("/oppfoelging")
    public OppfolgingskontraktResponse getOppfoelging(@PathParam("fnr") String fnr) throws PepException {
        Bruker bruker = Bruker.fraFnr(fnr)
                .medAktoerIdSupplier(() -> aktorService.getAktorId(fnr).orElseThrow(IngenTilgang::new));

        pepClient.sjekkLesetilgangTilBruker(bruker);
        LocalDate periodeFom = LocalDate.now().minusMonths(MANEDER_BAK_I_TID);
        LocalDate periodeTom = LocalDate.now().plusMonths(MANEDER_FREM_I_TID);
        XMLGregorianCalendar fom = convertDateToXMLGregorianCalendar(periodeFom);
        XMLGregorianCalendar tom = convertDateToXMLGregorianCalendar(periodeTom);

        return oppfolgingMapper.tilOppfolgingskontrakt(arenaOppfolgingService.hentOppfolgingskontraktListe(fom, tom, fnr));
    }

    @GET
    @Path("/oppfoelgingsstatus")
    @Deprecated
    public ArenaOppfolging getOppfoelginsstatus(@PathParam("fnr") String fnr) throws PepException {
        Bruker bruker = Bruker.fraFnr(fnr)
                .medAktoerIdSupplier(() -> aktorService.getAktorId(fnr).orElseThrow(IngenTilgang::new));

        pepClient.sjekkLesetilgangTilBruker(bruker);

        no.nav.fo.veilarboppfolging.domain.ArenaOppfolging arenaData = arenaOppfolgingService.hentArenaOppfolging(fnr);
        Oppfolgingsenhet enhet = hentEnhet(arenaData.getOppfolgingsenhet());

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
     API used by veilarbmaofs. Contains only the necessary information
     */
    @GET
    @Path("/oppfolgingsstatus")
    public OppfolgingEnhetMedVeileder getOppfolginsstatus(@PathParam("fnr") String fnr,
                                                          @ApiParam(value = "Deprecated og bør ikke settes. " +
                                                                  "Tilgjengelig pga. overgang til veilarbarena som har litt forsinkelse på data i Arena i motsetning til SOAP tjeneste.")
                                                          @QueryParam("brukArena") boolean brukArena) throws PepException {
        Bruker bruker = Bruker.fraFnr(fnr)
                .medAktoerIdSupplier(() -> aktorService.getAktorId(fnr).orElseThrow(IngenTilgang::new));

        pepClient.sjekkLesetilgangTilBruker(bruker);

        OppfolgingEnhetMedVeileder res;
        if(!brukArena && unleash.isEnabled("veilarboppfolging.oppfolgingsstatus.fra.veilarbarena")) {
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
