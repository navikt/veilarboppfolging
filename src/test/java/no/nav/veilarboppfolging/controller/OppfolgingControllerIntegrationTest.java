package no.nav.veilarboppfolging.controller;

import no.nav.common.abac.Pep;
import no.nav.common.abac.domain.request.ActionId;
import no.nav.common.auth.context.AuthContextHolder;
import no.nav.common.client.aktoroppslag.AktorOppslagClient;
import no.nav.common.client.aktorregister.AktorregisterClient;
import no.nav.common.types.identer.AktorId;
import no.nav.veilarboppfolging.config.ApplicationTestConfig;
import no.nav.veilarboppfolging.controller.request.AktiverArbeidssokerData;
import no.nav.veilarboppfolging.controller.request.Fnr;
import no.nav.veilarboppfolging.controller.response.OppfolgingPeriodeDTO;
import no.nav.veilarboppfolging.domain.Innsatsgruppe;
import no.nav.veilarboppfolging.repository.OppfolgingsPeriodeRepository;
import no.nav.veilarboppfolging.service.AuthService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = {ApplicationTestConfig.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class OppfolgingControllerIntegrationTest {
    private final static String fnr = "123";
    private final static String aktorId = fnr;
    private final String token = "token";

    @MockBean
    Pep veilarbPep;

    @MockBean
    AuthContextHolder authContextHolder;

    @Autowired
    AktorOppslagClient aktorOppslagClient;

    @MockBean
    AktorregisterClient aktorregisterClient;

    @Autowired
    AuthService authService;

    @Autowired
    OppfolgingController oppfolgingController;

    @Autowired
    OppfolgingsPeriodeRepository oppfolgingsPeriodeRepository;

    @Autowired
    SystemOppfolgingController systemOppfolgingController;

    @Test
    void hentOppfolgingsPeriode_brukerHarEnAktivOppfolgingsPeriode() throws EmptyResultDataAccessException {
        mockHappyPathVeileder();

        var perioder = startOppfolging();

        Assertions.assertEquals(1, perioder.size());

        var forstePeriode = perioder.get(0);
        var uuid = forstePeriode.uuid;
        var periode = oppfolgingController.hentOppfolgingsPeriode(uuid.toString());

        Assertions.assertEquals(uuid, periode.getUuid());
        Assertions.assertNotNull(forstePeriode.startDato);
        Assertions.assertEquals(forstePeriode.startDato, periode.getStartDato());
    }

    @Test
    void hentOppfolgingsPeriode_veilederManglerTilgang() {
        mockHappyPathVeileder();
        var perioder = startOppfolging();

        Assertions.assertEquals(1, perioder.size());

        var forstePeriode = perioder.get(0);
        var uuid = forstePeriode.uuid.toString();
        when(veilarbPep.harTilgangTilPerson(token, ActionId.READ, AktorId.of(aktorId))).thenReturn(false);

        assertThrows(ResponseStatusException.class, () -> oppfolgingController.hentOppfolgingsPeriode(uuid));

    }

    private List<OppfolgingPeriodeDTO> startOppfolging() {
        var aktiverArbeidssokerData = new AktiverArbeidssokerData(new Fnr(fnr), Innsatsgruppe.STANDARD_INNSATS);
        systemOppfolgingController.aktiverBruker(aktiverArbeidssokerData);
        return oppfolgingController.hentOppfolgingsperioder(fnr);
    }

    private void mockHappyPathVeileder() {
        mockAuthOK();
    }

    private void mockAuthOK() {
        String token = "token";
        when(veilarbPep.harTilgangTilPerson(token, ActionId.READ, AktorId.of(aktorId))).thenReturn(true);
        when(authContextHolder.getIdTokenString()).thenReturn(Optional.of(token));

        when(authContextHolder.erSystemBruker()).thenReturn(true);
        when(aktorOppslagClient.hentAktorId(new no.nav.common.types.identer.Fnr(fnr))).thenReturn(AktorId.of(aktorId));
    }
}
