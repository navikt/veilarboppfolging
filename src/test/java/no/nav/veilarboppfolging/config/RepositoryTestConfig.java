package no.nav.veilarboppfolging.config;

import no.nav.veilarboppfolging.repository.*;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({
        EskaleringsvarselRepository.class,
        KvpRepository.class,
        MaalRepository.class,
        ManuellStatusRepository.class,
        NyeBrukereFeedRepository.class,
        OppfolgingFeedRepository.class,
        OppfolgingsenhetHistorikkRepository.class,
        OppfolgingsPeriodeRepository.class,
        OppfolgingsStatusRepository.class,
        UtmeldingRepository.class,
        VeilederHistorikkRepository.class,
        VeilederTilordningerRepository.class,
        FeiletKafkaMeldingRepository.class
})
public class RepositoryTestConfig {}
