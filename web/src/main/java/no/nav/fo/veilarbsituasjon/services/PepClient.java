package no.nav.fo.veilarbsituasjon.services;

import no.nav.brukerdialog.security.context.SubjectHandler;
import no.nav.sbl.dialogarena.common.abac.pep.Pep;
import no.nav.sbl.dialogarena.common.abac.pep.domain.response.BiasedDecisionResponse;
import no.nav.sbl.dialogarena.common.abac.pep.domain.response.Decision;
import no.nav.sbl.dialogarena.common.abac.pep.exception.PepException;

import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotAuthorizedException;

public class PepClient {

    final private Pep pep;

    public PepClient(Pep pep) {
        this.pep = pep;
    }

    public boolean isServiceCallAllowed(String fnr) {
        final String ident = SubjectHandler.getSubjectHandler().getUid();
        BiasedDecisionResponse callAllowed;
        try {
            callAllowed = pep.isServiceCallAllowedWithIdent(ident, "veilarb", fnr);
        } catch (PepException e) {
            throw new InternalServerErrorException("something went wrong in PEP", e);
        }
        if (callAllowed.getBiasedDecision().equals(Decision.Deny)) {
            throw new NotAuthorizedException(ident + " doesn't have access to " + fnr);
        }
        return callAllowed.getBiasedDecision().equals(Decision.Permit);
    }

}
