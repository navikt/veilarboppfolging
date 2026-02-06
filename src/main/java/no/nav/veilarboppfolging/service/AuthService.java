package no.nav.veilarboppfolging.service;

import com.nimbusds.jwt.JWTClaimsSet;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
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
import no.nav.common.types.identer.*;
import no.nav.poao_tilgang.client.*;
import no.nav.veilarboppfolging.BadRequestException;
import no.nav.veilarboppfolging.ForbiddenException;
import no.nav.veilarboppfolging.UnauthorizedException;
import no.nav.veilarboppfolging.config.EnvironmentProperties;
import no.nav.veilarboppfolging.tokenClient.ErrorMappedAzureAdMachineToMachineTokenClient;
import no.nav.veilarboppfolging.tokenClient.ErrorMappedAzureAdOnBehalfOfTokenClient;
import no.nav.veilarboppfolging.utils.DownstreamApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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

@Slf4j
@Service
public class AuthService {

    public static final String UKJENT_NAV_IDENT = "UKJENT";
    private final AuthContextHolder authContextHolder;
    private final AuditLogger auditLogger;

    private final ErrorMappedAzureAdOnBehalfOfTokenClient aadOboTokenClient;

    private final ErrorMappedAzureAdMachineToMachineTokenClient machineToMachineTokenClient;

    private final AktorOppslagClient aktorOppslagClient;

    private final EnvironmentProperties environmentProperties;

    private final PoaoTilgangClient poaoTilgangClient;

    @Autowired
    public AuthService(AuthContextHolder authContextHolder, AktorOppslagClient aktorOppslagClient, ErrorMappedAzureAdOnBehalfOfTokenClient aadOboTokenClient, ErrorMappedAzureAdMachineToMachineTokenClient machineToMachineTokenClient, EnvironmentProperties environmentProperties, AuditLogger auditLogger, PoaoTilgangClient poaoTilgangClient) {
        this.authContextHolder = authContextHolder;
        this.aktorOppslagClient = aktorOppslagClient;
        this.aadOboTokenClient = aadOboTokenClient;
        this.machineToMachineTokenClient = machineToMachineTokenClient;
        this.environmentProperties = environmentProperties;
        this.auditLogger = auditLogger;
        this.poaoTilgangClient = poaoTilgangClient;
    }

    public void skalVereInternBruker() {
        if (!authContextHolder.erInternBruker()) {
            throw new ForbiddenException("Bruker er ikke en intern bruker");
        }
    }

    public void skalVereInternEllerSystemBruker() {
        if (!authContextHolder.erInternBruker() && !authContextHolder.erSystemBruker()) {
            throw new ForbiddenException("Bruker er verken en intern eller system bruker");
        }
    }

