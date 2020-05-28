package no.nav.fo.veilarboppfolging.utils;

import lombok.SneakyThrows;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.GregorianCalendar;

public class DateUtils {

    @SneakyThrows
    public static XMLGregorianCalendar now() {
        DatatypeFactory factory = DatatypeFactory.newInstance();
        GregorianCalendar calendar = new GregorianCalendar();
        return factory.newXMLGregorianCalendar(calendar);
    }

    public static ZonedDateTime toZonedDateTime(Date date) {
        return ZonedDateTime.ofInstant(date.toInstant(), ZoneId.of("Europe/Oslo"));
    }

    public static ZonedDateTime toZonedDateTime(Timestamp timestamp) {
        return ZonedDateTime.ofInstant(timestamp.toInstant(), ZoneId.of("Europe/Oslo"));
    }
}
