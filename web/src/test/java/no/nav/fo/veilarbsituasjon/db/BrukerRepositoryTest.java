package no.nav.fo.veilarbsituasjon.db;

import no.nav.fo.veilarbsituasjon.IntegrasjonsTest;
import no.nav.fo.veilarbsituasjon.domain.OppfolgingBruker;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;


import static org.assertj.core.api.Java6Assertions.assertThat;


public class BrukerRepositoryTest extends IntegrasjonsTest {

    private JdbcTemplate db = getBean(JdbcTemplate.class);

    private BrukerRepository brukerRepository = new BrukerRepository(db);

    @Nested
    class leggTilEllerOppdaterBruker {
        String aktoerid = "111111";
        @Test
        public void skalLeggeTilBruker() {
            brukerRepository.leggTilEllerOppdaterBruker(new OppfolgingBruker()
                    .setAktoerid(aktoerid)
                    .setVeileder("***REMOVED***"));
            assertThat(brukerRepository.hentVeilederForAktoer(aktoerid)).isEqualTo("***REMOVED***");
        }

        @Test
        public void skalOppdatereBrukerDersomDenFinnes() {
            String aktoerid = "1111111";
            brukerRepository.leggTilEllerOppdaterBruker(new OppfolgingBruker()
                    .setAktoerid(aktoerid)
                    .setVeileder("***REMOVED***"));
            brukerRepository.leggTilEllerOppdaterBruker(new OppfolgingBruker()
                    .setAktoerid(aktoerid)
                    .setVeileder("***REMOVED***"));

            assertThat(brukerRepository.hentVeilederForAktoer(aktoerid)).isEqualTo("***REMOVED***");
        }
    }
}
