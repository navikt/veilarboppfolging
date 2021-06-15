package no.nav.veilarboppfolging.controller;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.RequiredArgsConstructor;
import no.nav.common.auth.context.AuthContextHolder;
import no.nav.veilarboppfolging.controller.response.KvpDTO;
import no.nav.veilarboppfolging.domain.Kvp;
import no.nav.veilarboppfolging.repository.KvpRepository;
import no.nav.veilarboppfolging.service.AuthService;
import no.nav.veilarboppfolging.utils.DtoMappers;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/kvp")
public class KvpController {

    private final List<String> allowedUsers = List.of("srvveilarbdialog", "srvveilarbaktivitet");

    private final KvpRepository repository;

    private final AuthService authService;

    private final AuthContextHolder authContextHolder;


    @GetMapping("/{aktorId}/currentStatus")
    @ApiOperation(
            value = "Extract KVP status for an actor",
            notes = "This API endpoint is only available for system users"
    )
    @ApiResponses({
            @ApiResponse(code = 200, message = "Actor is currently in a KVP period.", response = KvpDTO.class),
            @ApiResponse(code = 204, message = "Actor is currently not in a KVP period."),
            @ApiResponse(code = 403, message = "The API endpoint is requested by a user which is not in the allowed users list."),
            @ApiResponse(code = 500, message = "There is a server-side bug which should be fixed.")
    })
    public ResponseEntity<KvpDTO> getKvpStatus(@PathVariable("aktorId") String aktorId) {
        // KVP information is only available to certain system users. We trust these users here,
        // so that we can avoid doing an ABAC query on each request.
        if (!isRequestAuthorized()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        long kvpId = repository.gjeldendeKvp(aktorId);
        if (kvpId == 0) {
            return ResponseEntity.status(204).build();
        }

        // This shouldn't happen, and signifies a bug in the dataset.
        // Throw a 500 error in order to make someone[tm] aware of the problem.
        Kvp kvp = repository.fetch(kvpId);
        if (kvp == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return ResponseEntity.ok(DtoMappers.kvpToDTO(kvp));
    }

    private boolean isRequestAuthorized() {
        String username = authContextHolder.getSubject().orElse("").toLowerCase();
        return authService.erSystemBruker() && allowedUsers.contains(username);
    }
}
