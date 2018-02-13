package no.nav.fo.veilarboppfolging.utils;

import no.nav.fo.veilarboppfolging.domain.Arbeidsforhold;
import no.nav.fo.veilarboppfolging.domain.ArenaOppfolging;
import no.nav.fo.veilarboppfolging.domain.RegistrertBruker;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static no.nav.fo.veilarboppfolging.TestUtils.getFodselsnummerOnDateMinusYears;
import static no.nav.fo.veilarboppfolging.services.registrerBruker.Konstanter.*;
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
                NUS_KODE_4,
                null,
                null,
                ENIG_I_OPPSUMMERING,
                OPPSUMMERING,
                UTDANNING_IKKE_BESTATT,
                UTDANNING_GODKJENT_NORGE,
                HAR_JOBBET_SAMMENHENGENDE,
                HAR_HELSEUTFORDRINGER,
                SITUASJON
        );
        assertThat(erIkkeSelvgaende(bruker)).isTrue();
    }

    @Test
    void brukerHarIkkeGodkjentUtdanning() {
        RegistrertBruker bruker = new RegistrertBruker(
                NUS_KODE_4,
                null,
                null,
                ENIG_I_OPPSUMMERING,
                OPPSUMMERING,
                UTDANNING_BESTATT,
                UTDANNING_IKKE_GODKJENT_NORGE,
                HAR_JOBBET_SAMMENHENGENDE,
                HAR_HELSEUTFORDRINGER,
                SITUASJON);
        assertThat(erIkkeSelvgaende(bruker)).isTrue();
    }

    @Test
    void brukerHarGrunnskole() {
        RegistrertBruker bruker = new RegistrertBruker(
                NUS_KODE_0,
                null,
                null,
                ENIG_I_OPPSUMMERING,
                OPPSUMMERING,
                UTDANNING_BESTATT,
                UTDANNING_GODKJENT_NORGE,
                HAR_JOBBET_SAMMENHENGENDE,
                HAR_HELSEUTFORDRINGER,
                SITUASJON);
        assertThat(erIkkeSelvgaende(bruker)).isTrue();
    }

}