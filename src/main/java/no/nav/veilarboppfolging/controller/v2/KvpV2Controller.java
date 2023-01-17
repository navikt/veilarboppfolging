package no.nav.veilarboppfolging.controller.v2;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import no.nav.common.auth.context.AuthContextHolder;
import no.nav.common.types.identer.AktorId;
import no.nav.veilarboppfolging.controller.response.KvpDTO;
import no.nav.veilarboppfolging.service.AuthService;
import no.nav.veilarboppfolging.service.KvpService;
import no.nav.veilarboppfolging.utils.DtoMappers;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v2/kvp")
public class KvpV2Controller {

    private final List<String> allowedUsers = List.of("srvveilarbdialog", "srvveilarbaktivitet");
    private final List<String> allowedApps = List.of("veilarbdialog", "veilarbaktivitet");

    private final KvpService kvpService;

    private final AuthService authService;

    private final AuthContextHolder authContextHolder;

    @GetMapping
    @Operation(responses = {
            @ApiResponse(responseCode = "200", description = "Actor is currently in a KVP period.", content = @Content(schema = @Schema(implementation = KvpDTO.class))),
            @ApiResponse(responseCode = "204", description = "Actor is currently not in a KVP period."),
            @ApiResponse(responseCode = "403", description = "The API endpoint is requested by a user which is not in the allowed users list.")
    })
    public ResponseEntity<KvpDTO> getKvpStatus(@RequestParam("aktorId") AktorId aktorId) {
        // KVP information is only available to certain system users. We trust these users here,
        // so that we can avoid doing an ABAC query on each request.
        if (!isRequestAuthorized(aktorId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        return kvpService.hentGjeldendeKvpPeriode(aktorId)
                .map(periode -> ResponseEntity.ok(DtoMappers.kvpToDTO(periode)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NO_CONTENT).build());
    }

    private boolean isRequestAuthorized(AktorId aktorId) {
        String username = authContextHolder.getSubject().orElse("").toLowerCase();
        String appName = authService.hentApplikasjonFraContext();
        if (authService.erSystemBrukerFraAzureAd()) {
            return allowedApps.contains(appName);
        } else if (authService.erSystemBruker()) {
            return allowedUsers.contains(username);
        } else if (authService.erInternBruker()) {
            return allowedApps.contains(appName);
        } else if (authService.erEksternBruker()) {
            return allowedApps.contains(appName)
                && authService.harEksternBrukerTilgang(authService.getFnrOrThrow(aktorId));
        } else {
            return false;
        }
    }

}
