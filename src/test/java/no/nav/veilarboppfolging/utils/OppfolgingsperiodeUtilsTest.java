package no.nav.veilarboppfolging.utils;

import no.nav.veilarboppfolging.repository.entity.OppfolgingsperiodeEntity;
import org.junit.Test;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static no.nav.veilarboppfolging.utils.OppfolgingsperiodeUtils.hentSisteOppfolgingsperiode;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class OppfolgingsperiodeUtilsTest {

    private OppfolgingsperiodeEntity lagPeriode(ZonedDateTime startDato, ZonedDateTime sluttDato) {
        return new OppfolgingsperiodeEntity(
                UUID.randomUUID(),
                "aktorId",
                null,
                startDato,
                sluttDato,
                null,
                List.of(),
                null,
                null,
                null,
                null
        );
    }

    @Test
    public void hentSisteOppfolgingsperiode_skal_returnere_null_for_tomt_eller_null_list() {
        assertNull(hentSisteOppfolgingsperiode(null));
        assertNull(hentSisteOppfolgingsperiode(Collections.emptyList()));
    }

    @Test
    public void hentSisteOppfolgingsperiode_skal_returnere_gjeldende_periode() {
        OppfolgingsperiodeEntity op1 = lagPeriode(
                ZonedDateTime.now(),
                ZonedDateTime.now().plusSeconds(10)
        );

        OppfolgingsperiodeEntity op2 = lagPeriode(
                ZonedDateTime.now().plusSeconds(30),
                null
        );

        assertEquals(op2, hentSisteOppfolgingsperiode(List.of(op1, op2)));
    }

    @Test
    public void hentSisteOppfolgingsperiode_skal_returnere_siste_periode_nar_flere_perioder_er_uten_slutt_dato() {
        OppfolgingsperiodeEntity op1 = lagPeriode(
                ZonedDateTime.now(),
                ZonedDateTime.now().plusSeconds(10)
        );

        OppfolgingsperiodeEntity op2 = lagPeriode(
                ZonedDateTime.now().plusSeconds(30),
                null
        );

        OppfolgingsperiodeEntity op3 = lagPeriode(
                ZonedDateTime.now().plusSeconds(50),
                null
        );

        assertEquals(op3, hentSisteOppfolgingsperiode(List.of(op1, op2, op3)));
    }

    @Test
    public void hentSisteOppfolgingsperiode_skal_returnere_siste_periode_hvis_ingen_gjeldende() {
        OppfolgingsperiodeEntity op1 = lagPeriode(
                ZonedDateTime.now(),
                ZonedDateTime.now().plusSeconds(10)
        );

        OppfolgingsperiodeEntity op2 = lagPeriode(
                ZonedDateTime.now().plusSeconds(30),
                ZonedDateTime.now().plusSeconds(50)
        );

        assertEquals(op2, hentSisteOppfolgingsperiode(List.of(op1, op2)));
    }

}
