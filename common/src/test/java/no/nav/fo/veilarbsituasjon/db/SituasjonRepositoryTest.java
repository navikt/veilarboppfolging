package no.nav.fo.veilarbsituasjon.db;

import no.nav.fo.IntegrasjonsTest;
import no.nav.fo.inject.Database;
import no.nav.fo.veilarbsituasjon.domain.*;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static java.lang.System.currentTimeMillis;
import static no.nav.fo.veilarbsituasjon.domain.VilkarStatus.GODKJENT;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

public class SituasjonRepositoryTest extends IntegrasjonsTest {

    private static final String AKTOR_ID = "2222";

    private Database db = getBean(Database.class);

    private SituasjonRepository situasjonRepository = new SituasjonRepository(db);

    @Nested
    class mal {
        @Test
        public void opprettOghentMal() {
            gittSituasjonForAktor(AKTOR_ID);
            opprettMal(AKTOR_ID, "Dette er et mål");

            MalData mal = getGjeldendeMal(AKTOR_ID);
            assertThat(mal.getAktorId(), equalTo(AKTOR_ID));
            assertThat(mal.getMal(), equalTo("Dette er et mål"));
        }

        @Test
        public void hentMalListe() {
            gittSituasjonForAktor(AKTOR_ID);
            opprettMal(AKTOR_ID, "Dette er et mål");
            opprettMal(AKTOR_ID, "Dette er et oppdatert mål");

            MalData mal = getGjeldendeMal(AKTOR_ID);
            assertThat(mal.getMal(), equalTo("Dette er et oppdatert mål"));
            List<MalData> malList = hentMal(AKTOR_ID);
            assertThat(malList.size(), greaterThan(1));
        }

        @Test
        public void slettMalForAktorEtter() {
            gittSituasjonForAktor(AKTOR_ID);

            Date forOpprettelse = new Date(System.currentTimeMillis() - 1);
            opprettMal(AKTOR_ID, "Dette er et mål");
            Date etterOpprettelse = new Date(System.currentTimeMillis() + 1);

            situasjonRepository.slettMalForAktorEtter(AKTOR_ID, etterOpprettelse);
            sjekk_at_aktor_har_mal();

            situasjonRepository.slettMalForAktorEtter("annenAktor", forOpprettelse);
            sjekk_at_aktor_har_mal();

            situasjonRepository.slettMalForAktorEtter(AKTOR_ID, forOpprettelse);
            sjekk_at_aktor_ikke_har_mal();
        }

        private void sjekk_at_aktor_ikke_har_mal() {
            assertThat(getGjeldendeMal(AKTOR_ID), nullValue());
            assertThat(hentMal(AKTOR_ID), empty());
        }

        private void sjekk_at_aktor_har_mal() {
            assertThat(hentMal(AKTOR_ID), not(empty()));
        }

        private MalData getGjeldendeMal(String aktorId) {
            return situasjonRepository.hentSituasjon(aktorId).get().getGjeldendeMal();
        }

