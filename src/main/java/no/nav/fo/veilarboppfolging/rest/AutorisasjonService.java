package no.nav.fo.veilarboppfolging.rest;

import no.nav.apiapp.feil.IngenTilgang;
import no.nav.apiapp.security.PepClient;
import no.nav.apiapp.security.PepClientComparator;
import no.nav.apiapp.security.veilarbabac.Bruker;
import no.nav.brukerdialog.security.domain.IdentType;
import no.nav.brukerdialog.security.oidc.OidcTokenValidator;
import no.nav.brukerdialog.security.oidc.OidcTokenValidatorResult;
import no.nav.brukerdialog.security.oidc.provider.IssoOidcProvider;
import no.nav.common.auth.SubjectHandler;
import no.nav.dialogarena.aktor.AktorService;
import no.nav.sbl.dialogarena.common.abac.pep.AbacPersonId;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.servlet.http.HttpServletRequest;

@Component
public class AutorisasjonService {

    @Inject
    Provider<HttpServletRequest> httpServletRequestProvider;

    @Inject
    VeilArbAbacService veilArbAbacService;

    @Inject
    AktorService aktorService;

    @Inject
    PepClient pepClient;

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
            throw new IngenTilgang();
        }
    }

    public boolean harVeilederSkriveTilgangTilFnr(String veilederId, String fnr) {
        return veilArbAbacService.harVeilederSkriveTilgangTilFnr(veilederId, fnr);
    }

    public void sjekkLesetilgangTilBruker(String fnr) {
        Bruker bruker = Bruker.fraFnr(fnr)
                .medAktoerIdSupplier(() -> aktorService.getAktorId(fnr)
                        .orElseThrow(() -> new IllegalArgumentException("Fant ikke aktørid")));

        PepClientComparator.get(
                () -> pepClient.sjekkLesetilgang(AbacPersonId.fnr(fnr)),
                () -> pepClient.sjekkLesetilgang(AbacPersonId.aktorId(bruker.getAktoerId()))
        );
    }

}
