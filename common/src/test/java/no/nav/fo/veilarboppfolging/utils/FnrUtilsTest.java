package no.nav.fo.veilarboppfolging.utils;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static no.nav.fo.veilarboppfolging.TestUtils.getFodselsnummerOnDateMinusYears;
import static no.nav.fo.veilarboppfolging.utils.FnrUtils.antallAarSidenDato;
import static no.nav.fo.veilarboppfolging.utils.FnrUtils.utledFodselsdatoForFnr;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class FnrUtilsTest {

    private static final LocalDate dagensDato = LocalDate.of(2017,12,14);

    @Test
    public void skalUtledeKorrektFodselsdato() {
        String fnr = getFodselsnummerOnDateMinusYears(dagensDato, 40);
        assertThat(utledFodselsdatoForFnr(fnr)).isEqualTo(LocalDate.of(1977,12,14));
    }

    @Test
    public void skalVaere20Aar() {
        LocalDate dato = LocalDate.of(1997,12,14);
        assertThat(antallAarSidenDato(dato, dagensDato)).isEqualTo(20);
    }

    @Test
    public void skalVaere20Aar_2() {
        LocalDate dato = LocalDate.of(1997, 1, 1);
        assertThat(antallAarSidenDato(dato, dagensDato)).isEqualTo(20);
    }

    @Test
    public void skalVaere19Aar() {
        LocalDate dato = LocalDate.of(1997, 12, 15);
        assertThat(antallAarSidenDato(dato, dagensDato)).isEqualTo(19);
    }

    @Test
    public void skalVaere19Aar_2() {
        LocalDate dato = LocalDate.of(1998, 12, 14);
        assertThat(antallAarSidenDato(dato, dagensDato)).isEqualTo(19);
    }
}