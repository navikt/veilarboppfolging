package no.nav.sbl.dialogarena.restclient;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

import static javax.ws.rs.core.HttpHeaders.COOKIE;

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

//        Arrays.stream(httpServletRequest.getCookies()).forEach(c -> {
// request.cookie(c.getName(), c.getValue());
//        });

        request.header(COOKIE,httpServletRequest.getHeader(COOKIE));
        return request.get(new GenericType<>(new ListType(responseClass)));
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
