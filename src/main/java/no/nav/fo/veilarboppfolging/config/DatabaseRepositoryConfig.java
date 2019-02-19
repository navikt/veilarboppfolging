package no.nav.fo.veilarboppfolging.config;

import no.nav.fo.veilarboppfolging.db.*;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({
        OppfolgingRepository.class,
        OppfolgingsStatusRepository.class,
        OppfolgingsPeriodeRepository.class,
        MaalRepository.class,
        ManuellStatusRepository.class,
        BrukervilkarRepository.class,
        EskaleringsvarselRepository.class,
        KvpRepository.class,
        OppfolgingFeedRepository.class,
        NyeBrukereFeedRepository.class,
        VeilederTilordningerRepository.class
})
public class DatabaseRepositoryConfig { }
