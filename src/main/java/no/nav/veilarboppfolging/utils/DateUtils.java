package no.nav.veilarboppfolging.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.GregorianCalendar;


public class DateUtils {

    private Logger log = LoggerFactory.getLogger(this.getClass());
    
    public static XMLGregorianCalendar now() {
        DatatypeFactory factory = DatatypeFactory.newInstance();
        GregorianCalendar calendar = new GregorianCalendar();
        return factory.newXMLGregorianCalendar(calendar);
    }

    public static XMLGregorianCalendar convertDateToXMLGregorianCalendar(LocalDate date) {
        final GregorianCalendar gregorianCalendar = GregorianCalendar.from(date.atStartOfDay(ZoneId.systemDefault()));

        try {
            return DatatypeFactory.newInstance().newXMLGregorianCalendar(gregorianCalendar);
        } catch (DatatypeConfigurationException e) {
            log.warn("Konvertering av dato \"" + date + "\" til XMLGregorianCalendar feilet.", e);
        }

        return null;
    }

    public static boolean between(ZonedDateTime start, ZonedDateTime stop, ZonedDateTime date) {
        return !date.isBefore(start) && (stop == null || !date.isAfter(stop));
    }

}
