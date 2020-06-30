package no.nav.veilarboppfolging.services;

import no.nav.apiapp.feil.IngenTilgang;
import no.nav.apiapp.security.PepClient;
import no.nav.brukerdialog.security.domain.IdentType;
import no.nav.brukerdialog.security.oidc.OidcTokenValidator;
import no.nav.brukerdialog.security.oidc.OidcTokenValidatorResult;
import no.nav.brukerdialog.security.oidc.provider.IssoOidcProvider;
import no.nav.common.auth.SubjectHandler;
import no.nav.dialogarena.aktor.AktorService;
import no.nav.veilarboppfolging.domain.AktorId;
import no.nav.sbl.dialogarena.common.abac.pep.AbacPersonId;
import no.nav.sbl.dialogarena.common.abac.pep.Pep;
import no.nav.sbl.dialogarena.common.abac.pep.RequestData;
import no.nav.sbl.dialogarena.common.abac.pep.domain.ResourceType;
import no.nav.sbl.dialogarena.common.abac.pep.domain.request.Action;
import no.nav.sbl.dialogarena.common.abac.pep.domain.response.BiasedDecisionResponse;
import no.nav.sbl.dialogarena.common.abac.pep.domain.response.Decision;
import no.nav.sbl.dialogarena.common.abac.pep.service.AbacServiceConfig;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.servlet.http.HttpServletRequest;

import static no.nav.veilarboppfolging.utils.FnrUtils.getAktorIdOrElseThrow;

@Component
public class AutorisasjonService {

    @Inject
    Provider<HttpServletRequest> httpServletRequestProvider;

    @Inject
    AktorService aktorService;

    @Inject
    PepClient pepClient;

    @Inject
    Pep pep;

    @Inject
    AbacServiceConfig abacServiceConfig;

    private OidcTokenValidator oidcTokenValidator = new OidcTokenValidator();
    private IssoOidcProvider issoProvider = new IssoOidcProvider();

    public void skalVereInternBruker() {
        skalVere(IdentType.InternBruker);
    }

    private void skalVere(IdentType forventetIdentType) {
        IdentType identType = SubjectHandler.getIdentType().orElse(null);
        if (identType != forventetIdentType) {
            throw new IngenTilgang(String.format("%s != %s", identType, forventetIdentType));
        }
    }

    public static boolean erInternBruker() {
        IdentType identType = SubjectHandler.getIdentType().orElse(null);
        return IdentType.InternBruker.equals(identType);
    }

    public static boolean erEksternBruker() {
        IdentType identType = SubjectHandler.getIdentType().orElse(null);
        return IdentType.EksternBruker.equals(identType);
    }

    public void skalVereSystemRessurs() {
        String systemToken = httpServletRequestProvider.get().getHeader("SystemAuthorization");
        OidcTokenValidatorResult validatedIssoToken = oidcTokenValidator.validate(systemToken, issoProvider);
        if (!validatedIssoToken.isValid()) {
            throw new IngenTilgang(validatedIssoToken.getErrorMessage());
        }
    }

    public boolean harVeilederSkriveTilgangTilFnr(String veilederId, String fnr) {
        RequestData requestData = new RequestData()
                .withAction(Action.ActionId.WRITE)
                .withDomain(PepConfig.DOMAIN_VEILARB)
                .withSubjectId(veilederId)
                .withPersonId(AbacPersonId.fnr(fnr))
                .withResourceType(ResourceType.Person)
                .withCredentialResource(abacServiceConfig.getUsername());

        BiasedDecisionResponse response = pep.harTilgang(requestData);

        return response.getBiasedDecision() == Decision.Permit;
    }

    public void sjekkLesetilgangTilBruker(String fnr) {
        AktorId aktorId = getAktorIdOrElseThrow(aktorService, fnr);
        pepClient.sjekkLesetilgangTilAktorId(aktorId.getAktorId());
    }

}
