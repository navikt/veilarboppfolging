package no.nav.fo.veilarbsituasjon.db;

import no.nav.fo.veilarbsituasjon.IntegrasjonsTest;
import no.nav.fo.veilarbsituasjon.rest.domain.OppfolgingBruker;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class BrukerRepositoryTest extends IntegrasjonsTest {

    private static final String AKTOR_ID = "2222";

    private BrukerRepository brukerRepository = new BrukerRepository(getBean(JdbcTemplate.class));

    @Test
    public void skalLeggeTilBruker() {
        brukerRepository.upsertVeilederTilordning(
                OppfolgingBruker
                        .builder()
                        .aktoerid(AKTOR_ID)
                        .veileder("***REMOVED***")
                        .build());
        assertThat(brukerRepository.hentTilordningForAktoer(AKTOR_ID).getVeileder(), is("***REMOVED***"));
    }

    @Test
    public void skalOppdatereBrukerDersomDenFinnes() {
        String aktoerid = "1111111";
        brukerRepository.upsertVeilederTilordning(OppfolgingBruker
                .builder()
                .aktoerid(aktoerid)
                .veileder("***REMOVED***")
                .build());
        brukerRepository.upsertVeilederTilordning(OppfolgingBruker
                .builder()
                .aktoerid(aktoerid)
                .veileder("***REMOVED***")
                .build());

        assertThat(brukerRepository.hentTilordningForAktoer(aktoerid).getVeileder(), is("***REMOVED***"));
    }

}