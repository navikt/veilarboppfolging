package no.nav.fo.veilarboppfolging.config;


import no.nav.apiapp.security.PepClient;
import no.nav.dialogarena.aktor.AktorService;
import no.nav.fo.veilarboppfolging.db.ArbeidssokerregistreringRepository;
import no.nav.fo.veilarboppfolging.service.ReservertKrrService;
import no.nav.fo.veilarboppfolging.services.ArbeidsforholdService;
import no.nav.fo.veilarboppfolging.services.ArenaOppfolgingService;
import no.nav.fo.veilarboppfolging.services.DigitalKontaktinformasjonService;
import no.nav.fo.veilarboppfolging.services.registrerBruker.RegistrerBrukerService;
import no.nav.fo.veilarboppfolging.services.startregistrering.StartRegistreringService;
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.binding.ArbeidsforholdV3;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ServiceConfigWS {

    @Bean
    ArbeidsforholdService arbeidsforholdService(ArbeidsforholdV3 arbeidsforholdV3) {
        return new ArbeidsforholdService(arbeidsforholdV3);
    }

    @Bean
    StartRegistreringService startRegistreringService(ArbeidssokerregistreringRepository arbeidssokerregistreringRepository,
                                                      PepClient pepClient,
                                                      AktorService aktorService,
                                                      ArenaOppfolgingService arenaOppfolgingService,
                                                      ArbeidsforholdService arbeidsforholdService) {
        return new StartRegistreringService(arbeidssokerregistreringRepository, pepClient, aktorService, arenaOppfolgingService, arbeidsforholdService);
    }

    @Bean
    RegistrerBrukerService registrerBrukerService(ArbeidssokerregistreringRepository arbeidssokerregistreringRepository,
                                                  PepClient  pepClient, AktorService aktorService) {
        return new RegistrerBrukerService(arbeidssokerregistreringRepository, pepClient, aktorService);
    }

    @Bean
    ReservertKrrService reserverKrrService(DigitalKontaktinformasjonService digitalKontaktinformasjonService, PepClient pepClient) {
        return new ReservertKrrService(digitalKontaktinformasjonService, pepClient);
    }
}
