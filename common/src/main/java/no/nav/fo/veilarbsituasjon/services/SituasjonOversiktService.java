package no.nav.fo.veilarbsituasjon.services;


import io.swagger.annotations.Api;
import lombok.SneakyThrows;
import lombok.val;
import no.nav.fo.veilarbsituasjon.db.SituasjonRepository;
import no.nav.fo.veilarbsituasjon.domain.*;
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
import no.nav.tjeneste.virksomhet.ytelseskontrakt.v3.YtelseskontraktV3;
import no.nav.tjeneste.virksomhet.ytelseskontrakt.v3.meldinger.WSHentYtelseskontraktListeRequest;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Inject;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

import static java.lang.System.currentTimeMillis;
import static java.util.Arrays.asList;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static no.nav.fo.veilarbaktivitet.domain.AktivitetStatus.AVBRUTT;
import static no.nav.fo.veilarbaktivitet.domain.AktivitetStatus.FULLFORT;
import static no.nav.fo.veilarbsituasjon.domain.VilkarStatus.GODKJENT;
import static no.nav.fo.veilarbsituasjon.domain.VilkarStatus.IKKE_BESVART;
import static no.nav.fo.veilarbsituasjon.utils.StringUtils.of;
import static no.nav.fo.veilarbsituasjon.vilkar.VilkarService.VilkarType.PRIVAT;
import static no.nav.fo.veilarbsituasjon.vilkar.VilkarService.VilkarType.UNDER_OPPFOLGING;
import static org.slf4j.LoggerFactory.getLogger;

