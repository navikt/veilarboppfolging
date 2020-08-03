package no.nav.veilarboppfolging.repository;

import no.nav.veilarboppfolging.domain.Oppfolgingsbruker;
import no.nav.veilarboppfolging.feed.NyeBrukereFeedDTO;
import no.nav.veilarboppfolging.test.DbTestUtils;
import no.nav.veilarboppfolging.test.LocalH2Database;
import org.junit.Before;
import org.junit.jupiter.api.Test;

import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class NyeBrukereFeedRepositoryTest {

    private NyeBrukereFeedRepository nyeBrukereFeedRepository = new NyeBrukereFeedRepository(LocalH2Database.getDb());

    @Before
    public void cleanup() {
        DbTestUtils.cleanupTestDb();
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