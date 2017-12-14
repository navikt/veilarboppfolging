package no.nav.fo.veilarboppfolging.ws.provider.startregistrering;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static no.nav.fo.veilarboppfolging.ws.provider.startregistrering.FnrUtils.antallAarSidenDato;
import static no.nav.fo.veilarboppfolging.ws.provider.startregistrering.FnrUtils.utledFodselsdatoForFnr;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class FnrUtilsTest {

    private static final LocalDate dagensDato = LocalDate.of(2017,12,14);
    private static final String AREMARK_FNR = "***REMOVED***";

    @Test
    public void skalUtledeKorrektFodselsdatoForAremark() {
        assertThat(utledFodselsdatoForFnr(AREMARK_FNR)).isEqualTo(LocalDate.of(1980,10,10));
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