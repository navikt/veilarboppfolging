package no.nav.veilarboppfolging.repository;

import no.nav.veilarboppfolging.domain.Oppfolgingsperiode;
import no.nav.veilarboppfolging.test.IsolatedDatabaseTest;
import org.junit.Before;
import org.junit.Test;

import java.time.ZonedDateTime;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class OppfolgingsPeriodeRepositoryTest extends IsolatedDatabaseTest {

    private OppfolgingsPeriodeRepository oppfolgingsPeriodeRepository;
    private OppfolgingsStatusRepository oppfolgingsStatusRepository;

    @Before
    public void setup() {
        oppfolgingsPeriodeRepository = new OppfolgingsPeriodeRepository(db);
        oppfolgingsStatusRepository = new OppfolgingsStatusRepository(db);
    }

    @Test
    public void skal_hente_siste_gjeldende_oppfolgingsperiode() {
        String aktorId = "123";
        ZonedDateTime now = ZonedDateTime.now();
        oppfolgingsStatusRepository.opprettOppfolging(aktorId);
        insertOppfolgingsperiode(aktorId, now.minusDays(10), now.minusDays(9));
        insertOppfolgingsperiode(aktorId, now.minusDays(3).plusMinutes(2), null);
        insertOppfolgingsperiode(aktorId, now.minusDays(3), null);
        insertOppfolgingsperiode(aktorId, now.minusDays(12), now.minusDays(11));

        Optional<Oppfolgingsperiode> gjeldendeOppfolgingsperiode =
                oppfolgingsPeriodeRepository.hentGjeldendeOppfolgingsperiode(aktorId);

        assertTrue("Skal ha gjeldende oppfølgingsperiode", gjeldendeOppfolgingsperiode.isPresent());
        assertEquals(now.minusDays(3).plusMinutes(2), gjeldendeOppfolgingsperiode.get().getStartDato());

    }

    private void insertOppfolgingsperiode(String aktorId, ZonedDateTime startDato, ZonedDateTime sluttDato) {
        db.update(
                "INSERT INTO OPPFOLGINGSPERIODE(aktor_id, startDato, sluttDato, oppdatert) " +
                        "VALUES (?, ?, ?, CURRENT_TIMESTAMP)",
                aktorId,
                startDato,
                sluttDato
        );
    }
}
