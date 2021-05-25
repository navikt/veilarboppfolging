package no.nav.veilarboppfolging.service;

import lombok.extern.slf4j.Slf4j;
import no.nav.common.abac.AbacUtils;
import no.nav.common.abac.Pep;
import no.nav.common.abac.XacmlResponseParser;
import no.nav.common.abac.constants.AbacDomain;
import no.nav.common.abac.constants.NavAttributter;
import no.nav.common.abac.constants.StandardAttributter;
import no.nav.common.abac.domain.Attribute;
import no.nav.common.abac.domain.request.*;
import no.nav.common.abac.domain.response.XacmlResponse;
import no.nav.common.auth.context.AuthContextHolder;
import no.nav.common.client.aktoroppslag.AktorOppslagClient;
import no.nav.common.client.aktorregister.AktorregisterClient;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.EnhetId;
import no.nav.common.types.identer.Fnr;
import no.nav.common.types.identer.NavIdent;
import no.nav.common.utils.Credentials;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static java.util.Optional.ofNullable;
import static no.nav.common.abac.XacmlRequestBuilder.lagEnvironment;
import static no.nav.common.abac.XacmlRequestBuilder.personIdAttribute;

@Slf4j
@Service
public class AuthService {

    private final AuthContextHolder authContextHolder;

    private final Pep veilarbPep;

    private final AktorOppslagClient aktorOppslagClient;

    private final AktorregisterClient aktorregisterClient;

    private final Credentials serviceUserCredentials;

    @Autowired
    public AuthService(AuthContextHolder authContextHolder, Pep veilarbPep, AktorOppslagClient aktorOppslagClient, AktorregisterClient aktorregisterClient, Credentials serviceUserCredentials) {
        this.authContextHolder = authContextHolder;
        this.veilarbPep = veilarbPep;
        this.aktorOppslagClient = aktorOppslagClient;
        this.aktorregisterClient = aktorregisterClient;
        this.serviceUserCredentials = serviceUserCredentials;
    }

    public void skalVereInternBruker() {
        if (!authContextHolder.erInternBruker()){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Ugyldig bruker type");
        }
    }

    public void skalVereSystemBruker() {
        if (!authContextHolder.erSystemBruker()){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Ugyldig bruker type");
        }
    }

    public boolean erInternBruker() {
        return authContextHolder.erInternBruker();
    }

    public boolean erSystemBruker() {
        return authContextHolder.erSystemBruker();
    }

    public boolean erEksternBruker() {
        return authContextHolder.erEksternBruker();
    }

    public boolean harTilgangTilEnhet(String enhetId) {
        //  ABAC feiler hvis man spør om tilgang til udefinerte enheter (null) men tillater å spørre om tilgang
        //  til enheter som ikke finnes (f.eks. tom streng)
        //  Ved å konvertere null til tom streng muliggjør vi å spørre om tilgang til enhet for brukere som
        //  ikke har enhet. Sluttbrukere da få permit mens veiledere vil få deny.
        return veilarbPep.harTilgangTilEnhet(getInnloggetBrukerToken(), ofNullable(enhetId).map(EnhetId::of).orElse(EnhetId.of("")));
    }

    public boolean harTilgangTilEnhetMedSperre(String enhetId) {
        return veilarbPep.harTilgangTilEnhetMedSperre(getInnloggetBrukerToken(), EnhetId.of(enhetId));
    }

    public boolean harVeilederSkriveTilgangTilFnr(String veilederId, String fnr) {
        return veilarbPep.harVeilederTilgangTilPerson(NavIdent.of(veilederId), ActionId.WRITE, AktorId.of(getAktorIdOrThrow(fnr)));
    }

    public void sjekkLesetilgangMedFnr(String fnr) {
        sjekkLesetilgangMedAktorId(getAktorIdOrThrow(fnr));
    }

    public void sjekkLesetilgangMedAktorId(String aktorId) {
        if (!veilarbPep.harTilgangTilPerson(getInnloggetBrukerToken(), ActionId.READ, AktorId.of(aktorId))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }

    public void sjekkSkrivetilgangMedFnr(String fnr) {
        sjekkSkrivetilgangMedAktorId(getAktorIdOrThrow(fnr));
    }

    public void sjekkSkrivetilgangMedAktorId(String aktorId) {
        if (!veilarbPep.harTilgangTilPerson(getInnloggetBrukerToken(), ActionId.WRITE, AktorId.of(aktorId))) {
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
        resource.getAttribute().add(personIdAttribute(AktorId.of(aktorId)));

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

    public String getAktorIdOrThrow(Fnr fnr) {
        return aktorOppslagClient.hentAktorId(fnr).get();
    }

    public String getAktorIdOrThrow(String fnr) {
        return getAktorIdOrThrow(Fnr.of(fnr));
    }

    public String getFnrOrThrow(String aktorId) {
        return aktorOppslagClient.hentFnr(AktorId.of(aktorId)).get();
    }

    public List<AktorId> getAlleAktorIderOrThrow(String fnr) {
        return aktorregisterClient.hentAktorIder(Fnr.of(fnr));
    }

    public String getInnloggetBrukerToken() {
        return authContextHolder.getIdTokenString().orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Fant ikke token for innlogget bruker"));
    }

    // NAV ident, fnr eller annen ID
    public String getInnloggetBrukerIdent() {
        return authContextHolder.getSubject().orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "NAV ident is missing"));
    }

    public String getInnloggetVeilederIdent() {
        if (!erInternBruker()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        return getInnloggetBrukerIdent();
    }
}
