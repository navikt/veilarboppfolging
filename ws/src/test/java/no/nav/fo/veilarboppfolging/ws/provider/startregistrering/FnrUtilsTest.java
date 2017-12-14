package no.nav.fo.veilarboppfolging.ws.provider.startregistrering;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static no.nav.fo.veilarboppfolging.ws.provider.startregistrering.FnrUtils.antallAarSidenDato;
import static no.nav.fo.veilarboppfolging.ws.provider.startregistrering.FnrUtils.utledFodselsdatoForFnr;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class FnrUtilsTest {

    private static final LocalDate dagensDato = LocalDate.of(2017,12,14);

    @Test
    public void fnrForPersonFodtMellom1900Og1999() {
        String fnr = "***REMOVED***";
        LocalDate date = utledFodselsdatoForFnr(fnr);
        assertThat(date).isEqualTo(LocalDate.of(1901,1,1));
    }

    @Test
    public void fnrForPersonFodtMellom1900Og1999Dnummer() {
        String fnr = "41010100000";
        LocalDate date = utledFodselsdatoForFnr(fnr);
        assertThat(date).isEqualTo(LocalDate.of(1901,1,1));
    }

    @Test
    public void fnrForPersonFodtMellom1900Og1999Hnummer() {
        String fnr = "01410100000";
        LocalDate date = utledFodselsdatoForFnr(fnr);
        assertThat(date).isEqualTo(LocalDate.of(1901,1,1));
    }

    @Test
    public void fnrForPersonFodtMellom1854Og1899() {
        String fnr = "***REMOVED***";
        LocalDate date = utledFodselsdatoForFnr(fnr);
        assertThat(date).isEqualTo(LocalDate.of(1854,1,1));
    }

    @Test
    public void fnrForPersonFodtMellom2000Og2039() {
        String fnr = "***REMOVED***";
        LocalDate date = utledFodselsdatoForFnr(fnr);
        assertThat(date).isEqualTo(LocalDate.of(2000,1,1));
    }

    @Test
    public void fnrForPersonFodtMellom2000Og2039_2() {
        String fnr = "***REMOVED***";
        LocalDate date = utledFodselsdatoForFnr(fnr);
        assertThat(date).isEqualTo(LocalDate.of(2038,1,1));
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