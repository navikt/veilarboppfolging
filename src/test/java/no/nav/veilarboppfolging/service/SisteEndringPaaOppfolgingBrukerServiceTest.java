package no.nav.veilarboppfolging.service;

import no.nav.common.types.identer.Fnr;
import no.nav.veilarboppfolging.repository.SisteEndringPaaOppfolgingBrukerRepository;
import no.nav.veilarboppfolging.test.IsolatedDatabaseTest;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;


public class SisteEndringPaaOppfolgingBrukerServiceTest extends IsolatedDatabaseTest {
    private SisteEndringPaaOppfolgingBrukerRepository sisteEndringPaaOppfolgingBrukerRepository;
    private SisteEndringPaaOppfolgingBrukerService sisteEndringPaaOppfolgingBrukerService;

    @Before
    public void setUp() {
        sisteEndringPaaOppfolgingBrukerRepository = new SisteEndringPaaOppfolgingBrukerRepository(db);
        sisteEndringPaaOppfolgingBrukerService = new SisteEndringPaaOppfolgingBrukerService(sisteEndringPaaOppfolgingBrukerRepository);
    }

    @Test
    public void testLagreSisteEndring() {
        Fnr fnrBruker1 = Fnr.of("1");
        ZonedDateTime sisteEndringBruker1 = ZonedDateTime.of(2022, 11, 10, 11, 0, 0, 0, ZoneId.systemDefault());

        Fnr fnrBruker2 = Fnr.of("2");
        ZonedDateTime sisteEndringBruker2 = ZonedDateTime.of(2019, 5, 19, 3, 0, 0, 0, ZoneId.systemDefault());

        Fnr fnrBruker3 = Fnr.of("3");
        ZonedDateTime sisteEndringBruker3 = ZonedDateTime.of(2022, 8, 3, 23, 0, 0, 0, ZoneId.systemDefault());

        sisteEndringPaaOppfolgingBrukerService.lagreSisteEndring(fnrBruker1, sisteEndringBruker1);
        sisteEndringPaaOppfolgingBrukerService.lagreSisteEndring(fnrBruker2, sisteEndringBruker2);
        sisteEndringPaaOppfolgingBrukerService.lagreSisteEndring(fnrBruker3, sisteEndringBruker3);


        Optional<ZonedDateTime> sisteEndringDato = sisteEndringPaaOppfolgingBrukerService.hentSisteEndringDato(fnrBruker1);
        Assert.assertTrue(sisteEndringDato.isPresent());
        Assert.assertTrue(sisteEndringDato.get().equals(sisteEndringBruker1));

        sisteEndringDato = sisteEndringPaaOppfolgingBrukerService.hentSisteEndringDato(fnrBruker2);
        Assert.assertTrue(sisteEndringDato.isPresent());
        Assert.assertTrue(sisteEndringDato.get().equals(sisteEndringBruker2));

        sisteEndringDato = sisteEndringPaaOppfolgingBrukerService.hentSisteEndringDato(fnrBruker3);
        Assert.assertTrue(sisteEndringDato.isPresent());
        Assert.assertTrue(sisteEndringDato.get().equals(sisteEndringBruker3));

        sisteEndringDato = sisteEndringPaaOppfolgingBrukerService.hentSisteEndringDato(Fnr.of("4"));
        Assert.assertFalse(sisteEndringDato.isPresent());
    }
}