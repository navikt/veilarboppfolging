package no.nav.fo.veilarbsituasjon.ws;


import io.swagger.annotations.Api;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.val;
import no.nav.fo.veilarbsituasjon.db.SituasjonRepository;
import no.nav.fo.veilarbsituasjon.domain.*;
import no.nav.fo.veilarbsituasjon.services.AktoerIdService;
import no.nav.fo.veilarbsituasjon.services.PepClient;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.DigitalKontaktinformasjonV1;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.informasjon.WSKontaktinformasjon;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.meldinger.WSHentDigitalKontaktinformasjonRequest;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.meldinger.WSHentDigitalKontaktinformasjonResponse;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.OppfoelgingPortType;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.meldinger.WSHentOppfoelgingsstatusRequest;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.meldinger.WSHentOppfoelgingsstatusResponse;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.ws.rs.*;
import java.sql.Timestamp;
import java.util.*;

import static java.lang.System.currentTimeMillis;
import static java.util.Arrays.asList;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static no.nav.fo.veilarbsituasjon.domain.VilkarStatus.GODKJENNT;
import static no.nav.fo.veilarbsituasjon.domain.VilkarStatus.IKKE_BESVART;

// TODO dette skal bli en webservice når tjenestespesifikasjonen er klar!
@Component
@Path("/ws/aktivitetsplan")
@Produces(APPLICATION_JSON)
@Api
public class AktivitetsplanSituasjonWebService {

    private static final Set<String> ARBEIDSOKERKODER = new HashSet<>(asList("ARBS", "RARBS", "PARBS"));
    private static final Set<String> OPPFOLGINGKODER = new HashSet<>(asList("BATT", "BFORM", "IKVAL", "VURDU", "OPPFI"));

    private final DigitalKontaktinformasjonV1 digitalKontaktinformasjonV1;
    private final SituasjonRepository situasjonRepository;
    private final AktoerIdService aktoerIdService;
    private final OppfoelgingPortType oppfoelgingPortType;
    private final PepClient pepClient;

    public AktivitetsplanSituasjonWebService(
            DigitalKontaktinformasjonV1 digitalKontaktinformasjonV1,
            SituasjonRepository situasjonRepository,
            AktoerIdService aktoerIdService,
            OppfoelgingPortType oppfoelgingPortType,
            PepClient pepClient) {
        this.digitalKontaktinformasjonV1 = digitalKontaktinformasjonV1;
        this.situasjonRepository = situasjonRepository;
        this.aktoerIdService = aktoerIdService;
        this.oppfoelgingPortType = oppfoelgingPortType;
        this.pepClient = pepClient;
    }

    @GET
    @Path("/{fnr}")
    @Transactional
    public OppfolgingOgVilkarStatus hentOppfolgingsStatus(@PathParam("fnr") String fnr) throws Exception {
        pepClient.isServiceCallAllowed(fnr);

        String aktorId = hentAktorId(fnr);
        Situasjon situasjon = hentSituasjon(aktorId);

        if (!situasjon.isOppfolging()) {
            situasjonRepository.oppdaterSituasjon(situasjon.setOppfolging(erUnderOppfolging(fnr)));
        }

        boolean erReservert = erReservertIKRR(fnr);
        if (erReservert && situasjon.isOppfolging()) {
            Timestamp dato = new Timestamp(currentTimeMillis());
            situasjonRepository.opprettStatus(
                    new Status(
                            aktorId,
                            true,
                            dato,
                            "Reservert og under oppfølging"
                    )
            );
            situasjonRepository.opprettBrukervilkar(
                    new Brukervilkar(
                            aktorId,
                            dato,
                            IKKE_BESVART,
                            ""
                    )
            );
        }

        String gjeldendeVilkar = finnGjeldendeVilkar();
        boolean vilkarMaBesvares = finnSisteVilkarStatus(situasjon)
                .filter(brukervilkar -> GODKJENNT.equals(brukervilkar.getVilkarstatus()))
                .map(Brukervilkar::getTekst)
                .map(brukerVilkar -> !brukerVilkar.equals(gjeldendeVilkar))
                .orElse(true);

        return new OppfolgingOgVilkarStatus()
                .setFnr(fnr)
                .setReservasjonKRR(erReservert)
                .setManuell(Optional.ofNullable(situasjon.getGjeldendeStatus())
                        .map(Status::isManuell)
                        .orElse(false)
                )
                .setUnderOppfolging(situasjon.isOppfolging())
                .setVilkarMaBesvares(vilkarMaBesvares);
    }

