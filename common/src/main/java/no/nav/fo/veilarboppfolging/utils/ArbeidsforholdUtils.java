package no.nav.fo.veilarboppfolging.utils;

import no.nav.fo.veilarboppfolging.domain.Arbeidsforhold;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

public class ArbeidsforholdUtils {

    static int antallMnd = 12;
    static int minAntallMndSammenhengendeJobb = 6;
    static int dagIMnd = 1;

    public static boolean oppfyllerKravOmArbeidserfaring(List<Arbeidsforhold> arbeidsforhold, LocalDate dagensDato) {
        int antallSammenhengendeMandeder = 0;
        int mndFraDagensMnd = 0;
        LocalDate innevaerendeMnd = LocalDate.of(dagensDato.getYear(), dagensDato.getMonthValue(), dagIMnd);

        while(antallSammenhengendeMandeder < minAntallMndSammenhengendeJobb && mndFraDagensMnd < antallMnd) {

            if(harArbeidsforholdPaaDato(arbeidsforhold, innevaerendeMnd)) {
                antallSammenhengendeMandeder += 1;
            } else {
                antallSammenhengendeMandeder = 0;
            }

            innevaerendeMnd = innevaerendeMnd.minusMonths(1);
            mndFraDagensMnd += 1;
        }
        return antallSammenhengendeMandeder >= minAntallMndSammenhengendeJobb;
    }

    static boolean harArbeidsforholdPaaDato(List<Arbeidsforhold> arbeidsforholdListe, LocalDate innevaerendeMnd) {
        return arbeidsforholdListe.stream()
                .map(arbeidsforhold -> erDatoInnenforPeriode(arbeidsforhold, innevaerendeMnd))
                .filter(b -> b)
                .findAny().orElse(false);
    }

    static boolean erDatoInnenforPeriode(Arbeidsforhold arbeidsforhold, LocalDate innevaerendeMnd) {
        return innevaerendeMnd.isAfter(arbeidsforhold.getFom().minusDays(1)) &&
                (Objects.isNull(arbeidsforhold.getTom()) || innevaerendeMnd.isBefore(arbeidsforhold.getTom().plusDays(1)));
    }
}
