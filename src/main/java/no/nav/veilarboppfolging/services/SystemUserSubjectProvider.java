package no.nav.veilarboppfolging.services;

import no.nav.brukerdialog.security.domain.IdentType;
import no.nav.brukerdialog.security.oidc.SystemUserTokenProvider;
import no.nav.common.auth.SsoToken;
import no.nav.common.auth.Subject;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

import static no.nav.brukerdialog.tools.SecurityConstants.SYSTEMUSER_USERNAME;
import static no.nav.sbl.util.EnvironmentUtils.getRequiredProperty;
import static no.nav.sbl.util.EnvironmentUtils.resolveSrvUserPropertyName;

@Component
public class SystemUserSubjectProvider {

    private final String username = getRequiredProperty(resolveSrvUserPropertyName(), SYSTEMUSER_USERNAME);
    private final SystemUserTokenProvider systemUserTokenProvider;

    @Inject
    public SystemUserSubjectProvider(SystemUserTokenProvider systemUserTokenProvider) {
        this.systemUserTokenProvider = systemUserTokenProvider;
    }

    public Subject getSystemUserSubject() {
        return new Subject(username, IdentType.Systemressurs, SsoToken.oidcToken(systemUserTokenProvider.getToken()));
    }

}
