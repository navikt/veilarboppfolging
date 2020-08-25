package no.nav.veilarboppfolging.mock;

import no.nav.common.abac.AbacClient;
import no.nav.common.abac.Pep;
import no.nav.common.abac.domain.AbacPersonId;
import no.nav.common.abac.domain.request.ActionId;
import no.nav.common.abac.domain.request.XacmlRequest;
import no.nav.common.abac.domain.response.XacmlResponse;
import no.nav.common.health.HealthCheckResult;

public class PepMock implements Pep {

    private final AbacClient abacClient;

    public PepMock() {
        this.abacClient = new AbacClientMock();
    }

    @Override
    public boolean harVeilederTilgangTilEnhet(String veilederIdent, String enhetId) {
        return true;
    }

    @Override
    public boolean harVeilederTilgangTilPerson(String veilederIdent, ActionId actionId, AbacPersonId personId) {
        return true;
    }

    @Override
    public boolean harTilgangTilPerson(String innloggetBrukerIdToken, ActionId actionId, AbacPersonId personId) {
        return true;
    }

    @Override
    public boolean harVeilederTilgangTilOppfolging(String innloggetVeilederIdToken) {
        return true;
    }

    @Override
    public boolean harVeilederTilgangTilModia(String innloggetVeilederIdToken) {
        return true;
    }

    @Override
    public boolean harVeilederTilgangTilKode6(String veilederIdent) {
        return true;
    }

    @Override
    public boolean harVeilederTilgangTilKode7(String veilederIdent) {
        return true;
    }

    @Override
    public boolean harVeilederTilgangTilEgenAnsatt(String veilederIdent) {
        return true;
    }

    @Override
    public AbacClient getAbacClient() {
        return abacClient;
    }

    class AbacClientMock implements AbacClient {
        @Override
        public String sendRawRequest(String s) {
            return null;
        }

        @Override
        public XacmlResponse sendRequest(XacmlRequest xacmlRequest) {
            return null;
        }

        @Override
        public HealthCheckResult checkHealth() {
            return HealthCheckResult.healthy();
        }
    }
}
