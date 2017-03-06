package no.nav.fo.veilarbsituasjon.services;

import lombok.val;
import no.nav.fo.veilarbsituasjon.db.SituasjonRepository;
import no.nav.fo.veilarbsituasjon.domain.Brukervilkar;
import no.nav.fo.veilarbsituasjon.domain.Situasjon;
import no.nav.fo.veilarbsituasjon.rest.domain.OppfolgingOgVilkarStatus;
import no.nav.fo.veilarbsituasjon.rest.domain.OpprettVilkarStatusRequest;
import no.nav.fo.veilarbsituasjon.rest.domain.OpprettVilkarStatusResponse;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.DigitalKontaktinformasjonV1;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.informasjon.WSKontaktinformasjon;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.meldinger.WSHentDigitalKontaktinformasjonRequest;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.meldinger.WSHentDigitalKontaktinformasjonResponse;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.OppfoelgingPortType;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.meldinger.WSHentOppfoelgingsstatusRequest;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.meldinger.WSHentOppfoelgingsstatusResponse;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Inject;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static java.lang.System.currentTimeMillis;
import static java.util.Arrays.asList;
import static java.util.Comparator.comparing;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static no.nav.fo.veilarbsituasjon.domain.VilkarStatus.GODKJENNT;
import static no.nav.fo.veilarbsituasjon.domain.VilkarStatus.IKKE_BESVART;

@Component
public class SituasjonOversiktService {

    private static final Set<String> ARBEIDSOKERKODER = new HashSet<>(asList("ARBS", "RARBS", "PARBS"));
    private static final Set<String> OPPFOLGINGKODER = new HashSet<>(asList("BATT", "BFORM", "IKVAL", "VURDU", "OPPFI"));

    @Inject
    private DigitalKontaktinformasjonV1 digitalKontaktinformasjonV1;

    @Inject
    private SituasjonRepository situasjonRepository;

    @Inject
    private AktoerIdService aktoerIdService;

    @Inject
    private OppfoelgingPortType oppfoelgingPortType;

    @Transactional
    public OppfolgingOgVilkarStatus hentOppfolgingsStatus(String fnr) throws Exception {
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

    public String hentVilkar() throws Exception {
        return finnGjeldendeVilkar();
    }

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

    private boolean erUnderOppfolging(String aktorId) throws Exception {
        val hentOppfolgingstatusRequest = new WSHentOppfoelgingsstatusRequest();
        hentOppfolgingstatusRequest.setPersonidentifikator(aktorId);
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
        return situasjon.getBrukervilkar().stream()
                .sorted(comparing(Brukervilkar::getDato).reversed())
                .findFirst();
    }


}
