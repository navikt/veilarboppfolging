package no.nav.veilarboppfolging.mock;

import no.nav.common.abac.AbacClient;
import no.nav.common.abac.Pep;
import no.nav.common.abac.domain.request.ActionId;
import no.nav.common.abac.domain.request.XacmlRequest;
import no.nav.common.abac.domain.response.XacmlResponse;
import no.nav.common.health.HealthCheckResult;
import no.nav.common.types.identer.EksternBrukerId;
import no.nav.common.types.identer.EnhetId;
import no.nav.common.types.identer.NavIdent;

public class PepMock implements Pep {

    private final AbacClient abacClient;

    public PepMock() {
        this.abacClient = new AbacClientMock();
    }

    @Override
    public boolean harVeilederTilgangTilEnhet(NavIdent navIdent, EnhetId enhetId) {
        return false;
    }

    @Override
    public boolean harTilgangTilEnhet(String s, EnhetId enhetId) {
        return true;
    }

    @Override
    public boolean harTilgangTilEnhetMedSperre(String s, EnhetId enhetId) {
        return false;
    }

    @Override
    public boolean harTilgangTilEnhetMedSperre(NavIdent navIdent, EnhetId enhetId) {
        return true;
    }

    @Override
    public boolean harVeilederTilgangTilPerson(NavIdent navIdent, ActionId actionId, EksternBrukerId eksternBrukerId) {
        return false;
    }

    @Override
    public boolean harTilgangTilPerson(String s, ActionId actionId, EksternBrukerId eksternBrukerId) {
        return true;
    }

    @Override
    public boolean harTilgangTilOppfolging(String s) {
        return false;
    }

    @Override
    public boolean harVeilederTilgangTilModia(String s) {
        return false;
    }

    @Override
    public boolean harVeilederTilgangTilKode6(NavIdent navIdent) {
        return false;
    }

    @Override
    public boolean harVeilederTilgangTilKode7(NavIdent navIdent) {
        return false;
    }

    @Override
    public boolean harVeilederTilgangTilEgenAnsatt(NavIdent navIdent) {
        return false;
    }

    @Override
    public AbacClient getAbacClient() {
        return abacClient;
    }

    static class AbacClientMock implements AbacClient {
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
