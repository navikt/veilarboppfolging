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
import java.util.stream.Collectors;

import static java.lang.System.currentTimeMillis;

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

        return getOppfolgingStatusData(fnr, situasjonResolver);
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

        return getOppfolgingStatusData(fnr, situasjonResolver);
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

        return getOppfolgingStatusData(fnr, situasjonResolver);
    }

    public OppfolgingStatusData hentAvslutningStatus(String fnr) throws Exception {
        val situasjonResolver = new SituasjonResolver(fnr, situasjonResolverDependencies);

        return getOppfolgingStatusDataMedAvslutningStatus(fnr, situasjonResolver);
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
        return getOppfolgingStatusDataMedAvslutningStatus(fnr, situasjonResolver);
    }


    public List<AvsluttetOppfolgingFeedData> hentAvsluttetOppfolgingEtterDato(Timestamp timestamp) {
        return situasjonRepository.hentAvsluttetOppfolgingEtterDato(timestamp);
    }

    public OppfolgingStatusData oppdaterManuellStatus(String fnr, boolean manuell, String begrunnelse, KodeverkBruker opprettetAv, String opprettetAvBrukerId) {
        val resolver = new SituasjonResolver(fnr, situasjonResolverDependencies);

        if (!resolver.reservertIKrr() && resolver.manuell() != manuell) {
            val nyStatus = new Status(resolver.getAktorId(), manuell, new Timestamp(currentTimeMillis()), begrunnelse, opprettetAv, opprettetAvBrukerId);
            situasjonRepository.opprettStatus(nyStatus);
            resolver.reloadSituasjon();
        }

        return getOppfolgingStatusData(fnr, resolver);
    }


    public List<InnstillingsHistorikk> hentInstillingsHistorikk(String fnr) {
        val resolver = new SituasjonResolver(fnr, situasjonResolverDependencies);
        val manuellHistorikk = situasjonRepository.hentManuellHistorikk(resolver.getAktorId());

        return manuellHistorikk.stream()
                .map((h) -> InnstillingsHistorikk.builder()
                        .beskrivelse("MANUELL")
                        .begrunnelse(h.getBegrunnelse())
                        .tidspunkt(h.getDato())
                        .opptettetAv(h.getOpprettetAv())
                        .opprettetAvBrukerId(h.getOpprettetAvBrukerId())
                        .build())
                .collect(Collectors.toList());
    }

    private OppfolgingStatusData getOppfolgingStatusData(String fnr, SituasjonResolver situasjonResolver) {
        return getOppfolgingStatusData(fnr, situasjonResolver, null);
    }

    private OppfolgingStatusData getOppfolgingStatusData(String fnr, SituasjonResolver situasjonResolver, AvslutningStatusData avslutningStatusData) {
        Situasjon situasjon = situasjonResolver.getSituasjon();
        return new OppfolgingStatusData()
                .setFnr(fnr)
                .setUnderOppfolging(situasjonResolver.getSituasjon().isOppfolging())
                .setReservasjonKRR(situasjonResolver.reservertIKrr())
                .setManuell(situasjonResolver.manuell())
                .setOppfolgingUtgang(situasjonResolver.getSituasjon().getOppfolgingUtgang())
                .setVilkarMaBesvares(situasjonResolver.maVilkarBesvares())
                .setKanStarteOppfolging(situasjonResolver.getKanSettesUnderOppfolging())
                .setAvslutningStatusData(avslutningStatusData)
                .setOppfolgingsperioder(situasjon.getOppfolgingsperioder())
                ;
    }

    private OppfolgingStatusData getOppfolgingStatusDataMedAvslutningStatus(String fnr, SituasjonResolver situasjonResolver) {
        val avslutningStatusData = AvslutningStatusData.builder()
                .kanAvslutte(situasjonResolver.kanAvslutteOppfolging())
                .underOppfolging(situasjonResolver.erUnderOppfolgingIArena())
                .harYtelser(situasjonResolver.harPagaendeYtelse())
                .harTiltak(situasjonResolver.harAktiveTiltak())
                .inaktiveringsDato(situasjonResolver.getInaktiveringsDato())
                .build();

        return getOppfolgingStatusData(fnr, situasjonResolver, avslutningStatusData);
    }
}
