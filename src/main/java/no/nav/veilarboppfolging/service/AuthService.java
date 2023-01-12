package no.nav.veilarboppfolging.service;

import com.nimbusds.jwt.JWTClaimsSet;
import lombok.SneakyThrows;
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
import no.nav.common.auth.context.UserRole;
import no.nav.common.auth.utils.IdentUtils;
import no.nav.common.client.aktoroppslag.AktorOppslagClient;
import no.nav.common.client.aktoroppslag.BrukerIdenter;
import no.nav.common.token_client.client.AzureAdOnBehalfOfTokenClient;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.EnhetId;
import no.nav.common.types.identer.Fnr;
import no.nav.common.types.identer.NavIdent;
import no.nav.common.utils.Credentials;
import no.nav.veilarboppfolging.config.EnvironmentProperties;
import no.nav.veilarboppfolging.utils.DownstreamApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static no.nav.common.abac.XacmlRequestBuilder.lagEnvironment;
import static no.nav.common.abac.XacmlRequestBuilder.personIdAttribute;
import static no.nav.common.auth.Constants.AAD_NAV_IDENT_CLAIM;

@Slf4j
@Service
public class AuthService {

    private final AuthContextHolder authContextHolder;

    private final Pep veilarbPep;

    private final AzureAdOnBehalfOfTokenClient aadOboTokenClient;

    private final AktorOppslagClient aktorOppslagClient;

    private final Credentials serviceUserCredentials;

    private final EnvironmentProperties environmentProperties;

    @Autowired
    public AuthService(AuthContextHolder authContextHolder, Pep veilarbPep, AktorOppslagClient aktorOppslagClient, Credentials serviceUserCredentials, AzureAdOnBehalfOfTokenClient aadOboTokenClient, EnvironmentProperties environmentProperties) {
        this.authContextHolder = authContextHolder;
        this.veilarbPep = veilarbPep;
        this.aktorOppslagClient = aktorOppslagClient;
        this.serviceUserCredentials = serviceUserCredentials;
        this.aadOboTokenClient = aadOboTokenClient;
        this.environmentProperties = environmentProperties;
    }

