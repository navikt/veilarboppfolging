package no.nav.veilarboppfolging.utils;


import no.nav.veilarboppfolging.services.OppfolgingService;
import no.nav.json.JsonProvider;
import org.glassfish.jersey.client.ClientConfig;
import org.slf4j.Logger;

import javax.inject.Provider;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;

import static no.nav.veilarboppfolging.utils.StringUtils.notNullAndNotEmpty;
import static org.glassfish.jersey.client.ClientProperties.*;
import static org.slf4j.LoggerFactory.getLogger;

public class RestClient {

    private static final Logger LOG = getLogger(OppfolgingService.class);

    private final Provider<HttpServletRequest> httpServletRequestProvider;
    private final String basePath;

    public RestClient(Provider<HttpServletRequest> httpServletRequestProvider, String basePath) {
        this.httpServletRequestProvider = httpServletRequestProvider;
        this.basePath = basePath;
    }

    public RestRequest request(String relativePath) {
        Client client = createClient();
        HttpServletRequest httpServletRequest = httpServletRequestProvider.get();
        WebTarget webTarget = client.target(basePath + relativePath);
        return new RestRequest(httpServletRequest, webTarget);
    }

    private Client createClient() {
        ClientConfig clientConfig = new ClientConfig();
        clientConfig.register(new JsonProvider());
        clientConfig.property(FOLLOW_REDIRECTS,false);
        clientConfig.property(CONNECT_TIMEOUT,5000);
        clientConfig.property(READ_TIMEOUT,15000);
        return ClientBuilder.newClient(clientConfig);
    }

    public static RestClient build(Provider<HttpServletRequest> httpServletRequestProvider, String basePath) {
        if (!notNullAndNotEmpty(basePath)) {
            throw new IllegalArgumentException("mangler basePath");
        }
        if (httpServletRequestProvider == null) {
            throw new IllegalArgumentException("mangler httpServletRequestProvider");
        }

        LOG.info("building rest client for [{}]", basePath);
        return new RestClient(httpServletRequestProvider, basePath);
    }

}
