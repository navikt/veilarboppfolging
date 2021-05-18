package no.nav.veilarboppfolging.controller;
import no.nav.veilarboppfolging.domain.Fnr;
import no.nav.veilarboppfolging.config.ApplicationTestConfig;
import no.nav.veilarboppfolging.domain.AktiverArbeidssokerData;
import no.nav.veilarboppfolging.domain.Innsatsgruppe;
import no.nav.veilarboppfolging.repository.OppfolgingsPeriodeRepository;
import no.nav.veilarboppfolging.service.*;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.EmptyResultDataAccessException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = {ApplicationTestConfig.class})
class OppfolgingControllerIntegrationTest {
    private final static String fnr = "123";
    private final static String aktorId = "321";


    @MockBean
    private AuthService authService;

    @Autowired
    private OppfolgingController oppfolgingController;

    @Autowired
    OppfolgingsPeriodeRepository oppfolgingsPeriodeRepository;

    @Autowired
    SystemOppfolgingController systemOppfolgingController;

    @Test
    void hentOppfolgingsPeriode_periodeEksistererIkke() throws EmptyResultDataAccessException {

        mockHappyPathBruker();

        assertThrows(EmptyResultDataAccessException.class, () -> oppfolgingController.hentOppfolgingsPeriode("123"));
    }

    @Test
    void hentOppfolgingsPeriode_brukerHarEnAktivOppfolgingsPeriode() throws EmptyResultDataAccessException {
        mockHappyPathVeileder();
        var aktiverArbeidssokerData = new AktiverArbeidssokerData(new Fnr(fnr), Innsatsgruppe.STANDARD_INNSATS);
        systemOppfolgingController.aktiverBruker(aktiverArbeidssokerData);
        var perioder = oppfolgingController.hentOppfolgingsperioder(fnr);

        Assertions.assertEquals(1, perioder.size());

        var forstePeriode = perioder.get(0);
        var uuid = forstePeriode.uuid;
        var periode = oppfolgingController.hentOppfolgingsPeriode(uuid.toString());

        Assertions.assertEquals(uuid, periode.getUuid());
        Assertions.assertNotNull(forstePeriode.startDato);
        Assertions.assertEquals(forstePeriode.startDato, periode.getStartDato());
    }

    private void mockHappyPathBruker() {
        when(authService.hentIdentForEksternEllerIntern(fnr)).thenReturn(fnr);
        mockAuthOK();
    }

    private void mockHappyPathVeileder() {
        when(authService.erInternBruker()).thenReturn(true);
        mockAuthOK();
    }

    private void mockAuthOK() {
        when(authService.getAktorIdOrThrow(fnr)).thenReturn(aktorId);
        when(authService.hentIdentForEksternEllerIntern(fnr)).thenReturn(fnr);
        doNothing().when(authService).sjekkLesetilgangMedFnr(fnr);
    }
}
