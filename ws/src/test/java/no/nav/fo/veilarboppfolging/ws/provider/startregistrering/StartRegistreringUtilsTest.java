package no.nav.fo.veilarboppfolging.ws.provider.startregistrering;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static no.nav.fo.veilarboppfolging.ws.provider.startregistrering.StartRegistreringUtils.erDatoEldreEnnEllerLikAar;
import static no.nav.fo.veilarboppfolging.ws.provider.startregistrering.StartRegistreringUtils.oppfyllerKravOmAutomatiskRegistrering;
import static org.assertj.core.api.Java6Assertions.assertThat;


public class StartRegistreringUtilsTest {

    private static final LocalDate dagensDato = LocalDate.of(2017,12,14);


    @Test
    public void datoEldreEnnEllerLikToAar() {
        LocalDate dato = LocalDate.of(2015,12,14);
        assertThat(erDatoEldreEnnEllerLikAar(dagensDato, dato,2)).isTrue();
    }

    @Test
    public void datoIkkeEldreEnnToAar() {
        LocalDate dato = LocalDate.of(2015,12,15);
        assertThat(erDatoEldreEnnEllerLikAar(dagensDato, dato,2)).isFalse();
    }

    @Test
    public void oppfyllerIkkeKravPgaISERVDato() {
        LocalDate ISERVFraDato = LocalDate.of(2015,12,15);
        assertThat(oppfyllerKravOmAutomatiskRegistrering("***REMOVED***",ISERVFraDato,dagensDato)).isFalse();
    }

    @Test
    public void oppfyllerIkkeKravPgaAlder() {
        LocalDate ISERVFraDato = LocalDate.of(2010,12,14);
        assertThat(oppfyllerKravOmAutomatiskRegistrering("***REMOVED***",ISERVFraDato,dagensDato)).isFalse();
    }

    @Test
    public void oppfylleKrav() {
        LocalDate ISERVFraDato = LocalDate.of(2015,12,13);
        assertThat(oppfyllerKravOmAutomatiskRegistrering("***REMOVED***",ISERVFraDato,dagensDato)).isTrue();
    }

}