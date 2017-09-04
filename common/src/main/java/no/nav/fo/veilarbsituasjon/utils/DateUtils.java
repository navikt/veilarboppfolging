package no.nav.fo.veilarbsituasjon.utils;

import lombok.SneakyThrows;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.GregorianCalendar;

import static java.util.Optional.ofNullable;

public class DateUtils {


    private static final DatatypeFactory DATATYPE_FACTORY = getDatatypeFactory();

    @SneakyThrows
    private static DatatypeFactory getDatatypeFactory() {
        return DatatypeFactory.newInstance();
    }

    public static XMLGregorianCalendar xmlCalendar(Date date) {
        return ofNullable(date).map(d->{
            GregorianCalendar cal = new GregorianCalendar();
            cal.setTime(date);
            return DATATYPE_FACTORY.newXMLGregorianCalendar(cal);
        }).orElse(null);
    }

    public static Date getDate(XMLGregorianCalendar xmlGregorianCalendar){
        return ofNullable(xmlGregorianCalendar)
                .map(XMLGregorianCalendar::toGregorianCalendar)
                .map(GregorianCalendar::getTime)
                .orElse(null);
    }

    public static Timestamp toTimeStamp(String utc) {
        return Timestamp.from(ZonedDateTime.parse(utc).toInstant());
    }

    public static ZonedDateTime toZonedDateTime(Timestamp endretTimestamp) {
        LocalDateTime localDateTime = endretTimestamp.toLocalDateTime();
        return ZonedDateTime.of(localDateTime, ZoneId.systemDefault()).withZoneSameInstant(ZoneOffset.UTC);
    }


}
