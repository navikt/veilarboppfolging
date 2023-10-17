package no.nav.veilarboppfolging.service;

import com.nimbusds.jwt.JWTClaimsSet;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.abac.Pep;
import no.nav.common.abac.domain.request.*;
import no.nav.common.audit_log.cef.AuthorizationDecision;
import no.nav.common.audit_log.cef.CefMessage;
import no.nav.common.audit_log.cef.CefMessageEvent;
import no.nav.common.audit_log.cef.CefMessageSeverity;
import no.nav.common.audit_log.log.AuditLogger;
import no.nav.common.auth.context.AuthContextHolder;
import no.nav.common.auth.context.UserRole;
import no.nav.common.auth.utils.IdentUtils;
import no.nav.common.client.aktoroppslag.AktorOppslagClient;
import no.nav.common.client.aktoroppslag.BrukerIdenter;
import no.nav.common.token_client.client.AzureAdOnBehalfOfTokenClient;
import no.nav.common.token_client.client.MachineToMachineTokenClient;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.EnhetId;
import no.nav.common.types.identer.Fnr;
import no.nav.common.types.identer.NavIdent;
import no.nav.poao_tilgang.client.*;
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
import java.util.UUID;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static no.nav.common.auth.Constants.AAD_NAV_IDENT_CLAIM;
import static no.nav.veilarboppfolging.utils.SecureLog.secureLog;

@Slf4j
@Service
public class AuthService {

    public static final String UKJENT_NAV_IDENT = "UKJENT";
    private final AuthContextHolder authContextHolder;
    private final AuditLogger auditLogger;

    private final Pep veilarbPep;

    private final AzureAdOnBehalfOfTokenClient aadOboTokenClient;

    private final MachineToMachineTokenClient machineToMachineTokenClient;

    private final AktorOppslagClient aktorOppslagClient;

    private final EnvironmentProperties environmentProperties;

    private final PoaoTilgangClient poaoTilgangClient;

    @Autowired
    public AuthService(AuthContextHolder authContextHolder, Pep veilarbPep, AktorOppslagClient aktorOppslagClient, AzureAdOnBehalfOfTokenClient aadOboTokenClient, MachineToMachineTokenClient machineToMachineTokenClient, EnvironmentProperties environmentProperties, AuditLogger auditLogger, PoaoTilgangClient poaoTilgangClient) {
        this.authContextHolder = authContextHolder;
        this.veilarbPep = veilarbPep;
        this.aktorOppslagClient = aktorOppslagClient;
        this.aadOboTokenClient = aadOboTokenClient;
        this.machineToMachineTokenClient = machineToMachineTokenClient;
        this.environmentProperties = environmentProperties;
        this.auditLogger = auditLogger;
        this.poaoTilgangClient = poaoTilgangClient;
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
        // Når man ikke bruker Pep så må man gjøre auditlogging selv
        var subjectUser = getInnloggetBrukerIdent();
        boolean sammeFnr = subjectUser.equals(fnr.get());
        boolean erNivaa4 = hentSikkerhetsnivaa()
                .filter("Level4"::equals)
                .isPresent();

        boolean isAllowed = erNivaa4 && sammeFnr;

        auditLogger.log(CefMessage.builder()
                .timeEnded(System.currentTimeMillis())
                .applicationName("veilarboppfolging")
                .sourceUserId(subjectUser)
                .event(CefMessageEvent.ACCESS)
                .severity(CefMessageSeverity.INFO)
                .name("veilarboppfolging-audit-log")
                .destinationUserId(fnr.get())
                .extension("msg", isAllowed ? "Ekstern bruker har gjort oppslag på seg selv" : "Ekstern bruker ble nektet innsyn")
                .build());
        return isAllowed;
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
        if (erInternBruker()) {
            Decision decision = poaoTilgangClient.evaluatePolicy(new NavAnsattTilgangTilNavEnhetPolicyInput(hentInnloggetVeilederUUID(), ofNullable(enhetId).orElse(""))).getOrThrow();
            auditLogWithMessageAndDestinationUserId(
                    "Veileder har gjort oppslag på enhet",
                    enhetId,
                    authContextHolder.getNavIdent().orElse(NavIdent.of(UKJENT_NAV_IDENT)).get(),
                    decision.isPermit() ? AuthorizationDecision.PERMIT : AuthorizationDecision.DENY
            );
            return decision.isPermit();
        } else {
            if (!erEksternBruker()) {
                secureLog.warn("Systembruker eller ukjent rolle kom inn i harTilgangTilEnhet, hvis dette skjer må man legge til håndtering");
            }
            return false;
        }
    }

