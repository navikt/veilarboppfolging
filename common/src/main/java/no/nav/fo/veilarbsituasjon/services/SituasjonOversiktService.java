package no.nav.fo.veilarbsituasjon.services;

import io.swagger.annotations.Api;
import lombok.SneakyThrows;
import lombok.val;
import no.nav.fo.veilarbsituasjon.db.SituasjonRepository;
import no.nav.fo.veilarbsituasjon.domain.*;
import no.nav.fo.veilarbsituasjon.services.SituasjonResolver.SituasjonResolverDependencies;
import no.nav.tjeneste.virksomhet.ytelseskontrakt.v3.YtelseskontraktV3;
import no.nav.tjeneste.virksomhet.ytelseskontrakt.v3.meldinger.WSHentYtelseskontraktListeRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Inject;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static java.util.Optional.ofNullable;
import static no.nav.fo.veilarbaktivitet.domain.AktivitetStatus.AVBRUTT;
import static no.nav.fo.veilarbaktivitet.domain.AktivitetStatus.FULLFORT;

@Component
@Api
public class SituasjonOversiktService {

    @Inject
    private SituasjonRepository situasjonRepository;

    @Inject
    private AktoerIdService aktoerIdService;

    @Inject
    private YtelseskontraktV3 ytelseskontraktV3;

    @Inject
    private VeilarbaktivtetService veilarbaktivtetService;

    @Inject
    private SituasjonResolverDependencies situasjonResolverDependencies;

    @Transactional
    public OppfolgingStatusData hentOppfolgingsStatus(String fnr) throws Exception {
        val situasjonResolver = new SituasjonResolver(fnr, situasjonResolverDependencies);

        situasjonResolver.sjekkStatusIArenaOgOppdaterSituasjon();
        situasjonResolver.sjekkReservasjonIKrrOgOppdaterSituasjon();

        return new OppfolgingStatusData()
                .setFnr(fnr)
                .setUnderOppfolging(situasjonResolver.getSitusjon().isOppfolging())
                .setReservasjonKRR(situasjonResolver.reservertIKrr())
                .setManuell(situasjonResolver.manuell())
                .setOppfolgingUtgang(situasjonResolver.getSitusjon().getOppfolgingUtgang())
                .setVilkarMaBesvares(situasjonResolver.maVilkarBesvares())
                .setKanStarteOppfolging(situasjonResolver.getKanSettesUnderOppfolging());
    }

    public Brukervilkar hentVilkar(String fnr) throws Exception {
        return new SituasjonResolver(fnr, situasjonResolverDependencies).getNyesteVilkar();
    }

    public List<Brukervilkar> hentHistoriskeVilkar(String fnr) {
        return situasjonRepository.hentHistoriskeVilkar(hentAktorId(fnr));
    }

    @Transactional
    public OppfolgingStatusData oppdaterVilkaar(String hash, String fnr, VilkarStatus vilkarStatus) throws Exception {
        val situasjonResolver = new SituasjonResolver(fnr, situasjonResolverDependencies);

        situasjonResolver.sjekkNyesteVilkarOgOppdaterSituasjon(hash, vilkarStatus);

        situasjonResolver.sjekkStatusIArenaOgOppdaterSituasjon();
        situasjonResolver.sjekkReservasjonIKrrOgOppdaterSituasjon();
        return new OppfolgingStatusData()
                .setFnr(fnr)
                .setUnderOppfolging(situasjonResolver.getSitusjon().isOppfolging())
                .setReservasjonKRR(situasjonResolver.reservertIKrr())
                .setManuell(situasjonResolver.manuell())
                .setOppfolgingUtgang(situasjonResolver.getSitusjon().getOppfolgingUtgang())
                .setVilkarMaBesvares(situasjonResolver.maVilkarBesvares())
                .setKanStarteOppfolging(situasjonResolver.getKanSettesUnderOppfolging());
    }

    public MalData hentMal(String fnr) {
        MalData gjeldendeMal = new SituasjonResolver(fnr, situasjonResolverDependencies).getSitusjon().getGjeldendeMal();
        return Optional.ofNullable(gjeldendeMal).orElse(new MalData());
    }

    public List<MalData> hentMalList(String fnr) {
        return new SituasjonResolver(fnr, situasjonResolverDependencies).getMalList();
    }

    public MalData oppdaterMal(String mal, String fnr, String endretAv) {
        return new SituasjonResolver(fnr, situasjonResolverDependencies).oppdaterMal(mal, endretAv);
    }

    public OppfolgingStatusData startOppfolging(String fnr) {
        val situasjonResolver = new SituasjonResolver(fnr, situasjonResolverDependencies);
        situasjonResolver.sjekkStatusIArenaOgOppdaterSituasjon();
        if (situasjonResolver.getKanSettesUnderOppfolging()) {
            situasjonRepository.oppdaterSituasjon(situasjonResolver.getSitusjon().setOppfolging(true));
        }
        situasjonResolver.sjekkReservasjonIKrrOgOppdaterSituasjon();
        return new OppfolgingStatusData()
                .setFnr(fnr)
                .setUnderOppfolging(situasjonResolver.getSitusjon().isOppfolging())
                .setReservasjonKRR(situasjonResolver.reservertIKrr())
                .setManuell(situasjonResolver.manuell())
                .setOppfolgingUtgang(situasjonResolver.getSitusjon().getOppfolgingUtgang())
                .setVilkarMaBesvares(situasjonResolver.maVilkarBesvares())
                .setKanStarteOppfolging(situasjonResolver.getKanSettesUnderOppfolging());
    }

    public AvslutningStatusData hentAvslutningStatus(String fnr) throws Exception {
        val situasjonResolver = new SituasjonResolver(fnr, situasjonResolverDependencies);

        boolean erUnderOppfolging = situasjonResolver.erUnderOppfolgingIArena();
        boolean harPagaendeYtelser = harPagaendeYtelser(fnr);
        boolean harAktiveTiltalk = harAktiveTiltak(fnr);

        boolean kanAvslutte = situasjonResolver.getSitusjon().isOppfolging()
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

    private boolean harAktiveTiltak(String fnr) {
        return veilarbaktivtetService
                .hentArenaAktiviteter(fnr)
                .stream()
                .anyMatch(arenaAktivitetDTO -> arenaAktivitetDTO.getStatus() != AVBRUTT && arenaAktivitetDTO.getStatus() != FULLFORT);
    }

    @SneakyThrows
    private boolean harPagaendeYtelser(String fnr) {
        val wsHentYtelseskontraktListeRequest = new WSHentYtelseskontraktListeRequest();
        wsHentYtelseskontraktListeRequest.setPersonidentifikator(fnr);
        val wsHentYtelseskontraktListeResponse = ytelseskontraktV3.hentYtelseskontraktListe(wsHentYtelseskontraktListeRequest);
        return !wsHentYtelseskontraktListeResponse.getYtelseskontraktListe().isEmpty();
    }

    private String hentAktorId(String fnr) {
        return ofNullable(aktoerIdService.findAktoerId(fnr))
                .orElseThrow(() -> new IllegalArgumentException("Fant ikke aktør for fnr: " + fnr));
    }


}
