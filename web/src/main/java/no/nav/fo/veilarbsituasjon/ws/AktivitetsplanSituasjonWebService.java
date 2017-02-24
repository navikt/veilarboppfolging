package no.nav.fo.veilarbsituasjon.ws;


import lombok.Data;
import lombok.experimental.Accessors;
import lombok.val;
import no.nav.fo.veilarbsituasjon.db.SituasjonRepository;
import no.nav.fo.veilarbsituasjon.domain.Brukervilkar;
import no.nav.fo.veilarbsituasjon.domain.Situasjon;
import no.nav.fo.veilarbsituasjon.domain.VilkarStatus;
import no.nav.fo.veilarbsituasjon.services.AktoerIdService;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.DigitalKontaktinformasjonV1;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.informasjon.WSKontaktinformasjon;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.meldinger.WSHentDigitalKontaktinformasjonRequest;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.meldinger.WSHentDigitalKontaktinformasjonResponse;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.HentOppfoelgingskontraktListeSikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.OppfoelgingPortType;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.informasjon.WSOppfoelgingskontrakt;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.meldinger.WSHentOppfoelgingskontraktListeRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.ws.rs.*;
import java.sql.Timestamp;
import java.util.Optional;

import static java.lang.System.currentTimeMillis;
import static java.util.Comparator.comparing;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static no.nav.fo.veilarbsituasjon.domain.VilkarStatus.GODKJENNT;
import static no.nav.fo.veilarbsituasjon.domain.VilkarStatus.IKKE_BESVART;
import static no.nav.fo.veilarbsituasjon.mock.OppfoelgingV1Mock.AKTIV_STATUS;

// TODO dette skal bli en webservice når tjenestespesifikasjonen er klar!
@Component
@Path("/ws/aktivitetsplan")
@Produces(APPLICATION_JSON)
public class AktivitetsplanSituasjonWebService {

    private final DigitalKontaktinformasjonV1 digitalKontaktinformasjonV1;
    private final SituasjonRepository situasjonRepository;
    private final AktoerIdService aktoerIdService;
    private final OppfoelgingPortType oppfoelgingPortType;

    public AktivitetsplanSituasjonWebService(
            DigitalKontaktinformasjonV1 digitalKontaktinformasjonV1,
            SituasjonRepository situasjonRepository,
            AktoerIdService aktoerIdService,
            OppfoelgingPortType oppfoelgingPortType) {
        this.digitalKontaktinformasjonV1 = digitalKontaktinformasjonV1;
        this.situasjonRepository = situasjonRepository;
        this.aktoerIdService = aktoerIdService;
        this.oppfoelgingPortType = oppfoelgingPortType;
    }

    @GET
    @Path("/{fnr}")
    @Transactional
    public OppfolgingOgVilkarStatus hentOppfolgingsStatus(@PathParam("fnr") String fnr) throws Exception {
        String aktorId = hentAktorId(fnr);
        Situasjon situasjon = hentSituasjon(aktorId);

        if (!situasjon.isOppfolging()) {
            situasjonRepository.oppdaterSituasjon(situasjon.setOppfolging(erUnderOppfolging(aktorId)));
        }

        boolean erReservert = erReservertIKRR(fnr);
        if (erReservert && situasjon.isOppfolging()) {
            situasjonRepository.oppdaterSituasjon(situasjon
                    .setManuell(true)
                    .leggTilBrukervilkar(new Brukervilkar().setVilkarstatus(IKKE_BESVART))
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
                .setManuell(situasjon.isManuell())
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
        Situasjon situasjon = hentSituasjon(hentAktorId(opprettVilkarStatusRequest.fnr));

        situasjonRepository.oppdaterSituasjon(situasjon.leggTilBrukervilkar(new Brukervilkar()
                        .setDato(new Timestamp(currentTimeMillis()))
                        .setTekst(opprettVilkarStatusRequest.hash)
                        .setVilkarstatus(opprettVilkarStatusRequest.status)
                )
        );

        return new OpprettVilkarStatusResponse()
                .setFnr(opprettVilkarStatusRequest.fnr)
                .setStatus(opprettVilkarStatusRequest.status);
    }

    private boolean erUnderOppfolging(String aktorId) throws HentOppfoelgingskontraktListeSikkerhetsbegrensning {
        val wsHentOppfoelgingskontraktListeRequest = new WSHentOppfoelgingskontraktListeRequest();
        wsHentOppfoelgingskontraktListeRequest.setPersonidentifikator(aktorId);
        return oppfoelgingPortType.hentOppfoelgingskontraktListe(wsHentOppfoelgingskontraktListeRequest)
                .getOppfoelgingskontraktListe()
                .stream()
                .map(WSOppfoelgingskontrakt::getStatus)
                .anyMatch(status -> AKTIV_STATUS.equals(status));
    }

    private boolean erReservertIKRR(String fnr) throws Exception {
        val wsHentDigitalKontaktinformasjonRequest = new WSHentDigitalKontaktinformasjonRequest().withPersonident(fnr);
        return of(digitalKontaktinformasjonV1.hentDigitalKontaktinformasjon(wsHentDigitalKontaktinformasjonRequest))
                .map(WSHentDigitalKontaktinformasjonResponse::getDigitalKontaktinformasjon)
                .map(WSKontaktinformasjon::getReservasjon)
                .map(StringUtils::isNotEmpty)
                .orElse(false);
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
        return situasjon.getBrukervilkar().stream()
                .sorted(comparing(Brukervilkar::getDato).reversed())
                .findFirst();
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
