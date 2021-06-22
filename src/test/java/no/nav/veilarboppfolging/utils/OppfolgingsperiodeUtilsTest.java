package no.nav.veilarboppfolging.utils;

import no.nav.veilarboppfolging.repository.entity.OppfolgingsperiodeEntity;
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
        OppfolgingsperiodeEntity op1 = OppfolgingsperiodeEntity.builder()
                .startDato(ZonedDateTime.now())
                .sluttDato(ZonedDateTime.now().plusSeconds(10))
                .build();

        OppfolgingsperiodeEntity op2 = OppfolgingsperiodeEntity.builder()
                .startDato(ZonedDateTime.now().plusSeconds(30))
                .sluttDato(null)
                .build();

        assertEquals(op2, hentSisteOppfolgingsperiode(List.of(op1, op2)));
    }

    @Test
    public void hentSisteOppfolgingsperiode_skal_returnere_siste_periode_nar_flere_perioder_er_uten_slutt_dato() {
        OppfolgingsperiodeEntity op1 = OppfolgingsperiodeEntity.builder()
                .startDato(ZonedDateTime.now())
                .sluttDato(ZonedDateTime.now().plusSeconds(10))
                .build();

        OppfolgingsperiodeEntity op2 = OppfolgingsperiodeEntity.builder()
                .startDato(ZonedDateTime.now().plusSeconds(30))
                .sluttDato(null)
                .build();

        OppfolgingsperiodeEntity op3 = OppfolgingsperiodeEntity.builder()
                .startDato(ZonedDateTime.now().plusSeconds(50))
                .sluttDato(null)
                .build();

        assertEquals(op3, hentSisteOppfolgingsperiode(List.of(op1, op2, op3)));
    }

    @Test
    public void hentSisteOppfolgingsperiode_skal_returnere_siste_periode_hvis_ingen_gjeldende() {
        OppfolgingsperiodeEntity op1 = OppfolgingsperiodeEntity.builder()
                .startDato(ZonedDateTime.now())
                .sluttDato(ZonedDateTime.now().plusSeconds(10))
                .build();

        OppfolgingsperiodeEntity op2 = OppfolgingsperiodeEntity.builder()
                .startDato(ZonedDateTime.now().plusSeconds(30))
                .sluttDato(ZonedDateTime.now().plusSeconds(50))
                .build();

        assertEquals(op2, hentSisteOppfolgingsperiode(List.of(op1, op2)));
    }

}
