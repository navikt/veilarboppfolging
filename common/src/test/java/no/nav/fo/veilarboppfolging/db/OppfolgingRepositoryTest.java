package no.nav.fo.veilarboppfolging.db;

import no.nav.fo.IntegrasjonsTest;
import no.nav.fo.veilarboppfolging.domain.*;
import no.nav.sbl.jdbc.Database;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static java.lang.System.currentTimeMillis;
import static no.nav.fo.veilarboppfolging.domain.VilkarStatus.GODKJENT;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

public class OppfolgingRepositoryTest extends IntegrasjonsTest {

    private static final String AKTOR_ID = "2222";
    private static final String SAKSBEHANDLER_ID = "Z990000";
    public static final String BEGRUNNELSE = "Begrunnelse";

    private Database db = getBean(Database.class);

    private OppfolgingRepository oppfolgingRepository = new OppfolgingRepository(db);

    private KvpRepository kvpRepository = new KvpRepository(db);

    @Nested
    class kvp {

        @Test
        public void startKvp() {
            gittOppfolgingForAktor(AKTOR_ID);
            start_kvp();

            assertThat(hentGjeldendeKvp(AKTOR_ID).getOpprettetBegrunnelse(), is(BEGRUNNELSE));
        }

        @Test
        public void stopKvp() {
            gittOppfolgingForAktor(AKTOR_ID);
            start_kvp();
            stop_kvp();

            assertThat(hentGjeldendeKvp(AKTOR_ID), nullValue());
        }

        @Test
        public void hentKvpHistorikk() {
            gittOppfolgingForAktor(AKTOR_ID);
            start_kvp();
            stop_kvp();
            start_kvp();
            stop_kvp();

            assertThat(kvpRepository.hentKvpHistorikk(AKTOR_ID), hasSize(2));
        }

        private void stop_kvp() {
            kvpRepository.stopKvp(AKTOR_ID, SAKSBEHANDLER_ID, BEGRUNNELSE);
        }

        private void start_kvp() {
            kvpRepository.startKvp(AKTOR_ID, "0123", SAKSBEHANDLER_ID, BEGRUNNELSE);
        }

        private Kvp hentGjeldendeKvp(String aktorId) {
            return oppfolgingRepository.hentOppfolging(aktorId).get().getGjeldendeKvp();
        }
    }

    @Nested
    class mal {
        @Test
        public void opprettOghentMal() {
            gittOppfolgingForAktor(AKTOR_ID);
            opprettMal(AKTOR_ID, "Dette er et mål");

            MalData mal = getGjeldendeMal(AKTOR_ID);
            assertThat(mal.getAktorId(), equalTo(AKTOR_ID));
            assertThat(mal.getMal(), equalTo("Dette er et mål"));
        }

        @Test
        public void hentMalListe() {
            gittOppfolgingForAktor(AKTOR_ID);
            opprettMal(AKTOR_ID, "Dette er et mål");
            opprettMal(AKTOR_ID, "Dette er et oppdatert mål");

            MalData mal = getGjeldendeMal(AKTOR_ID);
            assertThat(mal.getMal(), equalTo("Dette er et oppdatert mål"));
            List<MalData> malList = hentMal(AKTOR_ID);
            assertThat(malList.size(), greaterThan(1));
        }

        @Test
        public void slettMalForAktorEtter() {
            gittOppfolgingForAktor(AKTOR_ID);

            Date forOpprettelse = new Date(System.currentTimeMillis() - 1);
            opprettMal(AKTOR_ID, "Dette er et mål");
            Date etterOpprettelse = new Date(System.currentTimeMillis() + 1);

            oppfolgingRepository.slettMalForAktorEtter(AKTOR_ID, etterOpprettelse);
            sjekk_at_aktor_har_mal();

            oppfolgingRepository.slettMalForAktorEtter("annenAktor", forOpprettelse);
            sjekk_at_aktor_har_mal();

            oppfolgingRepository.slettMalForAktorEtter(AKTOR_ID, forOpprettelse);
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
            return oppfolgingRepository.hentOppfolging(aktorId).get().getGjeldendeMal();
        }

        private List<MalData> hentMal(String aktorId) {
            return oppfolgingRepository.hentMalList(aktorId);
        }
    }

