package no.nav.fo.veilarboppfolging.db;

import no.nav.fo.veilarboppfolging.domain.AktorId;
import no.nav.fo.veilarboppfolging.domain.BrukerRegistrering;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import static java.lang.System.currentTimeMillis;
import static no.nav.fo.veilarboppfolging.config.JndiLocalContextConfig.setupInMemoryDatabase;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class ArbeidssokerregistreringRepositoryTest {

    private JdbcTemplate db;
    private ArbeidssokerregistreringRepository arbeidssokerregistreringRepository;

    @BeforeEach
    public void setup() {
        this.db = new JdbcTemplate(setupInMemoryDatabase());
        arbeidssokerregistreringRepository = new ArbeidssokerregistreringRepository(db);
    }

    @Test
    public void skalHenteOppfolgingsflaggSomErTrue() {
        db.execute("INSERT INTO OPPFOLGINGSTATUS (AKTOR_ID, UNDER_OPPFOLGING) VALUES ('11111','1')");
        db.execute("INSERT INTO OPPFOLGINGSTATUS (AKTOR_ID, UNDER_OPPFOLGING) VALUES ('11112','0')");
        boolean oppfolgingsflagg = arbeidssokerregistreringRepository.erOppfolgingsflaggSatt(new AktorId("11111"));
        assertThat(oppfolgingsflagg).isTrue();
    }

    @Test
    public void skalHenteOppfolgingsflaggSomErFalse() {
        db.execute("INSERT INTO OPPFOLGINGSTATUS (AKTOR_ID, UNDER_OPPFOLGING) VALUES ('11111','0')");
        boolean oppfolgingsflagg = arbeidssokerregistreringRepository.erOppfolgingsflaggSatt(new AktorId("11111"));
        assertThat(oppfolgingsflagg).isFalse();
    }

    @Test
    public void oppfolgingsflaggSkalVaereFalseNaarIkkeSatt() {
        boolean oppfolgingsflagg = arbeidssokerregistreringRepository.erOppfolgingsflaggSatt(new AktorId("11111"));
        assertThat(oppfolgingsflagg).isFalse();
    }

    @Test
    public void registrerBruker() {
        BrukerRegistrering bruker = new BrukerRegistrering()
                .setAktorId(new AktorId("11111").getAktorId())
                .setNusKode("123")
                .setYrkesPraksis("12345")
                .setEnigIOppsummering(true)
                .setOppsummering("Test test oppsummering")
                .setUtdanningBestatt(true)
                .setUtdanningGodkjentNorge(true)
                .setHarJobbetSammenhengende(true)
                .setHarHelseutfordringer(true)
                .setSituasjon("MISTET_JOBB");

        BrukerRegistrering registrertBruker = arbeidssokerregistreringRepository.registrerBruker(bruker);

        assertThat(registrertBruker.getNusKode()).isEqualTo(bruker.getNusKode());
        assertThat(registrertBruker.getYrkesPraksis()).isEqualTo(bruker.getYrkesPraksis());
        assertThat(registrertBruker.isEnigIOppsummering()).isEqualTo(bruker.isEnigIOppsummering());
        assertThat(registrertBruker.getOppsummering()).isEqualTo(bruker.getOppsummering());
        assertThat(registrertBruker.isUtdanningBestatt()).isEqualTo(bruker.isUtdanningBestatt());
        assertThat(registrertBruker.isUtdanningGodkjentNorge()).isEqualTo(bruker.isUtdanningGodkjentNorge());
        assertThat(registrertBruker.isHarJobbetSammenhengende()).isEqualTo(bruker.isHarJobbetSammenhengende());
        assertThat(registrertBruker.isHarHelseutfordringer()).isEqualTo(bruker.isHarHelseutfordringer());
        assertThat(registrertBruker.getSituasjon()).isEqualTo(bruker.getSituasjon());
    }
}