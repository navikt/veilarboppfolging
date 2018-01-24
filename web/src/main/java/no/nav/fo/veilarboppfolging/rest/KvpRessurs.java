package no.nav.fo.veilarboppfolging.rest;

import io.swagger.annotations.Api;
import no.nav.fo.veilarboppfolging.db.KvpRepository;
import no.nav.fo.veilarboppfolging.domain.Kvp;
import no.nav.fo.veilarboppfolging.mappers.KvpMapper;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("/kvp/{aktorId}")
@Component
@Produces(APPLICATION_JSON)
@Api(value = "KVP")
public class KvpRessurs {

    @Inject
    private KvpRepository repository;

    @GET
    @Path("/currentStatus")
    public Response getKvpStatus(@PathParam("aktorId") String aktorId) {
        long kvpId = repository.gjeldendeKvp(aktorId);
        if (kvpId == 0) {
            return null;
        }

        // This shouldn't happen, and signifies a bug in the dataset.
        // Throw a 500 error in order to make someone[tm] aware of the problem.
        Kvp kvp = repository.fetch(kvpId);
        if (kvp == null) {
            return Response.status(500).build();
        }

        return Response.ok(KvpMapper.KvpToDTO(kvp)).build();
    }
}
