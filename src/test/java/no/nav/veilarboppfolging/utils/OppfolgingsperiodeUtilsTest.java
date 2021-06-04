package no.nav.veilarboppfolging.utils;

import no.nav.veilarboppfolging.domain.Oppfolgingsperiode;
import org.junit.Test;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;

import static no.nav.veilarboppfolging.utils.OppfolgingsperiodeUtils.hentSisteOppfolgingsperiode;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class OppfolgingsperiodeUtilsTest {

    @Test
    public void hentSisteOppfolgingsperiode_skal_returnere_null_for_tomt_eller_null_list() {
        assertNull(hentSisteOppfolgingsperiode(null));
        assertNull(hentSisteOppfolgingsperiode(Collections.emptyList()));
    }

    @Test
    public void hentSisteOppfolgingsperiode_skal_returnere_gjeldende_periode() {
        Oppfolgingsperiode op1 = Oppfolgingsperiode.builder()
                .startDato(ZonedDateTime.now())
                .sluttDato(ZonedDateTime.now().plusSeconds(10))
                .build();

        Oppfolgingsperiode op2 = Oppfolgingsperiode.builder()
                .startDato(ZonedDateTime.now().plusSeconds(30))
                .sluttDato(null)
                .build();

        assertEquals(op2, hentSisteOppfolgingsperiode(List.of(op1, op2)));
    }

    @Test
    public void hentSisteOppfolgingsperiode_skal_returnere_siste_periode_nar_flere_perioder_er_uten_slutt_dato() {
        Oppfolgingsperiode op1 = Oppfolgingsperiode.builder()
                .startDato(ZonedDateTime.now())
                .sluttDato(ZonedDateTime.now().plusSeconds(10))
                .build();

        Oppfolgingsperiode op2 = Oppfolgingsperiode.builder()
                .startDato(ZonedDateTime.now().plusSeconds(30))
                .sluttDato(null)
                .build();

        Oppfolgingsperiode op3 = Oppfolgingsperiode.builder()
                .startDato(ZonedDateTime.now().plusSeconds(50))
                .sluttDato(null)
                .build();

        assertEquals(op3, hentSisteOppfolgingsperiode(List.of(op1, op2, op3)));
    }

    @Test
    public void hentSisteOppfolgingsperiode_skal_returnere_siste_periode_hvis_ingen_gjeldende() {
        Oppfolgingsperiode op1 = Oppfolgingsperiode.builder()
                .startDato(ZonedDateTime.now())
                .sluttDato(ZonedDateTime.now().plusSeconds(10))
                .build();

        Oppfolgingsperiode op2 = Oppfolgingsperiode.builder()
                .startDato(ZonedDateTime.now().plusSeconds(30))
                .sluttDato(ZonedDateTime.now().plusSeconds(50))
                .build();

        assertEquals(op2, hentSisteOppfolgingsperiode(List.of(op1, op2)));
    }

}
