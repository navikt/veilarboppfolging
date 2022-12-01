package no.nav.veilarboppfolging.config;

import no.nav.common.auth.context.UserRole;
import no.nav.common.auth.oidc.filter.AzureAdUserRoleResolver;
import no.nav.common.auth.oidc.filter.OidcAuthenticationFilter;
import no.nav.common.auth.oidc.filter.OidcAuthenticatorConfig;
import no.nav.common.auth.utils.UserTokenFinder;
import no.nav.common.log.LogFilter;
import no.nav.common.rest.filter.ConsumerIdComplianceFilter;
import no.nav.common.rest.filter.SetStandardHttpHeadersFilter;
import no.nav.veilarboppfolging.utils.PingFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

import static no.nav.common.auth.Constants.*;
import static no.nav.common.auth.oidc.filter.OidcAuthenticator.fromConfigs;
import static no.nav.common.utils.EnvironmentUtils.isDevelopment;
import static no.nav.common.utils.EnvironmentUtils.requireApplicationName;
import static no.nav.veilarboppfolging.controller.AdminController.PTO_ADMIN_SERVICE_USER;

@Configuration
public class FilterConfig {

    private final List<String> ALLOWED_SERVICE_USERS = List.of(
            "srvveilarbportefolje", "srvveilarbdialog", "srvveilarbaktivitet",
            "srvveilarbjobbsoke", "srvveilarbdirigent", "srvveilarbregistre",
            "srvpam-cv-api", "srvveilarbvedtakss", PTO_ADMIN_SERVICE_USER
    );

    private OidcAuthenticatorConfig openAmStsAuthConfig(EnvironmentProperties properties) {
        return new OidcAuthenticatorConfig()
                .withDiscoveryUrl(properties.getOpenAmDiscoveryUrl())
                .withClientId(properties.getVeilarbloginOpenAmClientId())
                .withUserRole(UserRole.SYSTEM);
    }

    private OidcAuthenticatorConfig naisStsAuthConfig(EnvironmentProperties properties) {
        return new OidcAuthenticatorConfig()
                .withDiscoveryUrl(properties.getNaisStsDiscoveryUrl())
                .withClientIds(ALLOWED_SERVICE_USERS)
                .withUserRole(UserRole.SYSTEM);
    }

    private OidcAuthenticatorConfig tokenxAuthConfig(EnvironmentProperties properties) {
        return new OidcAuthenticatorConfig()
                .withDiscoveryUrl(properties.getTokenxDiscoveryUrl())
                .withClientId(properties.getTokenxClientId())
                .withUserRole(UserRole.EKSTERN);
    }

    private OidcAuthenticatorConfig openAmAuthConfig(EnvironmentProperties properties) {
        List<String> clientIds = List.of(
                properties.getVeilarbloginOpenAmClientId(),
                properties.getModialoginOpenAmClientId()
        );
        return new OidcAuthenticatorConfig()
                .withDiscoveryUrl(properties.getOpenAmDiscoveryUrl())
                .withClientIds(clientIds)
                .withIdTokenCookieName(OPEN_AM_ID_TOKEN_COOKIE_NAME)
                .withRefreshTokenCookieName(REFRESH_TOKEN_COOKIE_NAME)
                .withIdTokenFinder(new UserTokenFinder())
                .withRefreshUrl(properties.getOpenAmRefreshUrl())
                .withUserRole(UserRole.INTERN);
    }

    private OidcAuthenticatorConfig loginserviceIdportenConfig(EnvironmentProperties properties) {
        return new OidcAuthenticatorConfig()
                .withDiscoveryUrl(properties.getLoginserviceIdportenDiscoveryUrl())
                .withClientId(properties.getLoginserviceIdportenAudience())
                .withIdTokenCookieName(AZURE_AD_B2C_ID_TOKEN_COOKIE_NAME)
                .withUserRole(UserRole.EKSTERN);
    }

    private OidcAuthenticatorConfig naisAzureAdConfig(EnvironmentProperties properties) {
        return new OidcAuthenticatorConfig()
                .withDiscoveryUrl(properties.getNaisAadDiscoveryUrl())
                .withClientId(properties.getNaisAadClientId())
                .withUserRoleResolver(new AzureAdUserRoleResolver());
    }

    @Bean
    public FilterRegistrationBean<PingFilter> pingFilter() {
        // Veilarbproxy trenger dette endepunktet for å sjekke at tjenesten lever
        // /internal kan ikke brukes siden det blir stoppet før det kommer frem

        FilterRegistrationBean<PingFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new PingFilter());
        registration.setOrder(1);
        registration.addUrlPatterns("/api/ping");
        return registration;
    }

    @Bean
    public FilterRegistrationBean<OidcAuthenticationFilter> authenticationFilterRegistrationBean(EnvironmentProperties properties) {
        FilterRegistrationBean<OidcAuthenticationFilter> registration = new FilterRegistrationBean<>();
        OidcAuthenticationFilter authenticationFilter = new OidcAuthenticationFilter(
                fromConfigs(
                        openAmAuthConfig(properties),
                        loginserviceIdportenConfig(properties),
                        openAmStsAuthConfig(properties),
                        naisStsAuthConfig(properties),
                        naisAzureAdConfig(properties),
                        tokenxAuthConfig(properties)
                )
        );

        registration.setFilter(authenticationFilter);
        registration.setOrder(2);
        registration.addUrlPatterns("/api/*");
        return registration;
    }

    @Bean
    public FilterRegistrationBean<LogFilter> logFilterRegistrationBean() {
        FilterRegistrationBean<LogFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new LogFilter(requireApplicationName(), isDevelopment().orElse(false)));
        registration.setOrder(3);
        registration.addUrlPatterns("/*");
        return registration;
    }

    @Bean
    public FilterRegistrationBean<ConsumerIdComplianceFilter> consumerIdComplianceFilterRegistrationBean() {
        FilterRegistrationBean<ConsumerIdComplianceFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new ConsumerIdComplianceFilter(isDevelopment().orElse(false)));
        registration.setOrder(4);
        registration.addUrlPatterns("/api/*");
        return registration;
    }

    @Bean
    public FilterRegistrationBean<SetStandardHttpHeadersFilter> setStandardHeadersFilterRegistrationBean() {
        FilterRegistrationBean<SetStandardHttpHeadersFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new SetStandardHttpHeadersFilter());
        registration.setOrder(5);
        registration.addUrlPatterns("/*");
        return registration;
    }

    /*
    @Bean
    public FilterRegistrationBean<AuthInfoFilter> authInfoFilterRegistrationBean(MeterRegistry meterRegistry) {
        FilterRegistrationBean<AuthInfoFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new AuthInfoFilter(meterRegistry));
        registration.setOrder(6);
        registration.addUrlPatterns("/api/*");
        return registration;
    }*/
}
