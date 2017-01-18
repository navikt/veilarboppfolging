package no.nav.fo.veilarbsituasjon.mock;

import no.nav.fo.veilarbsituasjon.mock.MockSAMLOutInterceptor;
import no.nav.modig.security.ws.AbstractSAMLOutInterceptor;
import no.nav.modig.security.ws.SAMLCallbackHandler;

import java.util.Map;

public class TestOutInterceptor extends AbstractSAMLOutInterceptor {

    public TestOutInterceptor() {
        super(false);
    }

    public TestOutInterceptor(Map<String, Object> props) {
        super(false, props);
    }

    protected SAMLCallbackHandler getCallbackHandler() {
        return new SAMLCallbackHandler(new MockSAMLOutInterceptor());
    }
}
