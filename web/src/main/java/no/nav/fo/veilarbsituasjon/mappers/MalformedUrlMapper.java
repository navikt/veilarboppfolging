package no.nav.fo.veilarbsituasjon.mappers;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.net.MalformedURLException;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;

@Provider
public class MalformedUrlMapper implements ExceptionMapper<MalformedURLException> {
    @Override
    public Response toResponse(MalformedURLException exception) {
        return Response.status(BAD_REQUEST).entity("Feil format på callback-url").build();
    }
}
