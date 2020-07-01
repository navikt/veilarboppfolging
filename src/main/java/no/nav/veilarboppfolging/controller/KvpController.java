package no.nav.veilarboppfolging.controller;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import no.nav.common.auth.subject.SubjectHandler;
import no.nav.veilarboppfolging.db.KvpRepository;
import no.nav.veilarboppfolging.domain.Kvp;
import no.nav.veilarboppfolging.utils.mappers.DtoMappers;
import no.nav.veilarboppfolging.controller.domain.KvpDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static no.nav.common.utils.EnvironmentUtils.getRequiredProperty;
import static no.nav.veilarboppfolging.config.ApplicationConfig.KVP_API_BRUKERTILGANG_PROPERTY;

@RestController
@RequestMapping("/api/kvp")
public class KvpController {

    private final KvpRepository repository;

    @Autowired
    public KvpController(KvpRepository repository) {
        this.repository = repository;
    }

    @GET
    @Path("/{aktorId}/currentStatus")
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
    public KvpDTO getKvpStatus(@PathVariable("aktorId") String aktorId) {
        // KVP information is only available to certain system users. We trust these users here,
        // so that we can avoid doing an ABAC query on each request.
        if (!isRequestAuthorized()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        long kvpId = repository.gjeldendeKvp(aktorId);
        if (kvpId == 0) {
            return null;
        }

        // This shouldn't happen, and signifies a bug in the dataset.
        // Throw a 500 error in order to make someone[tm] aware of the problem.
        Kvp kvp = repository.fetch(kvpId);
        if (kvp == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return DtoMappers.kvpToDTO(kvp);
    }

    private boolean isRequestAuthorized() {
        String username = SubjectHandler.getIdent().orElse("").toLowerCase();
        String allowedUsersString = getRequiredProperty(KVP_API_BRUKERTILGANG_PROPERTY);
        List<String> allowedUsers = Arrays.stream(allowedUsersString.split(","))
                .map(String::trim)
                .collect(Collectors.toList());

        return allowedUsers.contains(username);
    }
}
