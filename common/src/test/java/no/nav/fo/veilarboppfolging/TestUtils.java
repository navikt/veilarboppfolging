package no.nav.fo.veilarboppfolging;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

import static no.bekk.bekkopen.person.FodselsnummerCalculator.getFodselsnummerForDate;

public class TestUtils {

    public static String getFodselsnummerOnDateMinusYears(LocalDate localDate, int minusYears) {
        Date date = Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).minusYears(minusYears).toInstant());
        return getFodselsnummerForDate(date).toString();

    }
}
