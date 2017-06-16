package no.nav.fo.veilarbsituasjon.db;

import no.nav.fo.veilarbsituasjon.IntegrasjonsTest;
import no.nav.fo.veilarbsituasjon.domain.*;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

import static java.lang.System.currentTimeMillis;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

public class SituasjonRepositoryTest extends IntegrasjonsTest {

    private static final String AKTOR_ID = "2222";

    private JdbcTemplate db = getBean(JdbcTemplate.class);

    private SituasjonRepository situasjonRepository = new SituasjonRepository(db);

    @Nested
    class mal {
        @Test
        public void opprettOghentMal() {
            gittSituasjonForAktor(AKTOR_ID);
            gittMal(AKTOR_ID, "Dette er et mål");

            MalData mal = situasjonRepository.hentSituasjon(AKTOR_ID).get().getGjeldendeMal();
            assertThat(mal.getAktorId(), equalTo(AKTOR_ID));
            assertThat(mal.getMal(), equalTo("Dette er et mål"));
        }

        @Test
        public void hentMalListe() {
            gittSituasjonForAktor(AKTOR_ID);
            gittMal(AKTOR_ID, "Dette er et mål");
            gittMal(AKTOR_ID, "Dette er et oppdatert mål");

            MalData mal = situasjonRepository.hentSituasjon(AKTOR_ID).get().getGjeldendeMal();
            assertThat(mal.getMal(), equalTo("Dette er et oppdatert mål"));
            List<MalData> malList = situasjonRepository.hentMalList(AKTOR_ID);
            assertThat(malList.size(), greaterThan(1));
        }
    }

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
            Situasjon situasjon = new Situasjon().setAktorId("0001").setOppfolging(true);
            situasjonRepository.opprettSituasjon(situasjon);
            Optional<Situasjon> uthentetSituasjon = hentSituasjon("0001");
            sjekkLikeSituasjoner(situasjon, uthentetSituasjon);
        }

        @Test
        public void oppdatererStatus() throws Exception {
            Situasjon situasjon = gittSituasjonForAktor(AKTOR_ID);
            situasjonRepository.oppdaterSituasjon(situasjon);

            Brukervilkar brukervilkar = new Brukervilkar(
                    AKTOR_ID,
                    new Timestamp(currentTimeMillis()),
                    VilkarStatus.GODKJENT,
                    "Vilkårstekst",
                    "Vilkårshash"
            );
            situasjonRepository.opprettBrukervilkar(brukervilkar);

            Optional<Situasjon> uthentetSituasjon = hentSituasjon(AKTOR_ID);
            assertThat(brukervilkar, equalTo(uthentetSituasjon.get().getGjeldendeBrukervilkar()));
        }
    }

    private void sjekkLikeSituasjoner(Situasjon oprinneligSituasjon, Optional<Situasjon> situasjon) {
        assertThat(oprinneligSituasjon, equalTo(situasjon.get()));
    }

    private Situasjon gittSituasjonForAktor(String aktorId) {
        Situasjon oppdatertSituasjon = new Situasjon().setAktorId(aktorId).setOppfolging(true);
        if (situasjonRepository.situasjonFinnes(oppdatertSituasjon)) {
            situasjonRepository.oppdaterSituasjon(oppdatertSituasjon);
        } else {
            situasjonRepository.opprettSituasjon(oppdatertSituasjon);
        }
        return oppdatertSituasjon;
    }

    private void sjekkAtSituasjonMangler(Optional<Situasjon> situasjon) {
        assertThat(situasjon.isPresent(), is(false));
    }

    private Optional<Situasjon> hentSituasjon(String ukjentAktorId) {
        return situasjonRepository.hentSituasjon(ukjentAktorId);
    }

    private void gittMal(String aktorId, String mal) {
        MalData input = new MalData()
                .setAktorId(aktorId)
                .setMal(mal)
                .setEndretAv(aktorId)
                .setDato(null);
        situasjonRepository.opprettMal(input);
    }
}