    public void skalVereEnAv(List<UserRole> roller) {
        UserRole loggedInUserRole = authContextHolder.requireRole();

        if (roller.stream().noneMatch(rolle -> rolle.equals(loggedInUserRole))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, format("Bruker med rolle %s har ikke tilgang", loggedInUserRole));
        }
    }

    public void skalVereInternBruker() {
        if (!authContextHolder.erInternBruker()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bruker er ikke en intern bruker");
        }
    }

    public void skalVereInternEllerSystemBruker() {
        if (!authContextHolder.erInternBruker() && !authContextHolder.erSystemBruker()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bruker er verken en intern eller system bruker");
        }
    }

    public boolean harEksternBrukerTilgang(Fnr fnr) {
        return getInnloggetBrukerIdent().equals(fnr.get());
    }

    public void skalVereSystemBruker() {
        if (!authContextHolder.erSystemBruker()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bruker er ikke en systembruker");
        }
    }

    public void skalVereSystemBrukerFraAzureAd() {
        if (!erSystemBrukerFraAzureAd()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bruker er ikke en systembruker fra azureAd");
        }
    }

    public boolean erInternBruker() {
        return authContextHolder.erInternBruker();
    }

    public boolean erSystemBruker() {
        return authContextHolder.erSystemBruker();
    }

    public boolean erSystemBrukerFraAzureAd() {
        return erSystemBruker() && harAADRolleForSystemTilSystemTilgang();
    }

    private boolean harAADRolleForSystemTilSystemTilgang() {
        return authContextHolder.getIdTokenClaims()
                .flatMap(claims -> {
                    try {
                        return Optional.ofNullable(claims.getStringListClaim("roles"));
                    } catch (ParseException e) {
                        return Optional.empty();
                    }
                })
                .orElse(emptyList())
                .contains("access_as_application");
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

    public boolean harVeilederSkriveTilgangTilFnr(String veilederId, Fnr fnr) {
        return veilarbPep.harVeilederTilgangTilPerson(NavIdent.of(veilederId), ActionId.WRITE, getAktorIdOrThrow(fnr));
    }

    public void sjekkLesetilgangMedFnr(Fnr fnr) {
        if (authContextHolder.erEksternBruker()) {
            if (!harEksternBrukerTilgang(fnr)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Ekstern bruker har ikke tilgang på andre brukere enn seg selv");
            }
        } else {
            sjekkLesetilgangMedAktorId(getAktorIdOrThrow(fnr));
        }
    }

    public void sjekkLesetilgangMedAktorId(AktorId aktorId) {
        sjekkTilgang(ActionId.READ, aktorId);

    }

    public void sjekkSkrivetilgangMedAktorId(AktorId aktorId) {
        sjekkTilgang(ActionId.WRITE, aktorId);
    }

    private void sjekkTilgang(ActionId actionId, AktorId aktorId) {
        Optional<NavIdent> navident = getNavIdentClaimHvisTilgjengelig();

        if (navident.isEmpty()) {
            if (!veilarbPep.harTilgangTilPerson(getInnloggetBrukerToken(), actionId, aktorId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }
        } else {
            if (!veilarbPep.harVeilederTilgangTilPerson(navident.orElseThrow(), actionId, aktorId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }
        }
    }


    public void sjekkTilgangTilEnhet(String enhetId) {
        if (!harTilgangTilEnhet(enhetId)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
    }

    public void sjekkTilgangTilPersonMedNiva3(AktorId aktorId) {
        XacmlRequest tilgangTilNiva3Request = lagSjekkTilgangTilNiva3Request(serviceUserCredentials.username, getInnloggetBrukerToken(), aktorId);

        XacmlResponse response = veilarbPep.getAbacClient().sendRequest(tilgangTilNiva3Request);

        if (!XacmlResponseParser.harTilgang(response)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
    }

    private XacmlRequest lagSjekkTilgangTilNiva3Request(String serviceUserName, String userOidcToken, AktorId aktorId) {
        String oidcTokenBody = AbacUtils.extractOidcTokenBody(userOidcToken);
        Environment environment = lagEnvironment(serviceUserName);
        environment.getAttribute().add(new Attribute(NavAttributter.ENVIRONMENT_FELLES_OIDC_TOKEN_BODY, oidcTokenBody));

        Action action = new Action();
        action.addAttribute(new Attribute(StandardAttributter.ACTION_ID, ActionId.READ.name()));

        Resource resource = new Resource();
        resource.getAttribute().add(new Attribute(NavAttributter.RESOURCE_FELLES_RESOURCE_TYPE, NavAttributter.RESOURCE_VEILARB_UNDER_OPPFOLGING));
        resource.getAttribute().add(new Attribute(NavAttributter.RESOURCE_FELLES_DOMENE, AbacDomain.VEILARB_DOMAIN));
        resource.getAttribute().add(personIdAttribute(aktorId));

        Request request = new Request()
                .withEnvironment(environment)
                .withAction(action)
                .withResource(resource);

        return new XacmlRequest().withRequest(request);
    }

    // TODO: Det er hårete å måtte skille på ekstern og intern
    //  En alternativ løsning er å ha egne endepunkter for de forskjellige rollene
    public Fnr hentIdentForEksternEllerIntern(Fnr queryParamFnr) {
        if (erSystemBruker()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        return hentIdentFraQueryParamEllerToken(queryParamFnr);
    }

    public Fnr hentIdentFraQueryParamEllerToken(Fnr queryParamFnr) {
        Fnr fnr;

        switch (authContextHolder.requireRole()) {
            case EKSTERN:
                fnr = Fnr.of(getInnloggetBrukerIdent());
                break;
            case INTERN:
            case SYSTEM:
                fnr = queryParamFnr;
                break;
            default:
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        if (fnr == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mangler fnr");
        }

        return fnr;
    }

    public AktorId getAktorIdOrThrow(Fnr fnr) {
        return aktorOppslagClient.hentAktorId(fnr);
    }

    public Fnr getFnrOrThrow(AktorId aktorId) {
        return aktorOppslagClient.hentFnr(aktorId);
    }

    public List<AktorId> getAlleAktorIderOrThrow(Fnr fnr) {
        BrukerIdenter brukerIdenter = aktorOppslagClient.hentIdenter(fnr);
        List<AktorId> alleAktorIder = new ArrayList<>();
        alleAktorIder.addAll(brukerIdenter.getHistoriskeAktorId());
        alleAktorIder.add(brukerIdenter.getAktorId());
        return alleAktorIder;
    }

    public String getInnloggetBrukerToken() {
        return authContextHolder.getIdTokenString().orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Fant ikke token for innlogget bruker"));
    }

    public Optional<String> getAadOboTokenForTjeneste(DownstreamApi api) {
        if (erAadOboToken()) {
            String scope = "api://" + api.cluster + "." + api.namespace + "." + api.serviceName + "/.default";
            return Optional.of(aadOboTokenClient.exchangeOnBehalfOfToken(scope, getInnloggetBrukerToken()));
        }
        return Optional.empty();
    }

    private boolean erAadOboToken() {
        Optional<String> navIdentClaim = authContextHolder.getIdTokenClaims()
                .flatMap((claims) -> authContextHolder.getStringClaim(claims, "NAVident"));
        return authContextHolder.getIdTokenClaims().map(JWTClaimsSet::getIssuer).filter(environmentProperties.getNaisAadIssuer()::equals).isPresent()
                && authContextHolder.getIdTokenClaims().map(x -> x.getClaim("oid")).isPresent()
                && navIdentClaim.isPresent();
    }

    // NAV ident, fnr eller annen ID
    public String getInnloggetBrukerIdent() {
        return authContextHolder.getUid()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User ident is missing"));
    }

    public String getInnloggetVeilederIdent() {
        if (!erInternBruker()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        return getInnloggetBrukerIdent();
    }

    @SneakyThrows
    private Optional<NavIdent> getNavIdentClaimHvisTilgjengelig() {
        if (erInternBruker()) {
            return Optional.ofNullable(authContextHolder.requireIdTokenClaims().getStringClaim(AAD_NAV_IDENT_CLAIM))
                    .filter(IdentUtils::erGydligNavIdent)
                    .map(NavIdent::of);
        }
        return empty();
    }


    public void sjekkAtApplikasjonErIAllowList(String[] allowlist) {
        sjekkAtApplikasjonErIAllowList(List.of(allowlist));
    }
    @SneakyThrows
    public void sjekkAtApplikasjonErIAllowList(List<String> allowlist) {
        String appname = hentApplikasjonFraContext();
        if (allowlist.contains(appname)) {
            return;
        }
        log.error("Applikasjon {} er ikke allowlist", appname);
        throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }

    public static boolean isAzure(Optional<JWTClaimsSet> maybeClaims) {
        return maybeClaims.map(claims -> hasIssuer(claims, "microsoftonline.com")).orElse(false);
    }

    public static boolean isTokenX(Optional<JWTClaimsSet> maybeClaims) {
        return maybeClaims.map(claims -> hasIssuer(claims, "tokendings")).orElse(false);
    }
    private static boolean hasIssuer(JWTClaimsSet claims, String issuerSubString) {
        var fullIssuerString = claims.getIssuer();
        return fullIssuerString.contains(issuerSubString);
    }

    private Optional<String> getFullAppName() { //  "cluster:team:app"
        var maybeClaims = authContextHolder.getIdTokenClaims();
        if (isAzure(maybeClaims)) {
            return maybeClaims.flatMap(claims -> getStringClaimOrEmpty(claims, "azp_name"));
        } else if (isTokenX(maybeClaims)) {
            return maybeClaims.flatMap(claims -> getStringClaimOrEmpty(claims, "client_id"));
        } else {
            return Optional.empty();
        }
    }

    public String hentApplikasjonFraContext() {
        return getFullAppName()
                .map(claim -> claim.split(":"))
                .filter(claims -> claims.length == 3)
                .map(claims -> claims[2])
                .orElse("");
    }

    private static Optional<String> getStringClaimOrEmpty(JWTClaimsSet claims, String claimName) {
        try {
            return ofNullable(claims.getStringClaim(claimName));
        } catch (Exception e) {
            return empty();
        }
    }
}
