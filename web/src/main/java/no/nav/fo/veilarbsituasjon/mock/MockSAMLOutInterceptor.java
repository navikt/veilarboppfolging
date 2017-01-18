package no.nav.fo.veilarbsituasjon.mock;

import no.nav.modig.security.ws.attributes.SAMLAttributes;


class MockSAMLOutInterceptor implements SAMLAttributes {

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
        return "InternBruker";
    }

    @Override
    public String getConsumerId() {
        return "srvveilarbsituasjon";
    }
}