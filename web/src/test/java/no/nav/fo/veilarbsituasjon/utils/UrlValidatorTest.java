package no.nav.fo.veilarbsituasjon.utils;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class UrlValidatorTest {

    @Test
    public void shouldNotBeValid() {
        assertTrue(UrlValidator.isInvalidUrl("htp://foo.com"));
    }

    @Test
    public void shouldBeValid() {
        assertTrue(UrlValidator.isValidUrl("http://veilarb.com"));
        assertTrue(UrlValidator.isValidUrl("https://veilarb.com"));
    }
}