package no.nav.fo.veilarboppfolging.config;


import no.nav.apiapp.security.PepClient;
import no.nav.dialogarena.aktor.AktorService;
import no.nav.fo.veilarboppfolging.db.ArbeidssokerregistreringRepository;
import no.nav.fo.veilarboppfolging.service.ReservertKrrService;
import no.nav.fo.veilarboppfolging.services.ArbeidsforholdService;
import no.nav.fo.veilarboppfolging.services.ArenaOppfolgingService;
import no.nav.fo.veilarboppfolging.services.DigitalKontaktinformasjonService;
import no.nav.fo.veilarboppfolging.services.registrerBruker.RegistrerBrukerService;
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
    RegistrerBrukerService registrerBrukerService(ArbeidssokerregistreringRepository arbeidssokerregistreringRepository,
                                                  PepClient pepClient,
                                                  AktorService aktorService,
                                                  ArenaOppfolgingService arenaOppfolgingService,
                                                  ArbeidsforholdService arbeidsforholdService,
                                                  BehandleArbeidssoekerV1 behandleArbeidssoekerV1) {
        return new RegistrerBrukerService(
                arbeidssokerregistreringRepository,
                pepClient,
                aktorService,
                arenaOppfolgingService,
                arbeidsforholdService,
                behandleArbeidssoekerV1
                );
    }

    @Bean
    ReservertKrrService reserverKrrService(DigitalKontaktinformasjonService digitalKontaktinformasjonService, PepClient pepClient) {
        return new ReservertKrrService(digitalKontaktinformasjonService, pepClient);
    }
}
