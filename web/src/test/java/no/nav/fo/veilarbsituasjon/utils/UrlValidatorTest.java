package no.nav.fo.veilarbsituasjon.utils;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class UrlValidatorTest {

    @Test
    void shouldNotBeValid() {
        assertTrue(UrlValidator.isInvalidUrl("htp://foo.com"));
    }

    @Test
    void shouldBeValid() {
        assertTrue(UrlValidator.isValidUrl("http://veilarb.com"));
        assertTrue(UrlValidator.isValidUrl("https://veilarb.com"));
    }
}