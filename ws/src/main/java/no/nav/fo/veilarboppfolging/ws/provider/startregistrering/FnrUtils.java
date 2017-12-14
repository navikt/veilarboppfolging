package no.nav.fo.veilarboppfolging.ws.provider.startregistrering;

import no.bekk.bekkopen.person.Fodselsnummer;
import no.bekk.bekkopen.person.FodselsnummerValidator;

import java.time.LocalDate;
import java.time.Period;


public class FnrUtils {

    public static LocalDate utledFodselsdatoForFnr(String fnr) {
        Fodselsnummer fodselsnummer = FodselsnummerValidator.getFodselsnummer(fnr);

        return LocalDate.of(
                Integer.parseInt(fodselsnummer.getBirthYear()),
                Integer.parseInt(fodselsnummer.getDayInMonth()),
                Integer.parseInt(fodselsnummer.getDayInMonth())
        );
    }

    public static int antallAarSidenDato(LocalDate dato, LocalDate dagensDato) {
        return Period.between(dato, dagensDato).getYears();
    }
}