    @GET
    @Path("/vilkar")
    public String hentVilkar() throws Exception {
        return finnGjeldendeVilkar();
    }

    @POST
    public OpprettVilkarStatusResponse opprettVilkaarstatus(OpprettVilkarStatusRequest opprettVilkarStatusRequest) throws Exception {
        final String fnr = opprettVilkarStatusRequest.fnr;
        pepClient.isServiceCallAllowed(fnr);

        Situasjon situasjon = hentSituasjon(hentAktorId(fnr));

        // TODO Hva gjør vi med hashen vs. teksten?
        situasjonRepository.opprettBrukervilkar(
                new Brukervilkar(
                        situasjon.getAktorId(),
                        new Timestamp(currentTimeMillis()),
                        opprettVilkarStatusRequest.status,
                        opprettVilkarStatusRequest.hash)
        );

        return new OpprettVilkarStatusResponse()
                .setFnr(fnr)
                .setStatus(opprettVilkarStatusRequest.status);
    }

    private boolean erUnderOppfolging(String fnr) throws Exception {
        val hentOppfolgingstatusRequest = new WSHentOppfoelgingsstatusRequest();
        hentOppfolgingstatusRequest.setPersonidentifikator(fnr);
        val oppfolgingstatus = oppfoelgingPortType.hentOppfoelgingsstatus(hentOppfolgingstatusRequest);

        return erArbeidssoker(oppfolgingstatus) || erIArbeidOgHarInnsatsbehov(oppfolgingstatus);
    }

    private boolean erReservertIKRR(String fnr) throws Exception {
        val wsHentDigitalKontaktinformasjonRequest = new WSHentDigitalKontaktinformasjonRequest().withPersonident(fnr);
        return of(digitalKontaktinformasjonV1.hentDigitalKontaktinformasjon(wsHentDigitalKontaktinformasjonRequest))
                .map(WSHentDigitalKontaktinformasjonResponse::getDigitalKontaktinformasjon)
                .map(WSKontaktinformasjon::getReservasjon)
                .map("true"::equalsIgnoreCase)
                .orElse(false);
    }

    private boolean erArbeidssoker(WSHentOppfoelgingsstatusResponse oppfolgingstatus) {
        return ARBEIDSOKERKODER.contains(oppfolgingstatus.getFormidlingsgruppeKode());
    }

    private boolean erIArbeidOgHarInnsatsbehov(WSHentOppfoelgingsstatusResponse oppfolgingstatus) {
        return OPPFOLGINGKODER.contains(oppfolgingstatus.getServicegruppeKode());
    }

    private String finnGjeldendeVilkar() {
        return "gjeldendeVilkar";
    }

    private Situasjon hentSituasjon(String aktorId) {
        return situasjonRepository.hentSituasjon(aktorId).orElse(new Situasjon().setAktorId(aktorId));
    }

    private String hentAktorId(String fnr) {
        return ofNullable(aktoerIdService.findAktoerId(fnr))
                .orElseThrow(() -> new IllegalArgumentException("Fant ikke aktør for fnr: " + fnr));
    }

    private Optional<Brukervilkar> finnSisteVilkarStatus(Situasjon situasjon) {
        return Optional.ofNullable(situasjon.getGjeldendeBrukervilkar());
    }

    @Data
    @Accessors(chain = true)
    public static class OppfolgingOgVilkarStatus {
        public String fnr;
        public boolean reservasjonKRR;
        public boolean manuell;
        public boolean underOppfolging;
        public boolean vilkarMaBesvares;
    }

    @Data
    @Accessors(chain = true)
    public static class OpprettVilkarStatusRequest {
        public String fnr;
        public VilkarStatus status;
        public String hash;
    }

    @Data
    @Accessors(chain = true)
    public static class OpprettVilkarStatusResponse {
        public String fnr;
        public VilkarStatus status;
    }

}