        private List<MalData> hentMal(String aktorId) {
            return situasjonRepository.hentMalList(aktorId);
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
        public void kanHenteForventetSituasjon() throws Exception {
            situasjonRepository.opprettSituasjon(AKTOR_ID);
            situasjonRepository.startOppfolgingHvisIkkeAlleredeStartet(AKTOR_ID);
            Situasjon uthentetSituasjon = hentSituasjon(AKTOR_ID).get();
            assertThat(uthentetSituasjon.getAktorId(), equalTo(AKTOR_ID));
            assertThat(uthentetSituasjon.isOppfolging(), is(true));
            assertThat(uthentetSituasjon.getOppfolgingsperioder().size(), is(1));
        }

        @Test
        public void oppdatererStatus() throws Exception {
            gittSituasjonForAktor(AKTOR_ID);

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

    @Nested
    class avsluttOppfolging {

        @Test
        public void avsluttOppfolgingResetterVeileder_Manuellstatus_Mal_Og_Vilkar() throws Exception {
            situasjonRepository.opprettSituasjon(AKTOR_ID);
            situasjonRepository.startOppfolgingHvisIkkeAlleredeStartet(AKTOR_ID);
            String veilederId = "veilederId";
            String maal = "Mål";
            settVeileder(veilederId, AKTOR_ID);
            situasjonRepository.opprettStatus(new Status(AKTOR_ID, true, new Timestamp(currentTimeMillis()), "Test", KodeverkBruker.SYSTEM, null));
            situasjonRepository.opprettMal(new MalData().setAktorId(AKTOR_ID).setMal(maal).setEndretAv("bruker").setDato(new Timestamp(currentTimeMillis())));
            String hash = "123";
            situasjonRepository.opprettBrukervilkar(new Brukervilkar().setAktorId(AKTOR_ID).setHash(hash).setVilkarstatus(GODKJENT));
            Situasjon situasjon = hentSituasjon(AKTOR_ID).get();
            assertThat(situasjon.isOppfolging(), is(true));
            assertThat(situasjon.getVeilederId(), equalTo(veilederId));
            assertThat(situasjon.getGjeldendeStatus().isManuell(), is(true));
            assertThat(situasjon.getGjeldendeMal().getMal(), equalTo(maal));
            assertThat(situasjon.getGjeldendeBrukervilkar().getHash(), equalTo(hash));
            
            situasjonRepository.avsluttOppfolging(AKTOR_ID, veilederId, "Funnet arbeid");
            Situasjon avsluttetSituasjon = hentSituasjon(AKTOR_ID).get();
            assertThat(avsluttetSituasjon.isOppfolging(), is(false));
            assertThat(avsluttetSituasjon.getVeilederId(), nullValue());
            assertThat(avsluttetSituasjon.getGjeldendeStatus(), nullValue());
            assertThat(avsluttetSituasjon.getGjeldendeMal(), nullValue());
            assertThat(avsluttetSituasjon.getGjeldendeBrukervilkar(), nullValue());
            
            List<Oppfolgingsperiode> oppfolgingsperioder = avsluttetSituasjon.getOppfolgingsperioder();
            assertThat(oppfolgingsperioder.size(), is(1));
            
            
        }
        
    }
    
    @Nested
    class oppfolgingsperiode {

        @Test
        public void kanHenteSituasjonMedIngenOppfolgingsperioder() throws Exception {
            situasjonRepository.opprettSituasjon(AKTOR_ID);
            Situasjon situasjon = situasjonRepository.hentSituasjon(AKTOR_ID).get();
            assertThat(situasjon.getOppfolgingsperioder(), empty());
        }

        @Test
        public void kanHenteSituasjonMedOppfolgingsperioder() throws Exception {
            situasjonRepository.opprettSituasjon(AKTOR_ID);
            situasjonRepository.startOppfolgingHvisIkkeAlleredeStartet(AKTOR_ID);
            List<Oppfolgingsperiode> oppfolgingsperioder = situasjonRepository.hentSituasjon(AKTOR_ID).get().getOppfolgingsperioder();
            assertThat(oppfolgingsperioder, hasSize(1));
            assertThat(oppfolgingsperioder.get(0).getStartDato(), not(nullValue()));
            assertThat(oppfolgingsperioder.get(0).getSluttDato(), nullValue());
            
            situasjonRepository.avsluttOppfolging(AKTOR_ID, "veileder", "begrunnelse");
            oppfolgingsperioder = situasjonRepository.hentSituasjon(AKTOR_ID).get().getOppfolgingsperioder();
            assertThat(oppfolgingsperioder, hasSize(1));
            assertThat(oppfolgingsperioder.get(0).getSluttDato(), not(nullValue()));

            situasjonRepository.startOppfolgingHvisIkkeAlleredeStartet(AKTOR_ID);
            oppfolgingsperioder = situasjonRepository.hentSituasjon(AKTOR_ID).get().getOppfolgingsperioder();
            assertThat(oppfolgingsperioder, hasSize(2));
        }

    }


    @Nested
    class situasjonMedVeileder {

        @Test
        public void utenVeileder() throws Exception {
            situasjonRepository.opprettSituasjon(AKTOR_ID);
            Situasjon situasjon = situasjonRepository.hentSituasjon(AKTOR_ID).get();
            assertThat(situasjon.getVeilederId(), nullValue());
        }

        @Test
        public void medVeilederPaaNyBruker() throws Exception {
            String veilederId = "veilederId";
            situasjonRepository.opprettSituasjon(AKTOR_ID);
            settVeileder(veilederId, AKTOR_ID);
            Situasjon situasjon = situasjonRepository.hentSituasjon(AKTOR_ID).get();
            assertThat(situasjon.getVeilederId(), equalTo(veilederId));
        }

    }

    //Setter veileder direkte vha. sql, siden det ikke finnes funksjonalitet for tildeling av veileder i
    //situasjonRepository. Dette finnes kun i BrukerRepository (og tilbys i PortefoljeRessurs) p.t.
    //Men siden hentSituasjon henter opp veilder er det likevel aktuelt å teste her at veileder returneres 
    //dersom det er satt i databasen. 
    private void settVeileder(String veilederId, String aktorId) {
        db.update("UPDATE situasjon SET VEILEDER = ? where aktorid = ?", veilederId, aktorId);
    }

    private Situasjon gittSituasjonForAktor(String aktorId) {
        Situasjon situasjon = situasjonRepository.hentSituasjon(aktorId)
                .orElseGet(() -> situasjonRepository.opprettSituasjon(aktorId));

        situasjonRepository.startOppfolgingHvisIkkeAlleredeStartet(aktorId);
        situasjon.setOppfolging(true);
        return situasjon;
    }

    private void sjekkAtSituasjonMangler(Optional<Situasjon> situasjon) {
        assertThat(situasjon.isPresent(), is(false));
    }

    private Optional<Situasjon> hentSituasjon(String ukjentAktorId) {
        return situasjonRepository.hentSituasjon(ukjentAktorId);
    }

    private void opprettMal(String aktorId, String mal) {
        MalData input = new MalData()
                .setAktorId(aktorId)
                .setMal(mal)
                .setEndretAv(aktorId)
                .setDato(new Timestamp(System.currentTimeMillis()));
        situasjonRepository.opprettMal(input);
    }
}