    // TODO bruk poao-tilgang - der vil vi også implementere representasjon etter hvert.
    public boolean harEksternBrukerTilgang(Fnr fnr) {
        // Når man ikke bruker Pep så må man gjøre auditlogging selv
        var subjectUser = getInnloggetBrukerIdent();
        boolean sammeFnr = subjectUser.equals(fnr.get());
        boolean erNivaa4 = hentSikkerhetsnivaa()
                .orElse(null) == SikkerthetsNivå.Nivå4;

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
            throw new ForbiddenException("Bruker er ikke en systembruker");
        }
    }

    public void skalVereSystemBrukerFraAzureAd() {
        if (!erSystemBrukerFraAzureAd()) {
            throw new ForbiddenException("Bruker er ikke en systembruker fra azureAd");
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

    public boolean erEksternBruker() {
        return authContextHolder.erEksternBruker();
    }

    public Optional<UserRole> getRole() {
        return authContextHolder.getRole();
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
        } else if (erEksternBruker()) {
            return false;
        } else {
            // Systembruker
            return true;
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
                throw new ForbiddenException("Ekstern bruker har ikke tilgang på andre brukere enn seg selv");
            }
        } else {
            sjekkLesetilgangMedAktorId(getAktorIdOrThrow(fnr));
        }
    }

    public void sjekkLesetilgangMedAktorId(AktorId aktorId) {
        sjekkTilgang(TilgangType.LESE, aktorId);

    }

    public void sjekkSkriveTilgangMedFnr(Fnr fnr) {
        if (erEksternBruker()) {
            if (!harEksternBrukerTilgang(fnr)) {
                throw new ForbiddenException("Ekstern bruker har ikke tilgang på andre brukere enn seg selv");
            }
        } else {
            sjekkSkrivetilgangMedAktorId(getAktorIdOrThrow(fnr));
        }
    }

    public void sjekkSkrivetilgangMedAktorId(AktorId aktorId) {
        sjekkTilgang(TilgangType.SKRIVE, aktorId);
    }

    public void sjekkTilgangTilEnhet(String enhetId) {

        if (!harTilgangTilEnhet(enhetId)) {
            throw new ForbiddenException("Har ikke tilgang til enhet");
        }
    }

    public void sjekkTilgangTilPersonMedNiva3(AktorId aktorId) {
        SikkerthetsNivå sikkerhetsnivaa = hentSikkerhetsnivaa().orElseThrow(() -> new UnauthorizedException("Fant ikke sikkerhetsnivå i token"));
        if (!getFnrOrThrow(aktorId).get().equals(hentInnloggetPersonIdent())) {
            log.warn("AktorId fnr mismatch  ");
            throw new ForbiddenException("AktorId fnr mismatch");
        }
        if (sikkerhetsnivaa == null) {
            log.warn("Bruker må ha nivå 3 eller 4");
            throw new ForbiddenException("Bruker må ha nivå 3 eller 4");
        }
    }

    // TODO: Det er hårete å måtte skille på ekstern og intern
    //  En alternativ løsning er å ha egne endepunkter for de forskjellige rollene
    public Fnr hentIdentForEksternEllerIntern(Fnr queryParamFnr) {
        if (erSystemBruker()) {
            throw new ForbiddenException("Må være enten ekstern eller intern bruker (ikke system)");
        }

        return hentIdentFraQueryParamEllerToken(queryParamFnr);
    }

    public Fnr hentIdentFraQueryParamEllerToken(Fnr queryParamFnr) {
        Fnr fnr;

        switch (authContextHolder.requireRole()) {
            case EKSTERN:
                fnr = Fnr.of(getInnloggetBrukerIdent());
                break;
            case INTERN, SYSTEM:
                fnr = queryParamFnr;
                break;
            default:
                throw new ForbiddenException("Må være intern eller ekstern eller system");
        }

        if (fnr == null) {
            throw new BadRequestException("Fnr må enten være i queryparam eller i token");
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
        return authContextHolder.getIdTokenString().orElseThrow(() -> new UnauthorizedException("Fant ikke token for innlogget bruker"));
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

    public String getMachineTokenForTjeneste(String scope) {
        return machineToMachineTokenClient.createMachineToMachineToken(scope);
    }

    // NAV ident, fnr eller annen ID
    public String getInnloggetBrukerIdent() {
        return authContextHolder.getUid()
                .orElseThrow(() -> new UnauthorizedException("User ident is missing"));
    }

    public String getInnloggetVeilederIdent() {
        if (!erInternBruker()) {
            throw new UnauthorizedException("Må være intern bruker");
        }
        return getInnloggetBrukerIdent();
    }

    public void sjekkAtApplikasjonErIAllowList(String[] allowlist) {
        sjekkAtApplikasjonErIAllowList(List.of(allowlist));
    }

    @SneakyThrows
    public void sjekkAtApplikasjonErIAllowList(List<String> allowlist) {
        String appname = hentApplikasjonFraContext();
        if (allowlist.contains(appname) || appname.equals("veilarboppfolging")) {
            return;
        }
        log.error("Applikasjon {} er ikke allowlist", appname);
        throw new ForbiddenException("Applikasjon " + appname + " er ikke allowlist");
    }

    public static boolean isAzure(Optional<JWTClaimsSet> maybeClaims) {
        return maybeClaims.map(claims -> hasIssuer(claims, "microsoftonline.com")).orElse(false);
    }

    public static boolean isTokenX(Optional<JWTClaimsSet> maybeClaims) {
        return maybeClaims.map(claims -> hasIssuer(claims, "tokenx")).orElse(false);
    }

    public String hentApplikasjonFraContext() {
        return getFullAppName()
                .map(claim -> claim.split(":"))
                .filter(claims -> claims.length == 3)
                .map(claims -> claims[2])
                .orElse("");
    }

    public String hentInnloggetPersonIdent() {
        return authContextHolder.getIdTokenClaims()
                .flatMap(claims -> getStringClaimOrEmpty(claims, "pid"))
                .orElse(null);
    }

    public void authorizeRequest(EksternBrukerId ident, List<String> allowList) {
        var idToken = getInnloggetBrukerToken();

        if (idToken == null || idToken.isEmpty()) {
            throw new UnauthorizedException("idToken is missing");
        }

        if (ident instanceof Fnr fnr) {
            authorizeFnr(fnr, allowList);
        }

        if (ident instanceof AktorId aktorId) {
            authorizeAktorId(aktorId, allowList);
        }
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

    /* Brukes når man trenger å vite hvorfor veileder fikk Deny */
    public Decision evaluerNavAnsattTilagngTilBruker(Fnr fnr, TilgangType tilgangType) {
        if(!erInternBruker()) {
            throw new ForbiddenException("Må være intern bruker");
        }
        return poaoTilgangClient.evaluatePolicy(new NavAnsattTilgangTilEksternBrukerPolicyInput(
                hentInnloggetVeilederUUID(), tilgangType, fnr.get()
        )).getOrThrow();
    }

    private void sjekkTilgang(TilgangType tilgangType, AktorId aktorId) {
        Optional<SikkerthetsNivå> sikkerhetsnivaa = hentSikkerhetsnivaa();
        if (erInternBruker()) {
            Decision decision = poaoTilgangClient.evaluatePolicy(new NavAnsattTilgangTilEksternBrukerPolicyInput(
                    hentInnloggetVeilederUUID(), tilgangType, getFnrOrThrow(aktorId).get()
            )).getOrThrow();
            auditLogWithMessageAndDestinationUserId(
                    "Veileder har gjort oppslag på aktorid",
                    aktorId.get(),
                    authContextHolder.getNavIdent().orElse(NavIdent.of(UKJENT_NAV_IDENT)).get(),
                    decision.isPermit() ? AuthorizationDecision.PERMIT : AuthorizationDecision.DENY
            );
            if (decision.isDeny()) {
                throw new ForbiddenException("NavAnsattTilgangTilEksternBrukerPolicyInput fikk deny");
            }
        } else if (erEksternBruker() && sikkerhetsnivaa.isPresent() && sikkerhetsnivaa.get() == SikkerthetsNivå.Nivå4) {
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
                throw new ForbiddenException("EksternBrukerTilgangTilEksternBrukerPolicyInput fikk deny");
            }
        }
    }

    private boolean erAadOboToken() {
        Optional<String> navIdentClaim = authContextHolder.getIdTokenClaims()
                .flatMap(claims -> authContextHolder.getStringClaim(claims, "NAVident"));
        return authContextHolder.getIdTokenClaims().map(JWTClaimsSet::getIssuer).filter(environmentProperties.getNaisAadIssuer()::equals).isPresent()
                && authContextHolder.getIdTokenClaims().map(x -> x.getClaim("oid")).isPresent()
                && navIdentClaim.isPresent();
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
                .orElseThrow(() -> new ForbiddenException("Fant ikke oid for innlogget veileder"));
    }


    enum SikkerthetsNivå {
        Nivå4,
        Nivå3
    }
    private Optional<SikkerthetsNivå> hentSikkerhetsnivaa() {
        return authContextHolder.getIdTokenClaims()
                .flatMap(claims -> getStringClaimOrEmpty(claims, "acr"))
                .map(claimValue -> {
                    if (claimValue.equals("Level3") || claimValue.equals("idporten-loa-substantial")) return SikkerthetsNivå.Nivå3;
                    if (claimValue.equals("Level4") || claimValue.equals("idporten-loa-high")) return SikkerthetsNivå.Nivå4;
                    log.warn("Unknown arc claim value, could not determine sikkerhetsnivå: {}", claimValue);
                    return null;
                });
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

    private void authorizeFnr(Fnr fnr, List<String> allowlist) {
        if (erSystemBrukerFraAzureAd()) {
            sjekkAtApplikasjonErIAllowList(allowlist);
        } else {
            sjekkLesetilgangMedFnr(fnr);
        }
    }

    private void authorizeAktorId(AktorId aktorId, List<String> allowlist) {
        if (erInternBruker()) {
            sjekkLesetilgangMedAktorId(aktorId);
        } else if (erSystemBrukerFraAzureAd()) {
            sjekkAtApplikasjonErIAllowList(allowlist);
        } else if (erEksternBruker()) {
            throw new ForbiddenException("Eksternbruker ikke tillatt");
        }
    }
}
