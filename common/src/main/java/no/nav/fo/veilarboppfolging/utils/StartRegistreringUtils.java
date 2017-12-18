package no.nav.fo.veilarboppfolging.utils;


import java.time.LocalDate;
import java.util.Objects;


public class StartRegistreringUtils {

    static final int ANTALL_AAR_ISERV = 2;
    static final int MIN_ALDER_AUTOMATISK_REGISTRERING = 30;
    static final int MAX_ALDER_AUTOMATISK_REGISTRERING = 59;

    public static boolean oppfyllerKravOmAutomatiskRegistrering(String fnr, LocalDate inaktiveringsdato, LocalDate dagensDato) {
        LocalDate fodselsdato = FnrUtils.utledFodselsdatoForFnr(fnr);
        int alder = FnrUtils.antallAarSidenDato(fodselsdato,dagensDato);

        boolean oppfyllerKravOmInaktivitet = Objects.isNull(inaktiveringsdato) || erDatoEldreEnnEllerLikAar(dagensDato, inaktiveringsdato, ANTALL_AAR_ISERV);
        boolean oppfyllerKravOmAlder = alder >= MIN_ALDER_AUTOMATISK_REGISTRERING && alder <= MAX_ALDER_AUTOMATISK_REGISTRERING;

        return oppfyllerKravOmInaktivitet && oppfyllerKravOmAlder;
    }


    static boolean erDatoEldreEnnEllerLikAar(LocalDate dagensDato, LocalDate dato, int aar) {
        return FnrUtils.antallAarSidenDato(dato, dagensDato) >= 2;
    }
}
