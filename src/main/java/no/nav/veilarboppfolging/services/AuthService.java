package no.nav.veilarboppfolging.services;

import lombok.extern.slf4j.Slf4j;
import no.nav.common.abac.Pep;
import no.nav.common.abac.domain.AbacPersonId;
import no.nav.common.abac.domain.request.ActionId;
import no.nav.common.auth.subject.IdentType;
import no.nav.common.auth.subject.SsoToken;
import no.nav.common.auth.subject.SubjectHandler;
import no.nav.common.client.aktorregister.AktorregisterClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import static java.lang.String.format;

@Slf4j
@Service
public class AuthService {

    private final Pep veilarbPep;

    private final AktorregisterClient aktorregisterClient;

    @Autowired
    public AuthService(Pep veilarbPep, AktorregisterClient aktorregisterClient) {
        this.veilarbPep = veilarbPep;
        this.aktorregisterClient = aktorregisterClient;
    }

    private void skalVere(IdentType forventetIdentType) {
        IdentType identType = SubjectHandler.getIdentType().orElse(null);
        if (identType != forventetIdentType) {
            log.warn(format("Forventet bruker av type %s, men fikk %s", identType, forventetIdentType));
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
    }

    public void skalVereInternBruker() {
        skalVere(IdentType.InternBruker);
    }

    public void skalVereSystemBruker() {
        skalVere(IdentType.Systemressurs);
    }

    public boolean erInternBruker() {
        IdentType identType = SubjectHandler.getIdentType().orElse(null);
        return IdentType.InternBruker.equals(identType);
    }

    public boolean erEksternBruker() {
        IdentType identType = SubjectHandler.getIdentType().orElse(null);
        return IdentType.EksternBruker.equals(identType);
    }

    public boolean harTilgangTilEnhet(String enhetId) {
        return veilarbPep.harVeilederTilgangTilEnhet(getInnloggetVeilederIdent(), enhetId);
    }

    public boolean harVeilederSkriveTilgangTilFnr(String veilederId, String fnr) {
        AbacPersonId personId = AbacPersonId.aktorId(getAktorIdOrThrow(fnr));
        return veilarbPep.harVeilederTilgangTilPerson(veilederId, ActionId.WRITE, personId);
    }

    public void sjekkLesetilgangMedFnr(String fnr) {
        sjekkLesetilgangMedAktorId(getAktorIdOrThrow(fnr));
    }

    public void sjekkLesetilgangMedAktorId(String aktorId) {
        if (veilarbPep.harTilgangTilPerson(getInnloggetBrukerToken(), ActionId.READ, AbacPersonId.aktorId(aktorId))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
    }

    public void sjekkSkrivetilgangMedFnr(String fnr) {
        AbacPersonId personId = AbacPersonId.aktorId(getAktorIdOrThrow(fnr));
        if (veilarbPep.harTilgangTilPerson(getInnloggetBrukerToken(), ActionId.WRITE, personId)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
    }

    // TODO: Det er hårete å måtte skille på ekstern og intern
    //  Lag istedenfor en egen controller for interne operasjoner og en annen for eksterne
    public String hentIdentForEksternEllerIntern(String queryParamFnr) {
        String fnr;

        if (erInternBruker()) {
            fnr = queryParamFnr;
        } else if (erEksternBruker()) {
            fnr = getInnloggetBrukerIdent();
        } else {
            // Systembruker har ikke tilgang
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        if (fnr == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mangler fnr");
        }

        return fnr;
    }

    public String getAktorIdOrThrow(String fnr) {
        return aktorregisterClient.hentAktorId(fnr);
    }

    public String getInnloggetBrukerToken() {
        return SubjectHandler
                .getSsoToken()
                .map(SsoToken::getToken)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fant ikke token for innlogget bruker"));
    }

    // NAV ident, fnr eller annen ID
    public String getInnloggetBrukerIdent() {
        return SubjectHandler
                .getIdent()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fant ikke ident for innlogget bruker"));
    }

    public String getInnloggetVeilederIdent() {
        if (!erInternBruker()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        return getInnloggetBrukerIdent();
    }

}