    @Nested
    class hentOppfolging {
        @Test
        public void manglerOppfolging() throws Exception {
            sjekkAtOppfolgingMangler(hentOppfolging("ukjentAktorId"));
            sjekkAtOppfolgingMangler(hentOppfolging(null));
        }
    }

    @Nested
    class oppdaterOppfolging {
        @Test
        public void kanHenteForventetOppfolging() throws Exception {
            oppfolgingRepository.opprettOppfolging(AKTOR_ID);
            oppfolgingRepository.startOppfolgingHvisIkkeAlleredeStartet(AKTOR_ID);
            Oppfolging uthentetOppfolging = hentOppfolging(AKTOR_ID).get();
            assertThat(uthentetOppfolging.getAktorId(), equalTo(AKTOR_ID));
            assertThat(uthentetOppfolging.isUnderOppfolging(), is(true));
            assertThat(uthentetOppfolging.getOppfolgingsperioder().size(), is(1));
        }

        @Test
        public void oppdatererStatus() throws Exception {
            gittOppfolgingForAktor(AKTOR_ID);

            Brukervilkar brukervilkar = new Brukervilkar(
                    AKTOR_ID,
                    new Timestamp(currentTimeMillis()),
                    VilkarStatus.GODKJENT,
                    "Vilkårstekst",
                    "Vilkårshash"
            );
            oppfolgingRepository.opprettBrukervilkar(brukervilkar);

            Optional<Oppfolging> uthentetOppfolging = hentOppfolging(AKTOR_ID);
            assertThat(brukervilkar, equalTo(uthentetOppfolging.get().getGjeldendeBrukervilkar()));
        }
    }

    @Nested
    class avsluttOppfolging {

        @Test
        public void avsluttOppfolgingResetterVeileder_Manuellstatus_Mal_Og_Vilkar() throws Exception {
            oppfolgingRepository.opprettOppfolging(AKTOR_ID);
            oppfolgingRepository.startOppfolgingHvisIkkeAlleredeStartet(AKTOR_ID);
            String veilederId = "veilederId";
            String maal = "Mål";
            settVeileder(veilederId, AKTOR_ID);
            oppfolgingRepository.opprettManuellStatus(new ManuellStatus(AKTOR_ID, true, new Timestamp(currentTimeMillis()), "Test", KodeverkBruker.SYSTEM, null));
            oppfolgingRepository.opprettMal(new MalData().setAktorId(AKTOR_ID).setMal(maal).setEndretAv("bruker").setDato(new Timestamp(currentTimeMillis())));
            String hash = "123";
            oppfolgingRepository.opprettBrukervilkar(new Brukervilkar().setAktorId(AKTOR_ID).setHash(hash).setVilkarstatus(GODKJENT));
            Oppfolging oppfolging = hentOppfolging(AKTOR_ID).get();
            assertThat(oppfolging.isUnderOppfolging(), is(true));
            assertThat(oppfolging.getVeilederId(), equalTo(veilederId));
            assertThat(oppfolging.getGjeldendeManuellStatus().isManuell(), is(true));
            assertThat(oppfolging.getGjeldendeMal().getMal(), equalTo(maal));
            assertThat(oppfolging.getGjeldendeBrukervilkar().getHash(), equalTo(hash));

            oppfolgingRepository.avsluttOppfolging(AKTOR_ID, veilederId, "Funnet arbeid");
            Oppfolging avsluttetOppfolging = hentOppfolging(AKTOR_ID).get();
            assertThat(avsluttetOppfolging.isUnderOppfolging(), is(false));
            assertThat(avsluttetOppfolging.getVeilederId(), nullValue());
            assertThat(avsluttetOppfolging.getGjeldendeManuellStatus(), nullValue());
            assertThat(avsluttetOppfolging.getGjeldendeMal(), nullValue());
            assertThat(avsluttetOppfolging.getGjeldendeBrukervilkar(), nullValue());

            List<Oppfolgingsperiode> oppfolgingsperioder = avsluttetOppfolging.getOppfolgingsperioder();
            assertThat(oppfolgingsperioder.size(), is(1));


        }

    }

    @Nested
    class oppfolgingsperiode {

