package no.nav.fo.veilarbsituasjon.services;

import lombok.Getter;
import lombok.SneakyThrows;
import lombok.val;
import no.nav.fo.veilarbsituasjon.db.SituasjonRepository;
import no.nav.fo.veilarbsituasjon.domain.*;
import no.nav.fo.veilarbsituasjon.utils.StringUtils;
import no.nav.fo.veilarbsituasjon.vilkar.VilkarService;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.DigitalKontaktinformasjonV1;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.HentDigitalKontaktinformasjonKontaktinformasjonIkkeFunnet;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.HentDigitalKontaktinformasjonPersonIkkeFunnet;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.informasjon.WSKontaktinformasjon;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.meldinger.WSHentDigitalKontaktinformasjonRequest;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.meldinger.WSHentDigitalKontaktinformasjonResponse;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.OppfoelgingPortType;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.meldinger.WSHentOppfoelgingsstatusRequest;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.meldinger.WSHentOppfoelgingsstatusResponse;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.sql.Timestamp;
import java.util.List;

import static java.lang.System.currentTimeMillis;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static no.nav.fo.veilarbsituasjon.domain.VilkarStatus.GODKJENT;
import static no.nav.fo.veilarbsituasjon.services.ArenaUtils.erUnderOppfolging;
import static no.nav.fo.veilarbsituasjon.services.ArenaUtils.kanSettesUnderOppfolging;
import static no.nav.fo.veilarbsituasjon.vilkar.VilkarService.VilkarType.PRIVAT;
import static no.nav.fo.veilarbsituasjon.vilkar.VilkarService.VilkarType.UNDER_OPPFOLGING;
import static org.slf4j.LoggerFactory.getLogger;

public class SituasjonResolver {

    private static final Logger LOG = getLogger(SituasjonResolver.class);

    private String fnr;
    private SituasjonResolverDependencies deps;

    private String aktorId;
    private Situasjon situasjon;
    private WSHentOppfoelgingsstatusResponse statusIArena;
    private boolean reservertIKrr;

    SituasjonResolver(String fnr, SituasjonResolverDependencies deps) {
        this.fnr = fnr;
        this.deps = deps;

        this.aktorId = ofNullable(deps.getAktoerIdService().findAktoerId(fnr))
                .orElseThrow(() -> new IllegalArgumentException("Fant ikke aktør for fnr: " + fnr));
        this.situasjon = hentSituasjon();
    }

    void sjekkStatusIArenaOgOppdaterSituasjon() {
        if (!situasjon.isOppfolging()) {
            this.statusIArena = sjekkArena(fnr);

            deps.getSituasjonRepository().oppdaterSituasjon(
                    situasjon.setOppfolging(
                            erUnderOppfolging(statusIArena)
                    )
            );
        }
    }

    void sjekkReservasjonIKrrOgOppdaterSituasjon() {
        if (situasjon.isOppfolging()) {
            this.reservertIKrr = sjekkKrr(fnr);
            if (!manuell() && reservertIKrr) {
                deps.getSituasjonRepository().opprettStatus(
                        new Status(
                                situasjon.getAktorId(),
                                true,
                                new Timestamp(currentTimeMillis()),
                                "Reservert og under oppfølging")
                );
            }
        }
    }

    void sjekkNyesteVilkarOgOppdaterSituasjon(String hash, VilkarStatus vilkarStatus) {
        Brukervilkar gjeldendeVilkar = getNyesteVilkar();
        if (gjeldendeVilkar.getHash().equals(hash)) {
            deps.getSituasjonRepository().opprettBrukervilkar(
                    new Brukervilkar(
                            aktorId,
                            new Timestamp(currentTimeMillis()),
                            vilkarStatus,
                            gjeldendeVilkar.getTekst(),
                            hash
                    ));
        }
    }

    Brukervilkar getNyesteVilkar() {
        String vilkarTekst = deps.getVilkarService().getVilkar(situasjon.isOppfolging() ? UNDER_OPPFOLGING : PRIVAT, null);
        return new Brukervilkar()
                .setTekst(vilkarTekst)
                .setHash(DigestUtils.sha256Hex(vilkarTekst));
    }

    boolean maVilkarBesvares() {
        return ofNullable(situasjon.getGjeldendeBrukervilkar())
                .filter(brukervilkar -> GODKJENT.equals(brukervilkar.getVilkarstatus()))
                .map(Brukervilkar::getHash)
                .map(brukerVilkar -> !brukerVilkar.equals(getNyesteVilkar().getHash()))
                .orElse(true);
    }

    List<MalData> getMalList() {
        return deps.getSituasjonRepository().hentMalList(aktorId);
    }

    MalData oppdaterMal(String mal, String endretAv) {
        MalData malData = new MalData()
                .setAktorId(aktorId)
                .setMal(mal)
                .setEndretAv(StringUtils.of(endretAv).orElse(aktorId))
                .setDato(new Timestamp(currentTimeMillis()));
        deps.getSituasjonRepository().opprettMal(malData);
        return hentSituasjon().getGjeldendeMal();
    }

    Situasjon getSitusjon() {
        return situasjon;
    }

    boolean reservertIKrr() {
        return reservertIKrr;
    }

    boolean manuell() {
        return ofNullable(situasjon.getGjeldendeStatus())
                .map(Status::isManuell)
                .orElse(false);
    }

    boolean getKanSettesUnderOppfolging() {
        return !situasjon.isOppfolging() && kanSettesUnderOppfolging(statusIArena);
    }

    boolean erUnderOppfolgingIArena() {
        statusIArena = sjekkArena(fnr);
        return erUnderOppfolging(statusIArena);
    }

    private Situasjon hentSituasjon() {
        return deps.getSituasjonRepository().hentSituasjon(aktorId)
                .orElseGet(() -> deps.getSituasjonRepository().opprettSituasjon(new Situasjon().setAktorId(aktorId)));
    }

    @SneakyThrows
    private WSHentOppfoelgingsstatusResponse sjekkArena(String fnr) {
        val hentOppfolgingstatusRequest = new WSHentOppfoelgingsstatusRequest();
        hentOppfolgingstatusRequest.setPersonidentifikator(fnr);
        return deps.getOppfoelgingPortType().hentOppfoelgingsstatus(hentOppfolgingstatusRequest);
    }

    @SneakyThrows
    private boolean sjekkKrr(String fnr) {
        val req = new WSHentDigitalKontaktinformasjonRequest().withPersonident(fnr);
        try {
            return of(deps.getDigitalKontaktinformasjonV1().hentDigitalKontaktinformasjon(req))
                    .map(WSHentDigitalKontaktinformasjonResponse::getDigitalKontaktinformasjon)
                    .map(WSKontaktinformasjon::getReservasjon)
                    .map("true"::equalsIgnoreCase)
                    .orElse(false);
        } catch (HentDigitalKontaktinformasjonKontaktinformasjonIkkeFunnet | HentDigitalKontaktinformasjonPersonIkkeFunnet e) {
            LOG.warn(e.getMessage(), e);
            return true;
        }
    }

    @Component
    @Getter
    public static class SituasjonResolverDependencies {

        @Inject
        private AktoerIdService aktoerIdService;

        @Inject
        private SituasjonRepository situasjonRepository;

        @Inject
        private OppfoelgingPortType oppfoelgingPortType;

        @Inject
        private DigitalKontaktinformasjonV1 digitalKontaktinformasjonV1;

        @Inject
        private VilkarService vilkarService;
    }
}
