package no.nav.fo.veilarboppfolging.ws.provider.startregistrering;


import java.time.LocalDate;
import java.util.Objects;

import static no.nav.fo.veilarboppfolging.ws.provider.startregistrering.FnrUtils.antallAarSidenDato;
import static no.nav.fo.veilarboppfolging.ws.provider.startregistrering.FnrUtils.utledFodselsdatoForFnr;

public class StartRegistreringUtils {

    static final int ANTALL_AAR_ISERV = 2;
    static final int MIN_ALDER_AUTOMATISK_REGISTRERING = 30;
    static final int MAX_ALDER_AUTOMATISK_REGISTRERING = 59;

    public static boolean oppfyllerKravOmAutomatiskRegistrering(String fnr, LocalDate inaktiveringsdato, LocalDate dagensDato) {
        LocalDate fodselsdato = utledFodselsdatoForFnr(fnr);
        int alder = antallAarSidenDato(fodselsdato,dagensDato);

        boolean oppfyllerKravOmInaktivitet = Objects.isNull(inaktiveringsdato) || erDatoEldreEnnEllerLikAar(dagensDato, inaktiveringsdato, ANTALL_AAR_ISERV);
        boolean oppfyllerKravOmAlder = alder >= MIN_ALDER_AUTOMATISK_REGISTRERING && alder <= MAX_ALDER_AUTOMATISK_REGISTRERING;

        return oppfyllerKravOmInaktivitet && oppfyllerKravOmAlder;
    }


    static boolean erDatoEldreEnnEllerLikAar(LocalDate dagensDato, LocalDate dato, int aar) {
        return antallAarSidenDato(dato, dagensDato) >= 2;
    }
}
