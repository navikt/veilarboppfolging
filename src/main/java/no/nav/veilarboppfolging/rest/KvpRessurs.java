package no.nav.veilarboppfolging.rest;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import no.nav.brukerdialog.tools.Utils;
import no.nav.common.auth.SubjectHandler;
import no.nav.veilarboppfolging.db.KvpRepository;
import no.nav.veilarboppfolging.domain.Kvp;
import no.nav.veilarboppfolging.mappers.KvpMapper;
import no.nav.veilarboppfolging.rest.domain.KvpDTO;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import java.util.List;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static no.nav.veilarboppfolging.config.ApplicationConfig.KVP_API_BRUKERTILGANG_PROPERTY;
import static no.nav.sbl.util.EnvironmentUtils.getRequiredProperty;

@Path("/kvp/{aktorId}")
@Component
@Produces(APPLICATION_JSON)
@Api(value = "KVP")
public class KvpRessurs {

    @Inject
    private KvpRepository repository;

    @GET
    @Path("/currentStatus")
    @ApiOperation(
            value = "Extract KVP status for an actor",
            notes = "This API endpoint is only available for system users, set in the property '" + KVP_API_BRUKERTILGANG_PROPERTY + "'."
    )
    @ApiResponses({
            @ApiResponse(code = 200, message = "Actor is currently in a KVP period.", response = KvpDTO.class),
            @ApiResponse(code = 204, message = "Actor is currently not in a KVP period."),
            @ApiResponse(code = 403, message = "The API endpoint is requested by a user which is not in the allowed users list."),
            @ApiResponse(code = 500, message = "There is a server-side bug which should be fixed.")
    })
    public Response getKvpStatus(@PathParam("aktorId") String aktorId) {
        // KVP information is only available to certain system users. We trust these users here,
        // so that we can avoid doing an ABAC query on each request.
        if (!isRequestAuthorized()) {
            return Response.status(403).build();
        }

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

    private boolean isRequestAuthorized() {
        String username = SubjectHandler.getIdent().orElse("").toLowerCase();
        String allowedUsersString = getRequiredProperty(KVP_API_BRUKERTILGANG_PROPERTY);
        List<String> allowedUsers = Utils.getCommaSeparatedUsers(allowedUsersString);
        return allowedUsers.contains(username);
    }
}
