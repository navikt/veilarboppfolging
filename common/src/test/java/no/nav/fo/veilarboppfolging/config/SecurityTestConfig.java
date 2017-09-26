package no.nav.fo.veilarboppfolging.config;

import no.nav.dialogarena.config.fasit.FasitUtils;
import no.nav.dialogarena.config.fasit.LdapConfig;

public class SecurityTestConfig {

    public static void setupLdap() {
        LdapConfig ldapConfig = FasitUtils.getLdapConfig("ldap", "veilarboppfolging", "t4");
        System.setProperty("ldap.username", ldapConfig.username);
        System.setProperty("ldap.password", ldapConfig.username);
    }

}
