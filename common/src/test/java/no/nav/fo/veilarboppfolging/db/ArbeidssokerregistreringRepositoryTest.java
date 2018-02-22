package no.nav.fo.veilarboppfolging.db;

import no.nav.fo.veilarboppfolging.domain.AktorId;
import no.nav.fo.veilarboppfolging.domain.BrukerRegistrering;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Date;

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

        Date opprettetDato = new Date(System.currentTimeMillis());
        AktorId aktorId = new AktorId("11111");
        BrukerRegistrering bruker = new BrukerRegistrering(
          "nus12",
          "12345",
          opprettetDato,
          true,
          "Test test oppsummering",
          true,
          true,
          false,
                "MISTET_JOBBEN");

        BrukerRegistrering brukerRegistrering = arbeidssokerregistreringRepository.lagreBruker(bruker, aktorId);

        assertRegistrertBruker(bruker, brukerRegistrering);
    }

    private void assertRegistrertBruker(BrukerRegistrering bruker, BrukerRegistrering brukerRegistrering) {
        assertThat(brukerRegistrering.getNusKode()).isEqualTo(bruker.getNusKode());
        assertThat(brukerRegistrering.getYrkesPraksis()).isEqualTo(bruker.getYrkesPraksis());
        assertThat(brukerRegistrering.isEnigIOppsummering()).isEqualTo(bruker.isEnigIOppsummering());
        assertThat(brukerRegistrering.getOppsummering()).isEqualTo(bruker.getOppsummering());
        assertThat(brukerRegistrering.isUtdanningBestatt()).isEqualTo(bruker.isUtdanningBestatt());
        assertThat(brukerRegistrering.isUtdanningGodkjentNorge()).isEqualTo(bruker.isUtdanningGodkjentNorge());
        assertThat(brukerRegistrering.isHarHelseutfordringer()).isEqualTo(bruker.isHarHelseutfordringer());
        assertThat(brukerRegistrering.getSituasjon()).isEqualTo(bruker.getSituasjon());
    }
}