    public boolean harTilgangTilEnhetMedSperre(String enhetId) {
        if (erEksternBruker()) {
            return true; // sluttbruker har altid tilgang til egne data
        }
        Decision decision = poaoTilgangClient.evaluatePolicy(new NavAnsattTilgangTilNavEnhetMedSperrePolicyInput(
                hentInnloggetVeilederUUID(), enhetId
        )).getOrThrow();
        auditLogWithMessageAndDestinationUserId(
                "Veileder har gjort oppslag på enhet med sperre",
                enhetId,
                authContextHolder.getNavIdent().orElse(NavIdent.of(UKJENT_NAV_IDENT)).get(),
                decision.isPermit() ? AuthorizationDecision.PERMIT : AuthorizationDecision.DENY
        );
        return decision.isPermit();

    }

    public boolean harVeilederSkriveTilgangTilFnr(String veilederId, Fnr fnr) {
        Decision decision = poaoTilgangClient.evaluatePolicy(new NavAnsattNavIdentSkrivetilgangTilEksternBrukerPolicyInput(
                veilederId, fnr.toString()
        )).getOrThrow();
        auditLogWithMessageAndDestinationUserId(
                "Veileder har gjort oppslag på fnr",
                fnr.get(),
                veilederId,
                decision.isPermit() ? AuthorizationDecision.PERMIT : AuthorizationDecision.DENY
        );
        return decision.isPermit();
    }

    public void sjekkLesetilgangMedFnr(Fnr fnr) {
        if (erEksternBruker()) {
            if (!harEksternBrukerTilgang(fnr)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Ekstern bruker har ikke tilgang på andre brukere enn seg selv");
            }
        } else {
            sjekkLesetilgangMedAktorId(getAktorIdOrThrow(fnr));
        }
    }

    // TODO fungerer ikke for eksternbrukere fordi abac ikke støtter tokenx tokens
    public void sjekkLesetilgangMedAktorId(AktorId aktorId) {
        sjekkTilgang(ActionId.READ, aktorId);

    }

    public void sjekkSkrivetilgangMedAktorId(AktorId aktorId) {
        sjekkTilgang(ActionId.WRITE, aktorId);
    }