        @Test
        public void kanHenteOppfolgingMedIngenOppfolgingsperioder() throws Exception {
            oppfolgingRepository.opprettOppfolging(AKTOR_ID);
            Oppfolging oppfolging = oppfolgingRepository.hentOppfolging(AKTOR_ID).get();
            assertThat(oppfolging.getOppfolgingsperioder(), empty());
        }

        @Test
        public void kanHenteOppfolgingMedOppfolgingsperioder() throws Exception {
            oppfolgingRepository.opprettOppfolging(AKTOR_ID);
            oppfolgingRepository.startOppfolgingHvisIkkeAlleredeStartet(AKTOR_ID);
            List<Oppfolgingsperiode> oppfolgingsperioder = oppfolgingRepository.hentOppfolging(AKTOR_ID).get().getOppfolgingsperioder();
            assertThat(oppfolgingsperioder, hasSize(1));
            assertThat(oppfolgingsperioder.get(0).getStartDato(), not(nullValue()));
            assertThat(oppfolgingsperioder.get(0).getSluttDato(), nullValue());

            oppfolgingRepository.avsluttOppfolging(AKTOR_ID, "veileder", "begrunnelse");
            oppfolgingsperioder = oppfolgingRepository.hentOppfolging(AKTOR_ID).get().getOppfolgingsperioder();
            assertThat(oppfolgingsperioder, hasSize(1));
            assertThat(oppfolgingsperioder.get(0).getSluttDato(), not(nullValue()));

            oppfolgingRepository.startOppfolgingHvisIkkeAlleredeStartet(AKTOR_ID);
            oppfolgingsperioder = oppfolgingRepository.hentOppfolging(AKTOR_ID).get().getOppfolgingsperioder();
            assertThat(oppfolgingsperioder, hasSize(2));
        }

    }


    @Nested
    class oppfolgingMedVeileder {

        @Test
        public void utenVeileder() throws Exception {
            oppfolgingRepository.opprettOppfolging(AKTOR_ID);
            Oppfolging oppfolging = oppfolgingRepository.hentOppfolging(AKTOR_ID).get();
            assertThat(oppfolging.getVeilederId(), nullValue());
        }

        @Test
        public void medVeilederPaaNyBruker() throws Exception {
            String veilederId = "veilederId";
            oppfolgingRepository.opprettOppfolging(AKTOR_ID);
            settVeileder(veilederId, AKTOR_ID);
            Oppfolging oppfolging = oppfolgingRepository.hentOppfolging(AKTOR_ID).get();
            assertThat(oppfolging.getVeilederId(), equalTo(veilederId));
        }

    }

    //Setter veileder direkte vha. sql, siden det ikke finnes funksjonalitet for tildeling av veileder i
    //OppfolgingRepository. Dette finnes kun i OppfolgingFeedRepository (og tilbys i PortefoljeRessurs) p.t.
    //Men siden hentOppfolging henter opp veilder er det likevel aktuelt å teste her at veileder returneres
    //dersom det er satt i databasen. 
    private void settVeileder(String veilederId, String aktorId) {
        db.update("UPDATE OPPFOLGINGSTATUS SET VEILEDER = ? where aktor_id = ?", veilederId, aktorId);
    }

    private Oppfolging gittOppfolgingForAktor(String aktorId) {
        Oppfolging oppfolging = oppfolgingRepository.hentOppfolging(aktorId)
                .orElseGet(() -> oppfolgingRepository.opprettOppfolging(aktorId));

        oppfolgingRepository.startOppfolgingHvisIkkeAlleredeStartet(aktorId);
        oppfolging.setUnderOppfolging(true);
        return oppfolging;
    }

    private void sjekkAtOppfolgingMangler(Optional<Oppfolging> oppfolging) {
        assertThat(oppfolging.isPresent(), is(false));
    }

    private Optional<Oppfolging> hentOppfolging(String ukjentAktorId) {
        return oppfolgingRepository.hentOppfolging(ukjentAktorId);
    }

    private void opprettMal(String aktorId, String mal) {
        MalData input = new MalData()
                .setAktorId(aktorId)
                .setMal(mal)
                .setEndretAv(aktorId)
                .setDato(new Timestamp(System.currentTimeMillis()));
        oppfolgingRepository.opprettMal(input);
    }
}