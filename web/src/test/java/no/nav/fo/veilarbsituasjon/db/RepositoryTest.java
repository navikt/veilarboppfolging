package no.nav.fo.veilarbsituasjon.db;

import com.google.common.base.Joiner;
import no.nav.fo.veilarbsituasjon.IntegrasjonsTest;
import no.nav.fo.veilarbsituasjon.domain.*;
import org.apache.commons.io.IOUtils;
import org.assertj.core.api.Java6Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

import static java.lang.System.currentTimeMillis;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class RepositoryTest extends IntegrasjonsTest {

    private static final String AKTOR_ID = "2222";

    private JdbcTemplate db = getBean(JdbcTemplate.class);

    private SituasjonRepository situasjonRepository = new SituasjonRepository(db);
    private BrukerRepository brukerRepository = new BrukerRepository(db);

    @Nested
    class hentSituasjon {
        @Test
        public void manglerSituasjon() throws Exception {
            sjekkAtSituasjonMangler(hentSituasjon("ukjentAktorId"));
            sjekkAtSituasjonMangler(hentSituasjon(null));
        }
    }

    @Nested
    class oppdaterSituasjon {
        @Test
        public void kanHenteSammeSituasjon() throws Exception {
            Situasjon situasjon = gittSituasjonForAktor(AKTOR_ID);
            Optional<Situasjon> uthentetSituasjon = hentSituasjon(AKTOR_ID);
            sjekkLikeSituasjoner(situasjon, uthentetSituasjon);
        }

        @Test
        public void oppdatererStatus() throws Exception {
            Situasjon situasjon = gittSituasjonForAktor(AKTOR_ID);
            situasjon.leggTilBrukervilkar(new Brukervilkar()
                    .setDato(new Timestamp(currentTimeMillis()))
                    .setTekst("hash")
                    .setVilkarstatus(VilkarStatus.GODKJENNT)
            );
            situasjonRepository.oppdaterSituasjon(situasjon);
            Optional<Situasjon> uthentetSituasjon = hentSituasjon(AKTOR_ID);
            sjekkLikeSituasjoner(situasjon, uthentetSituasjon);
        }
    }


    @Nested
    class leggTilEllerOppdaterBruker {
        String aktoerid = "111111";
        @Test
        public void skalLeggeTilBruker() {
            brukerRepository.leggTilEllerOppdaterBruker(new OppfolgingBruker()
                    .setAktoerid(aktoerid)
                    .setVeileder("***REMOVED***"));
            Java6Assertions.assertThat(brukerRepository.hentVeilederForAktoer(aktoerid)).isEqualTo("***REMOVED***");
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

            Java6Assertions.assertThat(brukerRepository.hentVeilederForAktoer(aktoerid)).isEqualTo("***REMOVED***");
        }
    }

    @Nested
    class hentAlleVeiledertilordninger {
        @Test
        public void skalReturnereAlleVeiledertilordninger() {
            slettAlleVeiledertilordninger();
            insertVeiledertilordninger();
            List<OppfolgingBruker> brukere = brukerRepository.hentAlleVeiledertilordninger();
            Java6Assertions.assertThat(brukere.size()).isEqualTo(20);
        }
    }

    private void slettAlleVeiledertilordninger() {
        db.update("DELETE FROM AKTOER_ID_TO_VEILEDER");
    }

    private void insertVeiledertilordninger() {
        try {
            db.execute(Joiner.on("\n").join(IOUtils.readLines(RepositoryTest.class.getResourceAsStream("/insert-aktoerid-veileder-testdata.sql"))));
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void sjekkLikeSituasjoner(Situasjon oppdatertSituasjon, Optional<Situasjon> situasjon) {
        assertThat(oppdatertSituasjon, equalTo(situasjon.get()));
    }

    private Situasjon gittSituasjonForAktor(String aktorId) {
        Situasjon oppdatertSituasjon = new Situasjon().setAktorId(aktorId).setOppfolging(true);
        situasjonRepository.oppdaterSituasjon(oppdatertSituasjon);
        return oppdatertSituasjon;
    }

    private void sjekkAtSituasjonMangler(Optional<Situasjon> situasjon) {
        assertThat(situasjon.isPresent(), is(false));
    }

    private Optional<Situasjon> hentSituasjon(String ukjentAktorId) {
        return situasjonRepository.hentSituasjon(ukjentAktorId);
    }

}