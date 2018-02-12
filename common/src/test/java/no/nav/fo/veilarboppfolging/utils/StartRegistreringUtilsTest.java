package no.nav.fo.veilarboppfolging.utils;

import no.nav.fo.veilarboppfolging.domain.Arbeidsforhold;
import no.nav.fo.veilarboppfolging.domain.ArenaOppfolging;
import no.nav.fo.veilarboppfolging.domain.RegistrertBruker;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static no.nav.fo.veilarboppfolging.TestUtils.getFodselsnummerOnDateMinusYears;
import static no.nav.fo.veilarboppfolging.utils.DateUtils.erDatoEldreEnnEllerLikAar;
import static no.nav.fo.veilarboppfolging.utils.StartRegistreringUtils.oppfyllerKravOmAutomatiskRegistrering;
import static no.nav.fo.veilarboppfolging.utils.StartRegistreringUtils.oppfyllerKravOmInaktivitet;
import static no.nav.fo.veilarboppfolging.utils.StartRegistreringUtils.erIkkeSelvgaende;
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
        assertThat(oppfyllerKravOmInaktivitet(dagensDato,ISERVFraDato)).isFalse();
    }

    @Test
    public void oppfylleKrav() {
        LocalDate ISERVFraDato = LocalDate.of(2015,12,13);
        String fnr = getFodselsnummerOnDateMinusYears(dagensDato, 40);
        List<Arbeidsforhold> arbeidsforhold = getArbeidsforholdSomOppfyllerKrav();
        ArenaOppfolging arenaOppfolging = new ArenaOppfolging().setInaktiveringsdato(ISERVFraDato);
        assertThat(oppfyllerKravOmAutomatiskRegistrering(fnr, () -> arbeidsforhold,arenaOppfolging,dagensDato)).isTrue();
    }

    public static List<Arbeidsforhold> getArbeidsforholdSomOppfyllerKrav() {
        Arbeidsforhold arbeidsforhold = new Arbeidsforhold();
        arbeidsforhold.setFom(LocalDate.of(2015,10,10));
        return Collections.singletonList(arbeidsforhold);
    }

    @Test
    void brukerHarIkkeBestattUtdanning() {
        RegistrertBruker bruker = new RegistrertBruker(
                "nus12",
                "12345",
                null,
                true,
                "Test test oppsummering",
                false,
                true,
                true,
                false,
                "MISTET_JOBBEN");
        assertThat(erIkkeSelvgaende(bruker)).isTrue();
    }

    @Test
    void brukerHarIkkeGodkjentUtdanning() {
        RegistrertBruker bruker = new RegistrertBruker(
                "nus12",
                "12345",
                null,
                true,
                "Test test oppsummering",
                true,
                false,
                true,
                false,
                "MISTET_JOBBEN");
        assertThat(erIkkeSelvgaende(bruker)).isTrue();
    }
    @Test
    void brukerHarIkkeJobbetSammenhengende() {
        RegistrertBruker bruker = new RegistrertBruker(
                "nus12",
                "12345",
                null,
                true,
                "Test test oppsummering",
                true,
                true,
                false,
                false,
                "MISTET_JOBBEN");
        assertThat(erIkkeSelvgaende(bruker)).isTrue();
    }
}