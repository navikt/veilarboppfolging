package no.nav.fo.veilarboppfolging.domain;

import lombok.Data;
import lombok.experimental.Accessors;
import no.nav.fo.veilarboppfolging.utils.DateUtils;
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.informasjon.arbeidsforhold.AnsettelsesPeriode;

import java.time.LocalDate;
import java.util.Optional;

@Data
@Accessors(chain = true)
public class Arbeidsforhold {
    private String arbeidsgiverOrgnummer;
    private LocalDate fom;
    private LocalDate tom;

    public static Arbeidsforhold of(no.nav.tjeneste.virksomhet.arbeidsforhold.v3.informasjon.arbeidsforhold.Arbeidsforhold arbeidsforhold) {
        return new Arbeidsforhold().setArbeidsgiverOrgnummer(arbeidsforhold.getArbeidsgiver().getAktoerId())
                .setFom(getFom(arbeidsforhold.getAnsettelsesPeriode()))
                .setTom(getTom(arbeidsforhold.getAnsettelsesPeriode()));
    }

    private static LocalDate getFom(AnsettelsesPeriode periode) {
        return Optional.ofNullable(periode)
                .map(AnsettelsesPeriode::getFomBruksperiode)
                .map(DateUtils::xmlGregorianCalendarToLocalDate).orElse(null);
    }

    private static LocalDate getTom(AnsettelsesPeriode periode) {
        return Optional.ofNullable(periode)
                .map(AnsettelsesPeriode::getFomBruksperiode)
                .map(DateUtils::xmlGregorianCalendarToLocalDate).orElse(null);
    }
}
