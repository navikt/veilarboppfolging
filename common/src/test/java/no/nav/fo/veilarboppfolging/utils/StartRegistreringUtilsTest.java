package no.nav.fo.veilarboppfolging.utils;

import no.nav.fo.veilarboppfolging.domain.Arbeidsforhold;
import no.nav.fo.veilarboppfolging.domain.ArenaOppfolging;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static no.nav.fo.veilarboppfolging.TestUtils.getFodselsnummerOnDateMinusYears;
import static no.nav.fo.veilarboppfolging.utils.DateUtils.erDatoEldreEnnEllerLikAar;
import static no.nav.fo.veilarboppfolging.utils.StartRegistreringUtils.*;
import static org.assertj.core.api.Java6Assertions.assertThat;


public class StartRegistreringUtilsTest {

    private static final LocalDate dagensDato = LocalDate.of(2017,12,14);

    @AfterEach
    public void clearSystemProperties() {
        System.clearProperty(MAX_ALDER_AUTOMATISK_REGISTRERING);
        System.clearProperty(MIN_ALDER_AUTOMATISK_REGISTRERING);
    }

    private void setGyldigeAldersProperties() {
        System.setProperty(MIN_ALDER_AUTOMATISK_REGISTRERING, "30");
        System.setProperty(MAX_ALDER_AUTOMATISK_REGISTRERING, "59");
    }

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
        setGyldigeAldersProperties();
        assertThat(oppfyllerKravOmAutomatiskRegistrering(fnr, () -> arbeidsforhold,arenaOppfolging,dagensDato)).isTrue();
    }

    @Test
    public void oppfylleIkkeKravVedUgyldigeAldersPropertier() {
        LocalDate ISERVFraDato = LocalDate.of(2015,12,13);
        String fnr = getFodselsnummerOnDateMinusYears(dagensDato, 40);
        List<Arbeidsforhold> arbeidsforhold = getArbeidsforholdSomOppfyllerKrav();
        ArenaOppfolging arenaOppfolging = new ArenaOppfolging().setInaktiveringsdato(ISERVFraDato);
        System.setProperty(MIN_ALDER_AUTOMATISK_REGISTRERING, "");
        System.setProperty(MAX_ALDER_AUTOMATISK_REGISTRERING, "59");
        assertThat(oppfyllerKravOmAutomatiskRegistrering(fnr, () -> arbeidsforhold,arenaOppfolging,dagensDato)).isFalse();
        clearSystemProperties();
        System.setProperty(MIN_ALDER_AUTOMATISK_REGISTRERING, "30");
        System.setProperty(MAX_ALDER_AUTOMATISK_REGISTRERING, "");
        assertThat(oppfyllerKravOmAutomatiskRegistrering(fnr, () -> arbeidsforhold,arenaOppfolging,dagensDato)).isFalse();
        clearSystemProperties();
        System.setProperty(MIN_ALDER_AUTOMATISK_REGISTRERING, "x");
        System.setProperty(MAX_ALDER_AUTOMATISK_REGISTRERING, "y");
        assertThat(oppfyllerKravOmAutomatiskRegistrering(fnr, () -> arbeidsforhold,arenaOppfolging,dagensDato)).isFalse();
    }

    @Test
    public void oppfylleIkkeKravVedManglendePropertier() {

        LocalDate ISERVFraDato = LocalDate.of(2015,12,13);
        String fnr = getFodselsnummerOnDateMinusYears(dagensDato, 40);
        List<Arbeidsforhold> arbeidsforhold = getArbeidsforholdSomOppfyllerKrav();
        ArenaOppfolging arenaOppfolging = new ArenaOppfolging().setInaktiveringsdato(ISERVFraDato);
        assertThat(oppfyllerKravOmAutomatiskRegistrering(fnr, () -> arbeidsforhold,arenaOppfolging,dagensDato)).isFalse();
    }

    public static List<Arbeidsforhold> getArbeidsforholdSomOppfyllerKrav() {
        Arbeidsforhold arbeidsforhold = new Arbeidsforhold();
        arbeidsforhold.setFom(LocalDate.of(2015,10,10));
        return Collections.singletonList(arbeidsforhold);
    }
}