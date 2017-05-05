package no.nav.fo.veilarbsituasjon.rest.feed;

import no.nav.fo.veilarbsituasjon.exception.HttpNotSupportedException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.net.MalformedURLException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class UrlValidatorTest {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void shouldNotBeValid() {
        assertTrue(UrlValidator.isInvalidUrl("htp://veilarb.com"));
        assertTrue(UrlValidator.isInvalidUrl("http://veilarb.com"));
    }

    @Test
    public void shouldBeValid() {
        assertTrue(UrlValidator.isValidUrl("https://veilarb.com"));
    }

    @Test
    public void shouldRecognizeHttpUrls() throws Exception {
        assertTrue(UrlValidator.isHttp("http://"));
        assertFalse(UrlValidator.isHttp("https://"));
    }

    @Test
    public void shouldInvalidateMalformedUrl() throws Exception {
        exception.expect(MalformedURLException.class);
        UrlValidator.validateUrl("htps://");
    }

    @Test
    public void shouldInvalidateHttpUrls() throws Exception {
        exception.expect(HttpNotSupportedException.class);
        UrlValidator.validateUrl("http://");
    }
}