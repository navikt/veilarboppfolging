package no.nav.fo.veilarboppfolging.security;

import lombok.Builder;
import lombok.Value;

@Builder
@Value
public class SecurityTokenServiceOidcProviderConfig {
    public final String discoveryUrl;

}
