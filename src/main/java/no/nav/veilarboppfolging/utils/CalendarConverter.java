package no.nav.veilarboppfolging.utils;

import org.slf4j.Logger;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.GregorianCalendar;

import static org.slf4j.LoggerFactory.getLogger;

public class CalendarConverter {

    private static final Logger LOG = getLogger(CalendarConverter.class);

    public static XMLGregorianCalendar convertDateToXMLGregorianCalendar(LocalDate date) {
        final GregorianCalendar gregorianCalendar = GregorianCalendar.from(date.atStartOfDay(ZoneId.systemDefault()));
        XMLGregorianCalendar xmlGregorianCalendar = null;
        try {
            xmlGregorianCalendar = DatatypeFactory.newInstance().newXMLGregorianCalendar(gregorianCalendar);
        } catch (DatatypeConfigurationException e) {
            LOG.warn("Konvertering av dato \"" + date + "\" til XMLGregorianCalendar feilet.", e);
        }
        return xmlGregorianCalendar;
    }

}
