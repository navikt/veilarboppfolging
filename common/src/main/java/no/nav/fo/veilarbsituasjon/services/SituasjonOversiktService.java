package no.nav.fo.veilarbsituasjon.services;

import lombok.val;
import no.nav.fo.veilarbsituasjon.db.SituasjonRepository;
import no.nav.fo.veilarbsituasjon.domain.*;
import no.nav.fo.veilarbsituasjon.vilkar.VilkarService;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.DigitalKontaktinformasjonV1;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.HentDigitalKontaktinformasjonKontaktinformasjonIkkeFunnet;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.HentDigitalKontaktinformasjonPersonIkkeFunnet;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.HentDigitalKontaktinformasjonSikkerhetsbegrensing;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.informasjon.WSKontaktinformasjon;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.meldinger.WSHentDigitalKontaktinformasjonRequest;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.meldinger.WSHentDigitalKontaktinformasjonResponse;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.OppfoelgingPortType;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.meldinger.WSHentOppfoelgingsstatusRequest;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.meldinger.WSHentOppfoelgingsstatusResponse;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Inject;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static java.lang.System.currentTimeMillis;
import static java.util.Arrays.asList;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static no.nav.fo.veilarbsituasjon.domain.VilkarStatus.GODKJENNT;
import static no.nav.fo.veilarbsituasjon.domain.VilkarStatus.IKKE_BESVART;
import static org.slf4j.LoggerFactory.getLogger;

@Component
public class SituasjonOversiktService {

    private static final Logger LOG = getLogger(SituasjonOversiktService.class);

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

    @Inject
    private VilkarService vilkarService;

    @Transactional
    public OppfolgingStatusData hentOppfolgingsStatus(String fnr) throws Exception {
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
                            "",
                            ""
                    )
            );
        }

        VilkarData gjeldendeVilkar = hentVilkar();
        boolean vilkarMaBesvares = finnSisteVilkarStatus(situasjon)
                .filter(brukervilkar -> GODKJENNT.equals(brukervilkar.getVilkarstatus()))
                .map(Brukervilkar::getHash)
                .map(brukerVilkar -> !brukerVilkar.equals(gjeldendeVilkar.getHash()))
                .orElse(true);

        return new OppfolgingStatusData()
                .setFnr(fnr)
                .setReservasjonKRR(erReservert)
                .setManuell(Optional.ofNullable(situasjon.getGjeldendeStatus())
                        .map(Status::isManuell)
                        .orElse(false)
                )
                .setUnderOppfolging(situasjon.isOppfolging())
                .setVilkarMaBesvares(vilkarMaBesvares);
    }

    public VilkarData hentVilkar() throws Exception {
        String vilkar = vilkarService.getVilkar(null);
        return new VilkarData()
                .setText(vilkar)
                .setHash(DigestUtils.sha256Hex(vilkar));
    }

    @Transactional
    public OppfolgingStatusData godtaVilkar(String hash, String fnr) throws Exception {
        Situasjon situasjon = hentSituasjon(hentAktorId(fnr));

        VilkarData gjeldendeVilkar = hentVilkar();
        if (gjeldendeVilkar.getHash().equals(hash)) {
            situasjonRepository.opprettBrukervilkar(
                    new Brukervilkar(
                            situasjon.getAktorId(),
                            new Timestamp(currentTimeMillis()),
                            VilkarStatus.GODKJENNT,
                            gjeldendeVilkar.getText(),
                            hash
                    ));
        }
        return hentOppfolgingsStatus(fnr);
    }

    private boolean erUnderOppfolging(String fnr) throws Exception {
        val hentOppfolgingstatusRequest = new WSHentOppfoelgingsstatusRequest();
        hentOppfolgingstatusRequest.setPersonidentifikator(fnr);
        val oppfolgingstatus = oppfoelgingPortType.hentOppfoelgingsstatus(hentOppfolgingstatusRequest);

        return erArbeidssoker(oppfolgingstatus) || erIArbeidOgHarInnsatsbehov(oppfolgingstatus);
    }

    private boolean erReservertIKRR(String fnr) throws Exception {
        try {
            val wsHentDigitalKontaktinformasjonRequest = new WSHentDigitalKontaktinformasjonRequest().withPersonident(fnr);
            return of(digitalKontaktinformasjonV1.hentDigitalKontaktinformasjon(wsHentDigitalKontaktinformasjonRequest))
                    .map(WSHentDigitalKontaktinformasjonResponse::getDigitalKontaktinformasjon)
                    .map(WSKontaktinformasjon::getReservasjon)
                    .map("true"::equalsIgnoreCase)
                    .orElse(false);
        } catch (HentDigitalKontaktinformasjonPersonIkkeFunnet | HentDigitalKontaktinformasjonKontaktinformasjonIkkeFunnet e) {
            LOG.warn(e.getMessage(), e);
            return true;
        }
    }

    private boolean erArbeidssoker(WSHentOppfoelgingsstatusResponse oppfolgingstatus) {
        return ARBEIDSOKERKODER.contains(oppfolgingstatus.getFormidlingsgruppeKode());
    }

    private boolean erIArbeidOgHarInnsatsbehov(WSHentOppfoelgingsstatusResponse oppfolgingstatus) {
        return OPPFOLGINGKODER.contains(oppfolgingstatus.getServicegruppeKode());
    }

    private Situasjon hentSituasjon(String aktorId) {
        return situasjonRepository.hentSituasjon(aktorId)
                .orElseGet(() -> situasjonRepository.opprettSituasjon(new Situasjon().setAktorId(aktorId)));
    }

    private String hentAktorId(String fnr) {
        return ofNullable(aktoerIdService.findAktoerId(fnr))
                .orElseThrow(() -> new IllegalArgumentException("Fant ikke aktør for fnr: " + fnr));
    }

    private Optional<Brukervilkar> finnSisteVilkarStatus(Situasjon situasjon) {
        return Optional.ofNullable(situasjon.getGjeldendeBrukervilkar());
    }


}
