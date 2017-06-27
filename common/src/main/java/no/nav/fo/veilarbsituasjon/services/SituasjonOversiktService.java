package no.nav.fo.veilarbsituasjon.services;

import io.swagger.annotations.Api;
import lombok.SneakyThrows;
import lombok.val;
import no.nav.fo.veilarbsituasjon.db.SituasjonRepository;
import no.nav.fo.veilarbsituasjon.domain.*;
import no.nav.fo.veilarbsituasjon.services.SituasjonResolver.SituasjonResolverDependencies;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Inject;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Component
@Api
public class SituasjonOversiktService {

    @Inject
    private SituasjonResolverDependencies situasjonResolverDependencies;

    @Inject
    private SituasjonRepository situasjonRepository;

    @Transactional
    public OppfolgingStatusData hentOppfolgingsStatus(String fnr) throws Exception {
        val situasjonResolver = new SituasjonResolver(fnr, situasjonResolverDependencies);

        situasjonResolver.sjekkStatusIArenaOgOppdaterSituasjon();

        Situasjon situasjon = situasjonResolver.getSituasjon();
        return new OppfolgingStatusData()
                .setFnr(fnr)
                .setUnderOppfolging(situasjon.isOppfolging())
                .setReservasjonKRR(situasjonResolver.reservertIKrr())
                .setManuell(situasjonResolver.manuell())
                .setOppfolgingUtgang(situasjon.getOppfolgingUtgang())
                .setVilkarMaBesvares(situasjonResolver.maVilkarBesvares())
                .setKanStarteOppfolging(situasjonResolver.getKanSettesUnderOppfolging())
                .setOppfolgingsperioder(situasjon.getOppfolgingsperioder());
    }

    public Brukervilkar hentVilkar(String fnr) throws Exception {
        return new SituasjonResolver(fnr, situasjonResolverDependencies).getNyesteVilkar();
    }

    public List<Brukervilkar> hentHistoriskeVilkar(String fnr) {
        return new SituasjonResolver(fnr, situasjonResolverDependencies).getHistoriskeVilkar();
    }

    @Transactional
    public OppfolgingStatusData oppdaterVilkaar(String hash, String fnr, VilkarStatus vilkarStatus) throws Exception {
        val situasjonResolver = new SituasjonResolver(fnr, situasjonResolverDependencies);

        situasjonResolver.sjekkNyesteVilkarOgOppdaterSituasjon(hash, vilkarStatus);

        return new OppfolgingStatusData()
                .setFnr(fnr)
                .setUnderOppfolging(situasjonResolver.getSituasjon().isOppfolging())
                .setReservasjonKRR(situasjonResolver.reservertIKrr())
                .setManuell(situasjonResolver.manuell())
                .setOppfolgingUtgang(situasjonResolver.getSituasjon().getOppfolgingUtgang())
                .setVilkarMaBesvares(situasjonResolver.maVilkarBesvares())
                .setKanStarteOppfolging(situasjonResolver.getKanSettesUnderOppfolging());
    }

    public MalData hentMal(String fnr) {
        MalData gjeldendeMal = new SituasjonResolver(fnr, situasjonResolverDependencies).getSituasjon().getGjeldendeMal();
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
        if (situasjonResolver.getKanSettesUnderOppfolging()) {
            situasjonResolver.startOppfolging();

        }
        return new OppfolgingStatusData()
                .setFnr(fnr)
                .setUnderOppfolging(situasjonResolver.getSituasjon().isOppfolging())
                .setReservasjonKRR(situasjonResolver.reservertIKrr())
                .setManuell(situasjonResolver.manuell())
                .setOppfolgingUtgang(situasjonResolver.getSituasjon().getOppfolgingUtgang())
                .setVilkarMaBesvares(situasjonResolver.maVilkarBesvares())
                .setKanStarteOppfolging(situasjonResolver.getKanSettesUnderOppfolging());
    }

    public OppfolgingStatusData hentAvslutningStatus(String fnr) throws Exception {
        val situasjonResolver = new SituasjonResolver(fnr, situasjonResolverDependencies);

        return getOppfolgingStatusData(fnr, situasjonResolver);
    }

    @SneakyThrows
    public OppfolgingStatusData avsluttOppfolging(String fnr, String veileder, String begrunnelse) {
        val situasjonResolver = new SituasjonResolver(fnr, situasjonResolverDependencies);

        if (situasjonResolver.kanAvslutteOppfolging()) {
            val oppfolgingsperiode = Oppfolgingsperiode.builder()
                    .aktorId(situasjonResolver.getAktorId())
                    .veileder(veileder)
                    .sluttDato(new Date())
                    .begrunnelse(begrunnelse)
                    .build();
            situasjonResolver.avsluttOppfolging(oppfolgingsperiode);
        }

        situasjonResolver.reloadSituasjon();
        return getOppfolgingStatusData(fnr, situasjonResolver);
    }

    private OppfolgingStatusData getOppfolgingStatusData(String fnr, SituasjonResolver situasjonResolver) {
        val avslutningStatusData = AvslutningStatusData.builder()
                .kanAvslutte(situasjonResolver.kanAvslutteOppfolging())
                .underOppfolging(situasjonResolver.erUnderOppfolgingIArena())
                .harYtelser(situasjonResolver.harPagaendeYtelse())
                .harTiltak(situasjonResolver.harAktiveTiltak())
                .inaktiveringsDato(situasjonResolver.getInaktiveringsDato())
                .build();

        Situasjon situasjon = situasjonResolver.getSituasjon();
        return new OppfolgingStatusData()
                .setFnr(fnr)
                .setUnderOppfolging(situasjon.isOppfolging())
                .setReservasjonKRR(situasjonResolver.reservertIKrr())
                .setManuell(situasjonResolver.manuell())
                .setOppfolgingUtgang(situasjon.getOppfolgingUtgang())
                .setVilkarMaBesvares(situasjonResolver.maVilkarBesvares())
                .setKanStarteOppfolging(situasjonResolver.getKanSettesUnderOppfolging())
                .setAvslutningStatusData(avslutningStatusData)
                .setOppfolgingsperioder(situasjon.getOppfolgingsperioder())
                ;
    }

    public List<AvsluttetOppfolgingFeedData> hentAvsluttetOppfolgingEtterDato(Timestamp timestamp) {
        return situasjonRepository.hentAvsluttetOppfolgingEtterDato(timestamp);
    }
}
