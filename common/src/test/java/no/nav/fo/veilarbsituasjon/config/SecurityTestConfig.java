package no.nav.fo.veilarbsituasjon.config;

import no.nav.dialogarena.config.fasit.FasitUtils;
import no.nav.dialogarena.config.fasit.LdapConfig;
import no.nav.dialogarena.config.fasit.ServiceUser;

public class SecurityTestConfig {

    public static void setupLdap() {
        LdapConfig ldapConfig = FasitUtils.getLdapConfig("ldap", "veilarbsituasjon", "t4");
        System.setProperty("ldap.username", ldapConfig.username);
        System.setProperty("ldap.password", ldapConfig.username);
    }

}
