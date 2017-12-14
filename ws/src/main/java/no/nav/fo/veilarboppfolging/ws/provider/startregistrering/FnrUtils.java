package no.nav.fo.veilarboppfolging.ws.provider.startregistrering;

import java.time.LocalDate;
import java.time.Period;


public class FnrUtils {

    private static final String fnrPattern = "\\d{11}";

    //Utleder fodselsdato basert på fodselsnummer.
    // Benytter ikke kontrollsiffer for å valider om fnr er gyldig eller ikke.
    public static LocalDate utledFodselsdatoForFnr(String fnr) {
        if(!fnr.matches(fnrPattern)) {
            throw new IllegalArgumentException(String.format("%s er ikke et gyldig fnr", fnr));
        }

        int year = Integer.parseInt(fnr.substring(4,6));
        int aarhundre = utledAarhundreFraFnr(fnr);
        int day = Integer.parseInt(fnr.substring(0,2));
        int month = Integer.parseInt(fnr.substring(2,4));

        if(day > 40) { // d-nummer
            day = day - 40;
        }
        if(month > 40) { // h-nummer
            month = month - 40;
        }

        return LocalDate.of(aarhundre * 100 + year, month, day);
    }

    private static int utledAarhundreFraFnr(String fnr) {
        int aarhundre;

        int aar = Integer.parseInt(fnr.substring(4,6));
        int individsiffer = Integer.parseInt(fnr.substring(6,9));

        if(individsiffer < 500 || (individsiffer >= 900 && aar > 39)) { // født på 1900-tallet
            aarhundre = 19;
        } else if (individsiffer >= 500 && individsiffer < 750 && aar >= 54) { // født på 1800-tallet
            aarhundre = 18;
        } else if (individsiffer >= 500 && aar < 40) { // født på 2000-tallet
            aarhundre = 20;
        } else {
            throw new IllegalArgumentException(String.format("%s er ikke et gyldig fnr", fnr));
        }
        return aarhundre;
    }

    public static int antallAarSidenDato(LocalDate dato, LocalDate dagensDato) {
        return Period.between(dato, dagensDato).getYears();
    }
}
