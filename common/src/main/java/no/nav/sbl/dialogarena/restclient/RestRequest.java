package no.nav.sbl.dialogarena.restclient;

import org.glassfish.hk2.utilities.reflection.ParameterizedTypeImpl;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import java.util.Arrays;
import java.util.List;

public class RestRequest {

    private final HttpServletRequest httpServletRequest;
    private WebTarget webTarget;

    public RestRequest(HttpServletRequest httpServletRequest, WebTarget webTarget) {
        this.httpServletRequest = httpServletRequest;
        this.webTarget = webTarget;
    }

    public RestRequest queryParam(String name, Object value) {
        webTarget = webTarget.queryParam(name, value);
        return this;
    }

    public <T> List<T> getList(Class<T> responseClass) {
        Invocation.Builder request = webTarget.request();
        Arrays.stream(httpServletRequest.getCookies()).forEach(c -> request.cookie(c.getName(), c.getValue()));
        return request.get(new GenericType<>(new ParameterizedTypeImpl(List.class, responseClass)));
    }

}
