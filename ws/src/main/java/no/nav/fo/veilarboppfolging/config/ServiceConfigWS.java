package no.nav.fo.veilarboppfolging.config;


import no.nav.apiapp.security.PepClient;
import no.nav.dialogarena.aktor.AktorService;
import no.nav.fo.veilarboppfolging.db.ArbeidssokerregistreringRepository;
import no.nav.fo.veilarboppfolging.db.NyeBrukereFeedRepository;
import no.nav.fo.veilarboppfolging.db.OppfolgingRepository;
import no.nav.fo.veilarboppfolging.services.ArbeidsforholdService;
import no.nav.fo.veilarboppfolging.services.ArenaOppfolgingService;
import no.nav.fo.veilarboppfolging.services.registrerBruker.BrukerRegistreringService;
import no.nav.fo.veilarboppfolging.services.registrerBruker.StartRegistreringStatusResolver;
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.binding.ArbeidsforholdV3;
import no.nav.tjeneste.virksomhet.behandlearbeidssoeker.v1.binding.BehandleArbeidssoekerV1;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ServiceConfigWS {

    @Bean
    ArbeidsforholdService arbeidsforholdService(ArbeidsforholdV3 arbeidsforholdV3) {
        return new ArbeidsforholdService(arbeidsforholdV3);
    }

    @Bean
    BrukerRegistreringService registrerBrukerService(
            ArbeidssokerregistreringRepository arbeidssokerregistreringRepository,
            AktorService aktorService,
            BehandleArbeidssoekerV1 behandleArbeidssoekerV1,
            RemoteFeatureConfig.OpprettBrukerIArenaFeature sjekkRegistrereBrukerArenaFeature,
            RemoteFeatureConfig.RegistreringFeature skalRegistrereBrukerGenerellFeature,
            OppfolgingRepository oppfolgingRepository,
            NyeBrukereFeedRepository nyeBrukereFeedRepository,
            StartRegistreringStatusResolver startRegistreringStatusResolver)
    {
        return new BrukerRegistreringService(
                arbeidssokerregistreringRepository,
                oppfolgingRepository,
                aktorService,
                behandleArbeidssoekerV1,
                sjekkRegistrereBrukerArenaFeature,
                skalRegistrereBrukerGenerellFeature,
                nyeBrukereFeedRepository,
                startRegistreringStatusResolver
        );
    }

    @Bean
    public StartRegistreringStatusResolver startRegistreringStatusResolver(
            AktorService aktorService,
            ArbeidssokerregistreringRepository arbeidssokerregistreringRepository,
            PepClient pepClient,
            ArenaOppfolgingService arenaOppfolgingService,
            ArbeidsforholdService arbeidsforholdService)
    {
        return new StartRegistreringStatusResolver(
                aktorService,
                arbeidssokerregistreringRepository,
                pepClient,
                arenaOppfolgingService,
                arbeidsforholdService
        );
    }
}
