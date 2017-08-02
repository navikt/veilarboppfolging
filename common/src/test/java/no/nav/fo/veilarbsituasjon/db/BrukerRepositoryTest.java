package no.nav.fo.veilarbsituasjon.db;

import no.nav.fo.veilarbsituasjon.IntegrasjonsTest;
import no.nav.fo.veilarbsituasjon.rest.domain.OppfolgingBruker;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

public class BrukerRepositoryTest extends IntegrasjonsTest {

    private static final String AKTOR_ID = "2222";

    private BrukerRepository brukerRepository = new BrukerRepository(getBean(JdbcTemplate.class));

    @Test
    public void skalLeggeTilBruker() {
        brukerRepository.upsertVeilederTilordning(new OppfolgingBruker()
                .setAktoerid(AKTOR_ID)
                .setVeileder("***REMOVED***"));
        assertThat(brukerRepository.hentVeilederForAktoer(AKTOR_ID), is("***REMOVED***"));
    }

    @Test
    public void skalOppdatereBrukerDersomDenFinnes() {
        String aktoerid = "1111111";
        brukerRepository.upsertVeilederTilordning(new OppfolgingBruker()
                .setAktoerid(aktoerid)
                .setVeileder("***REMOVED***"));
        brukerRepository.upsertVeilederTilordning(new OppfolgingBruker()
                .setAktoerid(aktoerid)
                .setVeileder("***REMOVED***"));

        assertThat(brukerRepository.hentVeilederForAktoer(aktoerid), is("***REMOVED***"));
    }

}