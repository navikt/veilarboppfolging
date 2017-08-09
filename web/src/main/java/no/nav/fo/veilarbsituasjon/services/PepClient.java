package no.nav.fo.veilarbsituasjon.services;

import no.nav.brukerdialog.security.context.SubjectHandler;
import no.nav.brukerdialog.security.domain.OidcCredential;
import no.nav.sbl.dialogarena.common.abac.pep.Pep;
import no.nav.sbl.dialogarena.common.abac.pep.domain.response.BiasedDecisionResponse;
import no.nav.sbl.dialogarena.common.abac.pep.domain.response.Decision;
import no.nav.sbl.dialogarena.common.abac.pep.exception.PepException;

import javax.ws.rs.NotAuthorizedException;

public class PepClient {

    final private Pep pep;

    public PepClient(Pep pep) {
        this.pep = pep;
    }

    public boolean isServiceCallAllowed(String fnr) throws PepException {
        BiasedDecisionResponse callAllowed;

        callAllowed = pep.isServiceCallAllowedWithOidcToken(getToken(), "veilarb", fnr);
        if (callAllowed.getBiasedDecision().equals(Decision.Deny)) {
            final String ident = SubjectHandler.getSubjectHandler().getUid();
            throw new NotAuthorizedException(ident + " doesn't have access to " + fnr);
        }
        return callAllowed.getBiasedDecision().equals(Decision.Permit);
    }

    private String getToken() {

        final OidcCredential credential = (OidcCredential) SubjectHandler.getSubjectHandler().getSubject()
                .getPublicCredentials()
                .stream()
                .filter(cred -> cred instanceof OidcCredential).findFirst()
                .get();
        return credential.getToken();
    }

}
