package no.nav.veilarboppfolging.service;

import lombok.extern.slf4j.Slf4j;
import no.nav.common.abac.AbacUtils;
import no.nav.common.abac.Pep;
import no.nav.common.abac.XacmlResponseParser;
import no.nav.common.abac.constants.AbacDomain;
import no.nav.common.abac.constants.NavAttributter;
import no.nav.common.abac.constants.StandardAttributter;
import no.nav.common.abac.domain.AbacPersonId;
import no.nav.common.abac.domain.Attribute;
import no.nav.common.abac.domain.request.*;
import no.nav.common.abac.domain.response.XacmlResponse;
import no.nav.common.auth.subject.IdentType;
import no.nav.common.auth.subject.SsoToken;
import no.nav.common.auth.subject.SubjectHandler;
import no.nav.common.client.aktorregister.AktorregisterClient;
import no.nav.common.utils.Credentials;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import static java.lang.String.format;
import static java.util.Optional.ofNullable;
import static no.nav.common.abac.XacmlRequestBuilder.lagEnvironment;
import static no.nav.common.abac.XacmlRequestBuilder.personIdAttribute;

@Slf4j
@Service
public class AuthService {

    private final Pep veilarbPep;

    private final AktorregisterClient aktorregisterClient;

    private final Credentials serviceUserCredentials;

    @Autowired
    public AuthService(Pep veilarbPep, AktorregisterClient aktorregisterClient, Credentials serviceUserCredentials) {
        this.veilarbPep = veilarbPep;
        this.aktorregisterClient = aktorregisterClient;
        this.serviceUserCredentials = serviceUserCredentials;
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

    public boolean erSystemBruker() {
        IdentType identType = SubjectHandler.getIdentType().orElse(null);
        return IdentType.Systemressurs.equals(identType);
    }

    public boolean erEksternBruker() {
        IdentType identType = SubjectHandler.getIdentType().orElse(null);
        return IdentType.EksternBruker.equals(identType);
    }

    public boolean harTilgangTilEnhet(String enhetId) {
        //  ABAC feiler hvis man spør om tilgang til udefinerte enheter (null) men tillater å spørre om tilgang
        //  til enheter som ikke finnes (f.eks. tom streng)
        //  Ved å konvertere null til tom streng muliggjør vi å spørre om tilgang til enhet for brukere som
        //  ikke har enhet. Sluttbrukere da få permit mens veiledere vil få deny.
        return veilarbPep.harTilgangTilEnhet(getInnloggetBrukerToken(), ofNullable(enhetId).orElse(""));
    }

    public boolean harVeilederSkriveTilgangTilFnr(String veilederId, String fnr) {
        AbacPersonId personId = AbacPersonId.aktorId(getAktorIdOrThrow(fnr));
        return veilarbPep.harVeilederTilgangTilPerson(veilederId, ActionId.WRITE, personId);
    }

    public void sjekkLesetilgangMedFnr(String fnr) {
        sjekkLesetilgangMedAktorId(getAktorIdOrThrow(fnr));
    }

    public void sjekkLesetilgangMedAktorId(String aktorId) {
        if (!veilarbPep.harTilgangTilPerson(getInnloggetBrukerToken(), ActionId.READ, AbacPersonId.aktorId(aktorId))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }

    public void sjekkSkrivetilgangMedFnr(String fnr) {
        sjekkSkrivetilgangMedAktorId(getAktorIdOrThrow(fnr));
    }

    public void sjekkSkrivetilgangMedAktorId(String aktorId) {
        if (!veilarbPep.harTilgangTilPerson(getInnloggetBrukerToken(), ActionId.WRITE, AbacPersonId.aktorId(aktorId))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }

    public void sjekkTilgangTilEnhet(String enhetId) {
        if (!harTilgangTilEnhet(enhetId)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
    }

    public void sjekkTilgangTilPersonMedNiva3(String aktorId) {
        XacmlRequest tilgangTilNiva3Request = lagSjekkTilgangTilNiva3Request(serviceUserCredentials.username, getInnloggetBrukerToken(), aktorId);

        XacmlResponse response = veilarbPep.getAbacClient().sendRequest(tilgangTilNiva3Request);

        if (!XacmlResponseParser.harTilgang(response)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
    }

    private XacmlRequest lagSjekkTilgangTilNiva3Request(String serviceUserName, String userOidcToken, String aktorId) {
        String oidcTokenBody = AbacUtils.extractOidcTokenBody(userOidcToken);
        Environment environment = lagEnvironment(serviceUserName);
        environment.getAttribute().add(new Attribute(NavAttributter.ENVIRONMENT_FELLES_OIDC_TOKEN_BODY, oidcTokenBody));

        Action action = new Action();
        action.addAttribute(new Attribute(StandardAttributter.ACTION_ID, ActionId.READ.name()));

        Resource resource = new Resource();
        resource.getAttribute().add(new Attribute(NavAttributter.RESOURCE_FELLES_RESOURCE_TYPE, NavAttributter.RESOURCE_VEILARB_UNDER_OPPFOLGING));
        resource.getAttribute().add(new Attribute(NavAttributter.RESOURCE_FELLES_DOMENE, AbacDomain.VEILARB_DOMAIN));
        resource.getAttribute().add(personIdAttribute(AbacPersonId.aktorId(aktorId)));

        Request request = new Request()
                .withEnvironment(environment)
                .withAction(action)
                .withResource(resource);

        return new XacmlRequest().withRequest(request);
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

    public String getFnrOrThrow(String aktorId) {
        return aktorregisterClient.hentFnr(aktorId);
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
