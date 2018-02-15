package no.nav.fo.veilarboppfolging.utils;


import io.vavr.control.Try;
import no.nav.apiapp.security.PepClient;
import no.nav.fo.veilarboppfolging.domain.Arbeidsforhold;
import no.nav.fo.veilarboppfolging.domain.ArenaOppfolging;
import no.nav.fo.veilarboppfolging.domain.RegistrertBruker;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import static no.nav.fo.veilarboppfolging.services.ArenaUtils.erUnderOppfolging;
import static no.nav.fo.veilarboppfolging.utils.ArbeidsforholdUtils.oppfyllerKravOmArbeidserfaring;
import static no.nav.fo.veilarboppfolging.utils.DateUtils.erDatoEldreEnnEllerLikAar;
import static no.nav.fo.veilarboppfolging.utils.KonfigForRegistrertBrukerIkkeSelvgaende.*;


public class StartRegistreringUtils {

    static final int ANTALL_AAR_ISERV = 2;
    static final int MIN_ALDER_AUTOMATISK_REGISTRERING = 30;
    static final int MAX_ALDER_AUTOMATISK_REGISTRERING = 59;

    public static boolean oppfyllerKravOmAutomatiskRegistrering(String fnr, Supplier<List<Arbeidsforhold>> arbeidsforholdSupplier,
                                                                ArenaOppfolging arenaOppfolging, LocalDate dagensDato) {
        LocalDate fodselsdato = FnrUtils.utledFodselsdatoForFnr(fnr);
        int alder = FnrUtils.antallAarSidenDato(fodselsdato,dagensDato);
        LocalDate inaktiveringsdato = Optional.ofNullable(arenaOppfolging).map(ArenaOppfolging::getInaktiveringsdato).orElse(null);

        return oppfyllerKravOmInaktivitet(dagensDato, inaktiveringsdato) &&
                oppfyllerKravOmAlder(alder) &&
                oppfyllerKravOmArbeidserfaring(arbeidsforholdSupplier.get(),dagensDato);
    }

    public static boolean erUnderoppfolgingIArena(ArenaOppfolging arenaOppfolging) {
        return erUnderOppfolging(
                arenaOppfolging.getFormidlingsgruppe(),
                arenaOppfolging.getServicegruppe());
    }

    static boolean oppfyllerKravOmAlder(int alder) {
        return alder >= MIN_ALDER_AUTOMATISK_REGISTRERING && alder <= MAX_ALDER_AUTOMATISK_REGISTRERING;
    }

    static boolean oppfyllerKravOmInaktivitet(LocalDate dagensDato, LocalDate inaktiveringsdato) {
        return Objects.isNull(inaktiveringsdato) || erDatoEldreEnnEllerLikAar(dagensDato, inaktiveringsdato, ANTALL_AAR_ISERV);
    }

    public static <T extends Throwable> void sjekkLesetilgangOrElseThrow(String fnr, PepClient pepClient, Function<Throwable, T> exceptionMapper) throws T {
        Try.of(() -> pepClient.sjekkLeseTilgangTilFnr(fnr))
                .getOrElseThrow(exceptionMapper);
    }

    public static boolean erBesvarelseneValidertSomIkkeSelvgaaende(RegistrertBruker bruker) {
        return bruker.getSituasjon().equals(ANNET)
                || bruker.getNusKode().equals(NUS_KODE_0)
                || bruker.getNusKode().equals(NUS_KODE_2)
                || bruker.isHarJobbetSammenhengende() == HAR_JOBBET_SAMMENHENGENDE
                || bruker.isUtdanningBestatt() == UTDANNING_BESTATT
                || bruker.isUtdanningGodkjentNorge() == UTDANNING_GODKJENT_I_NORGE
                || bruker.isHarHelseutfordringer() == HAR_HELSEUTFORDRINGER;
    }
}
