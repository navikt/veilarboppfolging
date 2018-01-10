package no.nav.fo.veilarboppfolging.utils;


import no.nav.fo.veilarboppfolging.domain.Arbeidsforhold;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static java.util.Arrays.asList;
import static no.nav.fo.veilarboppfolging.utils.ArbeidsforholdUtils.*;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class ArbeidsforholdUtilsTest {

    @Test
    public void datoSkalVaereInneforPeriodeNaarTomErNull() {
        LocalDate mnd = LocalDate.of(2017,12,1);
        LocalDate fom = LocalDate.of(2010,12,1);
        Arbeidsforhold arbeidsforhold = new Arbeidsforhold().setFom(fom);
        assertThat(erDatoInnenforPeriode(arbeidsforhold,mnd)).isTrue();
    }

    @Test
    public void datoSkalVaereInneforPeriode() {
        LocalDate mnd = LocalDate.of(2017,12,1);
        LocalDate fom = LocalDate.of(2017,12,1);
        LocalDate tom = LocalDate.of(2017,12,30);
        Arbeidsforhold arbeidsforhold = new Arbeidsforhold().setFom(fom).setTom(tom);
        assertThat(erDatoInnenforPeriode(arbeidsforhold,mnd)).isTrue();
    }

    @Test
    public void datoSkalVaereInneforPeriode_2() {
        LocalDate mnd = LocalDate.of(2017,12,1);
        LocalDate fom = LocalDate.of(2017,10,1);
        LocalDate tom = LocalDate.of(2017,12,1);
        Arbeidsforhold arbeidsforhold = new Arbeidsforhold().setFom(fom).setTom(tom);
        assertThat(erDatoInnenforPeriode(arbeidsforhold,mnd)).isTrue();
    }

    @Test
    public void datoSkalIkkeVaereInneforPeriode() {
        LocalDate mnd = LocalDate.of(2017,12,1);
        LocalDate fom = LocalDate.of(2017,9,1);
        LocalDate tom = LocalDate.of(2017,11,30);
        Arbeidsforhold arbeidsforhold = new Arbeidsforhold().setFom(fom).setTom(tom);
        assertThat(erDatoInnenforPeriode(arbeidsforhold,mnd)).isFalse();
    }

    @Test
    public void skalHaArbeidsforholdPaaDato() {
        LocalDate mnd = LocalDate.of(2017,12,1);
        LocalDate fom1 = LocalDate.of(2017,10,1);
        LocalDate tom1 = LocalDate.of(2017,12,1);
        LocalDate fom2 = LocalDate.of(2017,12,1);
        LocalDate tom2 = LocalDate.of(2017,12,30);
        Arbeidsforhold arbeidsforhold1 = new Arbeidsforhold().setFom(fom1).setTom(tom1);
        Arbeidsforhold arbeidsforhold2 = new Arbeidsforhold().setFom(fom2).setTom(tom2);
        List<Arbeidsforhold> arbeidsforhold = asList(arbeidsforhold1, arbeidsforhold2);

        assertThat(harArbeidsforholdPaaDato(arbeidsforhold, mnd)).isTrue();
    }

    @Test
    public void skalIkkeHaArbeidsforholdPaaDato() {
        LocalDate mnd = LocalDate.of(2018,12,1);
        LocalDate fom1 = LocalDate.of(2017,10,1);
        LocalDate tom1 = LocalDate.of(2017,12,1);
        LocalDate fom2 = LocalDate.of(2017,12,1);
        LocalDate tom2 = LocalDate.of(2017,12,30);
        Arbeidsforhold arbeidsforhold1 = new Arbeidsforhold().setFom(fom1).setTom(tom1);
        Arbeidsforhold arbeidsforhold2 = new Arbeidsforhold().setFom(fom2).setTom(tom2);
        List<Arbeidsforhold> arbeidsforhold = asList(arbeidsforhold1, arbeidsforhold2);

        assertThat(harArbeidsforholdPaaDato(arbeidsforhold, mnd)).isFalse();
    }

    @Test
    public void skalVaereIJobb2av4Mnd() {
        ArbeidsforholdUtils.antallMnd = 4;
        ArbeidsforholdUtils.minAntallMndSammenhengendeJobb = 2;
        LocalDate dagensDato = LocalDate.of(2017,12,20);

        LocalDate mnd = LocalDate.of(2018,12,1);
        LocalDate fom1 = LocalDate.of(2017,10,1);
        LocalDate tom1 = LocalDate.of(2017,10,31);
        LocalDate fom2 = LocalDate.of(2017,9,1);
        LocalDate tom2 = LocalDate.of(2017,9,30);
        Arbeidsforhold arbeidsforhold1 = new Arbeidsforhold().setFom(fom1).setTom(tom1);
        Arbeidsforhold arbeidsforhold2 = new Arbeidsforhold().setFom(fom2).setTom(tom2);
        List<Arbeidsforhold> arbeidsforhold = asList(arbeidsforhold1, arbeidsforhold2);

        assertThat(oppfyllerKravOmArbeidserfaring(arbeidsforhold,dagensDato)).isTrue();
    }

    @Test
    public void skalIkkeVaereIJobb2av4Mnd() {
        ArbeidsforholdUtils.antallMnd = 4;
        ArbeidsforholdUtils.minAntallMndSammenhengendeJobb = 2;
        LocalDate dagensDato = LocalDate.of(2017,12,20);

        LocalDate mnd = LocalDate.of(2018,12,1);
        LocalDate fom1 = LocalDate.of(2017,11,1);
        LocalDate tom1 = LocalDate.of(2017,11,30);
        LocalDate fom2 = LocalDate.of(2017,9,1);
        LocalDate tom2 = LocalDate.of(2017,9,30);
        Arbeidsforhold arbeidsforhold1 = new Arbeidsforhold().setFom(fom1).setTom(tom1);
        Arbeidsforhold arbeidsforhold2 = new Arbeidsforhold().setFom(fom2).setTom(tom2);
        List<Arbeidsforhold> arbeidsforhold = asList(arbeidsforhold1, arbeidsforhold2);

        assertThat(oppfyllerKravOmArbeidserfaring(arbeidsforhold,dagensDato)).isFalse();
    }
}