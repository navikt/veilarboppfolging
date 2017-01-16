package no.nav.fo.veilarbsituasjon.config;

import no.nav.modig.security.ws.attributes.SAMLAttributes;


public class MockSAMLOutInterceptor implements SAMLAttributes {

    @Override
    public String getUid() {
        return "Z990300";
    }

    @Override
    public String getAuthenticationLevel() {
        return "4";
    }

    @Override
    public String getIdentType() {
        return "Internbruker";
    }

    @Override
    public String getConsumerId() {
        return "srvveilarbsituasjon";
    }
}