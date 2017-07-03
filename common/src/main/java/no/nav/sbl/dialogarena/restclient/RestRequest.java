package no.nav.sbl.dialogarena.restclient;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.NewCookie;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
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

    public <ELEMENT> List<ELEMENT> getList(Class<ELEMENT> responseClass) {
        Invocation.Builder request = webTarget.request();
        Arrays.stream(httpServletRequest.getCookies()).forEach(c -> request.cookie(mapCookie(c)));
        return request.get(new GenericType<>(new ListType(responseClass)));
    }

    /**
     * javax.ws.rs.core.Cookie lager en RFC 2109-cookie (versjon=1)
     * dette hånderer ikke applikasjonsstacken vår.
     * Løser dette ved å bruke NewCookie som gjør at cookies serialiseres på versjon 0-format
     */
    private Cookie mapCookie(javax.servlet.http.Cookie c) {
        return new NewCookie(c.getName(), c.getValue());
    }

    private class ListType implements ParameterizedType {
        private final Class<?> elementType;

        public ListType(Class<?> elementType) {
            this.elementType = elementType;
        }

        @Override
        public Type[] getActualTypeArguments() {
            return new Type[]{elementType};
        }

        @Override
        public Type getRawType() {
            return List.class;
        }

        @Override
        public Type getOwnerType() {
            return List.class;
        }
    }
}
