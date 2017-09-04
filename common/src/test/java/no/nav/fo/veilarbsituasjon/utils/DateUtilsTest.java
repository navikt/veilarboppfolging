package no.nav.fo.veilarbsituasjon.utils;

import org.junit.Test;

import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DateUtilsTest {
    @Test
    public void toTimeStamp() throws Exception {
        String utcString = "2017-04-19T12:21:04.963+02:00";
        String expectedString = "2017-04-19 12:21:04.963";
        Timestamp timestamp = DateUtils.toTimeStamp(utcString);
        String result = timestamp.toString();
        assertEquals(expectedString, result);
    }

    @Test
    public void toZonedDateTime() throws Exception {
        Timestamp timestamp = Timestamp.valueOf("2017-04-19 12:21:04.963");
        String expectedString = "2017-04-19T12:21:04.963+02:00";
        ZonedDateTime zoned = ZonedDateTime.of(timestamp.toLocalDateTime(), ZoneId.of("+02:00"));
        assertEquals(expectedString, zoned.toString());
    }
}