    private void sjekkTilgang(ActionId actionId, AktorId aktorId) {
        Optional<String> sikkerhetsnivaa = hentSikkerhetsnivaa();
        if (erInternBruker()) {
            Decision decision = poaoTilgangClient.evaluatePolicy(new NavAnsattTilgangTilEksternBrukerPolicyInput(
                    hentInnloggetVeilederUUID(), mapActionTypeToTilgangsType(actionId), getFnrOrThrow(aktorId).get()
            )).getOrThrow();
            auditLogWithMessageAndDestinationUserId(
                    "Veileder har gjort oppslag på aktorid",
                    aktorId.get(),
                    authContextHolder.getNavIdent().orElse(NavIdent.of(UKJENT_NAV_IDENT)).get(),
                    decision.isPermit() ? AuthorizationDecision.PERMIT : AuthorizationDecision.DENY
            );
            if (decision.isDeny()) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }
        } else if (erEksternBruker() && sikkerhetsnivaa.isPresent() && sikkerhetsnivaa.get().equals("Level4")) {
            Decision decision = poaoTilgangClient.evaluatePolicy(new EksternBrukerTilgangTilEksternBrukerPolicyInput(
                    hentInnloggetPersonIdent(), getFnrOrThrow(aktorId).get()
            )).getOrThrow();
            auditLogWithMessageAndDestinationUserId(
                    "Ekstern bruker har gjort oppslag på aktorid",
                    aktorId.get(),
                    hentInnloggetPersonIdent(),
                    decision.isPermit() ? AuthorizationDecision.PERMIT : AuthorizationDecision.DENY
            );
            if (decision.isDeny()) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }

        } else if (erSystemBruker()) {
            if (!veilarbPep.harTilgangTilPerson(getInnloggetBrukerToken(), actionId, aktorId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }
        } else {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }


    public void sjekkTilgangTilEnhet(String enhetId) {
        if (!harTilgangTilEnhet(enhetId)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
    }

    public void sjekkTilgangTilPersonMedNiva3(AktorId aktorId) {
        String sikkerhetsnivaa = hentSikkerhetsnivaa().orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN));
        if (!getFnrOrThrow(aktorId).get().equals(hentInnloggetPersonIdent())) {
            log.warn("AktorId fnr mismatch  ");
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "AktorId fnr mismatch");
        }
        if (!(sikkerhetsnivaa.equals("Level4") || sikkerhetsnivaa.equals("Level3"))) {
            log.warn("Bruker må ha nivå 3 eller 4");
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bruker må ha nivå 3 eller 4");
        }
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

    public String getAadOboTokenForTjeneste(DownstreamApi api) {
        if (!erAadOboToken())
            throw new IllegalStateException("Kan ikke hente AAD-OBO token når innlogget bruker ikke er intern");
        String scope = "api://" + api.cluster + "." + api.namespace + "." + api.serviceName + "/.default";
        return aadOboTokenClient.exchangeOnBehalfOfToken(scope, getInnloggetBrukerToken());
    }

    public String getAadOboTokenForTjeneste(String tokenScope) {
        return aadOboTokenClient.exchangeOnBehalfOfToken(tokenScope, authContextHolder.requireIdTokenString());
    }

    public String getMachineTokenForTjeneste(DownstreamApi api) {
        String scope = "api://" + api.cluster + "." + api.namespace + "." + api.serviceName + "/.default";
        return machineToMachineTokenClient.createMachineToMachineToken(scope);
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
        return maybeClaims.map(claims -> hasIssuer(claims, "tokenx")).orElse(false);
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
            return authContextHolder.getSubject();
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

    private UUID hentInnloggetVeilederUUID() {
        return authContextHolder.getIdTokenClaims()
                .flatMap(claims -> getStringClaimOrEmpty(claims, "oid"))
                .map(UUID::fromString)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Fant ikke oid for innlogget veileder"));
    }

    public String hentInnloggetPersonIdent() {
        return authContextHolder.getIdTokenClaims()
                .flatMap(claims -> getStringClaimOrEmpty(claims, "pid"))
                .orElse(null);
    }

    private Optional<String> hentSikkerhetsnivaa() {
        return authContextHolder.getIdTokenClaims()
                .flatMap(claims -> getStringClaimOrEmpty(claims, "acr"));
    }

    private TilgangType mapActionTypeToTilgangsType(ActionId actionId) {
        if (actionId == ActionId.READ) {
            return TilgangType.LESE;
        } else if (actionId == ActionId.WRITE) {
            return TilgangType.SKRIVE;
        }
        throw new RuntimeException("Uventet actionId");
    }

    private void auditLogWithMessageAndDestinationUserId(String logMessage, String destinationUserId, String sourceUserID, AuthorizationDecision authorizationDecision) {
        if (AuthorizationDecision.PERMIT.equals(authorizationDecision)) {
            log.debug("tilgang til {}, erEksternBruker {}", logMessage, erEksternBruker());
        } else {
            log.info("ikke tilgang til {}, erEksternbruker {}", logMessage, erEksternBruker());
        }

        auditLogger.log(CefMessage.builder()
                .timeEnded(System.currentTimeMillis())
                .applicationName("veilarboppfolging")
                .sourceUserId(sourceUserID)
                .authorizationDecision(authorizationDecision)
                .event(CefMessageEvent.ACCESS)
                .severity(CefMessageSeverity.INFO)
                .name("veilarboppfolging-audit-log")
                .destinationUserId(destinationUserId)
                .extension("msg", logMessage)
                .build());
    }
}
