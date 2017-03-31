package no.nav.fo.veilarbsituasjon.config;

import no.nav.dialogarena.config.fasit.FasitUtils;
import no.nav.dialogarena.config.fasit.LdapConfig;
import no.nav.dialogarena.config.fasit.ServiceUser;

public class SecurityTestConfig {


    public static void setupSystemUser() {
        ServiceUser serviceUser = FasitUtils.getServiceUser("srvveilarbsituasjon", "veilarbsituasjon", "t4");
        System.setProperty("no.nav.abac.systemuser.username", serviceUser.username);
        System.setProperty("no.nav.abac.systemuser.password", serviceUser.password);

        System.setProperty("no.nav.modig.security.systemuser.username", serviceUser.username);
        System.setProperty("no.nav.modig.security.systemuser.password", serviceUser.password);
    }

    public static void setupLdap() {
        LdapConfig ldapConfig = FasitUtils.getLdapConfig("ldap", "veilarbsituasjon", "t4");
        System.setProperty("ldap.username", ldapConfig.username);
        System.setProperty("ldap.password", ldapConfig.username);
    }

}
