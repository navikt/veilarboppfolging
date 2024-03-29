package no.nav.veilarboppfolging.controller;

import com.nimbusds.jwt.JWTClaimsSet;
import no.nav.common.abac.domain.request.ActionId;
import no.nav.common.client.aktoroppslag.AktorOppslagClient;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.veilarboppfolging.IntegrationTest;
import no.nav.veilarboppfolging.controller.request.AktiverArbeidssokerData;
import no.nav.veilarboppfolging.controller.request.Innsatsgruppe;
import no.nav.veilarboppfolging.controller.response.OppfolgingPeriodeDTO;
import no.nav.veilarboppfolging.repository.OppfolgingsPeriodeRepository;
import no.nav.veilarboppfolging.service.AuthService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;


class OppfolgingControllerIntegrationTest extends IntegrationTest {

    private final static Fnr FNR = Fnr.of("123");

    private final static AktorId AKTOR_ID = AktorId.of("3409823");

    private final static String TOKEN = "token";

    @Autowired
    AktorOppslagClient aktorOppslagClient;

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
        mockAuthOk();

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
        mockAuthOk();
        var perioder = startOppfolging();

        Assertions.assertEquals(1, perioder.size());

        var forstePeriode = perioder.get(0);
        var uuid = forstePeriode.uuid.toString();
        when(veilarbPep.harTilgangTilPerson(TOKEN, ActionId.READ, AKTOR_ID)).thenReturn(false);

        assertThrows(ResponseStatusException.class, () -> oppfolgingController.hentOppfolgingsPeriode(uuid));

    }

    private List<OppfolgingPeriodeDTO> startOppfolging() {
        var aktiverArbeidssokerData = new AktiverArbeidssokerData(
                new AktiverArbeidssokerData.Fnr(FNR.get()),
                Innsatsgruppe.STANDARD_INNSATS
        );
        systemOppfolgingController.aktiverBruker(aktiverArbeidssokerData);
        return oppfolgingController.hentOppfolgingsperioder(FNR);
    }

    private void mockAuthOk() {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer("microsoftonline.com")
                .claim("azp_name", "cluster:team:veilarbregistrering")
                .claim("roles", Collections.singletonList("access_as_application"))
                .build();

        when(authContextHolder.getIdTokenClaims()).thenReturn(Optional.of(claims));

        String token = "token";
        when(veilarbPep.harTilgangTilPerson(token, ActionId.READ, AKTOR_ID)).thenReturn(true);
        when(authContextHolder.getIdTokenString()).thenReturn(Optional.of(token));

        when(authContextHolder.erSystemBruker()).thenReturn(true);
        when(aktorOppslagClient.hentAktorId(FNR)).thenReturn(AKTOR_ID);
        when(aktorOppslagClient.hentFnr(AKTOR_ID)).thenReturn(FNR);
    }

}
