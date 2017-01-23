package no.nav.fo.veilarbsituasjon.utils;


import javax.xml.datatype.*;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.GregorianCalendar;

public class CalendarConverter {
    public static XMLGregorianCalendar convertDateToXMLGregorianCalendar(LocalDate date) {
        GregorianCalendar gregorianCalendar = GregorianCalendar.from(date.atStartOfDay(ZoneId.systemDefault()));
        XMLGregorianCalendar xmlGregorianCalendar = null;
        try {
            xmlGregorianCalendar = DatatypeFactory.newInstance().newXMLGregorianCalendar(gregorianCalendar);
        } catch (DatatypeConfigurationException e) {
            e.printStackTrace();
        }
        return xmlGregorianCalendar;

    }
}
