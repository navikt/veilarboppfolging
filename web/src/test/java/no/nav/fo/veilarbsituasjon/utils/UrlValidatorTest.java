package no.nav.fo.veilarbsituasjon.utils;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class UrlValidatorTest {

    @Test
    public void shouldNotBeValid() {
        assertTrue(UrlValidator.isInvalidUrl("htp://veilarb.com"));
        assertTrue(UrlValidator.isInvalidUrl("http://veilarb.com"));
    }

    @Test
    public void shouldBeValid() {
        assertTrue(UrlValidator.isValidUrl("https://veilarb.com"));
    }
}