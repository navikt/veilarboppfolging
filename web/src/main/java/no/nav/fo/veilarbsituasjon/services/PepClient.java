package no.nav.fo.veilarbsituasjon.services;

import no.nav.brukerdialog.security.context.SubjectHandler;
import no.nav.brukerdialog.security.domain.OidcCredential;
import no.nav.sbl.dialogarena.common.abac.pep.Pep;
import no.nav.sbl.dialogarena.common.abac.pep.domain.response.BiasedDecisionResponse;
import no.nav.sbl.dialogarena.common.abac.pep.domain.response.Decision;
import no.nav.sbl.dialogarena.common.abac.pep.exception.PepException;
import org.slf4j.Logger;

import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotAuthorizedException;

import static org.slf4j.LoggerFactory.getLogger;

public class PepClient {

    final private Pep pep;
    private static final Logger LOG = getLogger(PepClient.class);

    public PepClient(Pep pep) {
        this.pep = pep;
    }

    public boolean isServiceCallAllowed(String fnr) {
        BiasedDecisionResponse callAllowed;

        try {
            callAllowed = pep.isServiceCallAllowedWithOidcToken(getToken(), "veilarb", fnr);
        } catch (PepException e) {
            LOG.error("Something went wrong in PEP", e);
            throw new InternalServerErrorException("something went wrong in PEP", e);
        }
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
