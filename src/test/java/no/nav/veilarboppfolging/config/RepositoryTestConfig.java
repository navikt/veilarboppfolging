package no.nav.veilarboppfolging.config;

import no.nav.veilarboppfolging.repository.*;
import no.nav.veilarboppfolging.service.OppfolgingRepositoryService;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({
        AvsluttOppfolgingEndringRepository.class,
        EskaleringsvarselRepository.class,
        KvpRepository.class,
        MaalRepository.class,
        ManuellStatusRepository.class,
        NyeBrukereFeedRepository.class,
        OppfolgingFeedRepository.class,
        OppfolgingRepositoryService.class,
        OppfolgingsenhetHistorikkRepository.class,
        OppfolgingsPeriodeRepository.class,
        OppfolgingsStatusRepository.class,
        UtmeldingRepository.class,
        VeilederHistorikkRepository.class,
        VeilederTilordningerRepository.class
})
public class RepositoryTestConfig {}