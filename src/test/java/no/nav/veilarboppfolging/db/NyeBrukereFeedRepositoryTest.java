package no.nav.veilarboppfolging.db;

import no.nav.veilarboppfolging.feed.NyeBrukereFeedDTO;
import no.nav.veilarboppfolging.domain.Oppfolgingsbruker;
import no.nav.sbl.jdbc.Database;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static java.util.stream.Collectors.toList;
import static no.nav.veilarboppfolging.config.JndiLocalContextConfig.setupInMemoryDatabase;
import static org.assertj.core.api.Java6Assertions.assertThat;

class NyeBrukereFeedRepositoryTest {

    private static Database database;
    private static NyeBrukereFeedRepository nyeBrukereFeedRepository;

    @BeforeEach
    public void setup() {
        database = new Database(new JdbcTemplate(setupInMemoryDatabase()));
        nyeBrukereFeedRepository = new NyeBrukereFeedRepository(database);
    }

    @Test
    public void skalOppdatereFeedIdPaaAlleElementer() {
        Oppfolgingsbruker oppfolgingsbruker1 = Oppfolgingsbruker.builder().aktoerId("111111").build();
        Oppfolgingsbruker oppfolgingsbruker2 = Oppfolgingsbruker.builder().aktoerId("222222").build();

        nyeBrukereFeedRepository.leggTil(oppfolgingsbruker1);
        nyeBrukereFeedRepository.leggTil(oppfolgingsbruker2);

        int antall = nyeBrukereFeedRepository.leggTilFeedIdPaAlleElementerUtenFeedId();

        assertThat(antall).isEqualTo(2);

        List<NyeBrukereFeedDTO> nyeBrukerFeedElementer = nyeBrukereFeedRepository.hentElementerStorreEnnId("0", 500);

        List<Long> ids = nyeBrukerFeedElementer.stream().map(NyeBrukereFeedDTO::getId).collect(toList());
        assertThat(ids).containsExactly(1L, 2L);
    }

    @Test
    public void feedElementerSkalPagineresOgSorteresStigende() {
        leggTilElementer(20);
        nyeBrukereFeedRepository.leggTilFeedIdPaAlleElementerUtenFeedId();

        List<Long> ids1 = nyeBrukereFeedRepository.hentElementerStorreEnnId("3", 5).stream()
                .map(NyeBrukereFeedDTO::getId).collect(toList());

        List<Long> ids2 = nyeBrukereFeedRepository.hentElementerStorreEnnId("8", 5).stream()
                .map(NyeBrukereFeedDTO::getId).collect(toList());

        assertThat(ids1).containsExactly(4L, 5L, 6L, 7L, 8L);
        assertThat(ids2).containsExactly(9L, 10L, 11L, 12L, 13L);
    }

    private void leggTilElementer(int antall) {
        for(int i = 0; i < antall; i++) {
            nyeBrukereFeedRepository.leggTil(Oppfolgingsbruker.builder().aktoerId("111111").build());
        }
    }

}