@Component
@Api
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
    private YtelseskontraktV3 ytelseskontraktV3;

    @Inject
    private VilkarService vilkarService;

    @Inject
    private VeilarbaktivtetService veilarbaktivtetService;

    @Transactional
    public OppfolgingStatusData hentOppfolgingsStatus(String fnr) throws Exception {
        Situasjon situasjon = situasjonForFnr(fnr);
        String aktorId = situasjon.getAktorId();

        if (!situasjon.isOppfolging()) {
            situasjonRepository.oppdaterSituasjon(situasjon.setOppfolging(erUnderOppfolging(fnr)));
        }

        boolean erReservertOgUnderOppfolging = situasjon.isOppfolging() && erReservertIKRR(fnr);
        if (erReservertOgUnderOppfolging) {
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

        Brukervilkar gjeldendeVilkar = hentVilkar(situasjon);
        boolean vilkarMaBesvares = finnSisteVilkarStatus(situasjon)
                .filter(brukervilkar -> GODKJENT.equals(brukervilkar.getVilkarstatus()))
                .map(Brukervilkar::getHash)
                .map(brukerVilkar -> !brukerVilkar.equals(gjeldendeVilkar.getHash()))
                .orElse(true);

        return new OppfolgingStatusData()
                .setFnr(fnr)
                .setReservasjonKRR(erReservertOgUnderOppfolging)
                .setManuell(Optional.ofNullable(situasjon.getGjeldendeStatus())
                        .map(Status::isManuell)
                        .orElse(false)
                )
                .setUnderOppfolging(situasjon.isOppfolging())
                .setOppfolgingUtgang(situasjon.getOppfolgingUtgang())
                .setVilkarMaBesvares(vilkarMaBesvares);
    }

    public AvslutningStatusData hentAvslutningStatus(String fnr) throws Exception {
        boolean erUnderOppfolging = erUnderOppfolging(fnr);
        boolean harPagaendeYtelser = harPagaendeYtelser(fnr);
        boolean harAktiveTiltalk = harAktiveTiltak(fnr);

        boolean kanAvslutte = situasjonForFnr(fnr).isOppfolging()
                && !erUnderOppfolging
                && !harPagaendeYtelser
                && !harAktiveTiltalk;

        // TODO: Erstatt dette når inaktiveringsDato finnes i arena
        LocalDate date = LocalDate.now().minusMonths(2);
        Date toManedSiden = Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date inaktiveringsDato = fnr.equals("***REMOVED***") ? toManedSiden : new Date();
        //

        return AvslutningStatusData.builder()
                .kanAvslutte(kanAvslutte)
                .underOppfolging(erUnderOppfolging)
                .harYtelser(harPagaendeYtelser)
                .harTiltak(harAktiveTiltalk)
                .inaktiveringsDato(inaktiveringsDato)
                .build();
    }

    private boolean harAktiveTiltak(String fnr) {
        return veilarbaktivtetService
                .hentArenaAktiviteter(fnr)
                .stream()
                .anyMatch(arenaAktivitetDTO -> arenaAktivitetDTO.getStatus() != AVBRUTT && arenaAktivitetDTO.getStatus() != FULLFORT);
    }

    @SneakyThrows
    private boolean harPagaendeYtelser(String fnr)  {
        val wsHentYtelseskontraktListeRequest = new WSHentYtelseskontraktListeRequest();
        wsHentYtelseskontraktListeRequest.setPersonidentifikator(fnr);
        val wsHentYtelseskontraktListeResponse = ytelseskontraktV3.hentYtelseskontraktListe(wsHentYtelseskontraktListeRequest);
        return !wsHentYtelseskontraktListeResponse.getYtelseskontraktListe().isEmpty();
    }

    public Brukervilkar hentVilkar(String fnr) throws Exception {
        return hentVilkar(situasjonForFnr(fnr));
    }

    private Situasjon situasjonForFnr(String fnr) {
        String aktorId = hentAktorId(fnr);
        return hentSituasjon(aktorId);
    }

    public Brukervilkar hentVilkar(Situasjon situasjon) {
        String vilkar = vilkarService.getVilkar(situasjon.isOppfolging() ? UNDER_OPPFOLGING : PRIVAT, null);
        return new Brukervilkar()
                .setTekst(vilkar)
                .setHash(DigestUtils.sha256Hex(vilkar));
    }

    @Transactional
    public OppfolgingStatusData oppdaterVilkaar(String hash, String fnr, VilkarStatus vilkarStatus) throws Exception {
        Situasjon situasjon = hentSituasjon(hentAktorId(fnr));

        Brukervilkar gjeldendeVilkar = hentVilkar(situasjon);
        if (gjeldendeVilkar.getHash().equals(hash)) {
            situasjonRepository.opprettBrukervilkar(
                    new Brukervilkar(
                            situasjon.getAktorId(),
                            new Timestamp(currentTimeMillis()),
                            vilkarStatus,
                            gjeldendeVilkar.getTekst(),
                            hash
                    ));
        }
        return hentOppfolgingsStatus(fnr);
    }

    public MalData hentMal(String fnr) {
        return Optional.ofNullable(hentSituasjon(hentAktorId(fnr)).getGjeldendeMal()).orElse(new MalData());
    }

    public List<MalData> hentMalList(String fnr) {
        return situasjonRepository.hentMalList(hentAktorId(fnr));
    }

    public List<Brukervilkar> hentHistoriskeVilkar(String fnr) {
        return situasjonRepository.hentHistoriskeVilkar(hentAktorId(fnr));
    }

    public MalData oppdaterMal(String mal, String fnr, String endretAv) {
        String aktorId = hentAktorId(fnr);
        Timestamp dato = new Timestamp(currentTimeMillis());
        MalData malData = new MalData()
                .setAktorId(aktorId)
                .setMal(mal)
                .setEndretAv(of(endretAv).orElse(aktorId))
                .setDato(dato);
        situasjonRepository.opprettMal(malData);
        return hentMal(fnr);
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

    @SneakyThrows
    public AvslutningStatusData avsluttOppfolging(String aktorId, String veilederId, String begrunnelse) {
        val avslutningStatus = hentAvslutningStatus(aktorId);
        val oppfolgingsperiode = Oppfolgingsperiode.builder()
                .aktorId(aktorId)
                .veilederId(veilederId)
                .sluttDato(new Date())
                .begrunnelse(begrunnelse)
                .build();

        if (avslutningStatus.kanAvslutte) {
            situasjonRepository.oppdaterSituasjon(aktorId, false);
            situasjonRepository.opprettOppfolgingsperiode(oppfolgingsperiode);
        }

        return avslutningStatus;
    }
}
