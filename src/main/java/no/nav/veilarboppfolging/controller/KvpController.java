package no.nav.veilarboppfolging.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import no.nav.common.auth.context.AuthContextHolder;
import no.nav.common.types.identer.AktorId;
import no.nav.veilarboppfolging.InternalServerError;
import no.nav.veilarboppfolging.UnauthorizedException;
import no.nav.veilarboppfolging.controller.response.KvpDTO;
import no.nav.veilarboppfolging.repository.KvpRepository;
import no.nav.veilarboppfolging.repository.entity.KvpPeriodeEntity;
import no.nav.veilarboppfolging.service.AuthService;
import no.nav.veilarboppfolging.utils.DtoMappers;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/kvp")
public class KvpController {

    private final List<String> allowedApps = List.of("veilarbdialog", "veilarbaktivitet");
    private final KvpRepository repository;
    private final AuthService authService;
    private final AuthContextHolder authContextHolder;


    @GetMapping("/{aktorId}/currentStatus")
    @Operation(
            summary = "Extract KVP status for an actor. This API endpoint is only available for system users",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Actor is currently in a KVP period.", content = @Content(schema = @Schema(implementation = KvpDTO.class))),
                    @ApiResponse(responseCode = "204", description = "Actor is currently not in a KVP period."),
                    @ApiResponse(responseCode = "403", description = "The API endpoint is requested by a user which is not in the allowed users list."),
                    @ApiResponse(responseCode = "500", description = "There is a server-side bug which should be fixed.")
            }
    )
    public ResponseEntity<KvpDTO> getKvpStatus(@PathVariable("aktorId") AktorId aktorId) {
        // KVP information is only available to certain system users. We trust these users here,
        // so that we can avoid doing an ABAC query on each request.
        authService.skalVereSystemBruker();
        authService.sjekkAtApplikasjonErIAllowList(allowedApps);

        // TODO: Do this inside a service

        long kvpId = repository.gjeldendeKvp(aktorId);
        if (kvpId == 0) {
            return ResponseEntity.status(204).build();
        }

        // This shouldn't happen, and signifies a bug in the dataset.
        // Throw a 500 error in order to make someone[tm] aware of the problem.
        Optional<KvpPeriodeEntity> maybeKvpPeriode = repository.hentKvpPeriode(kvpId);

        if (maybeKvpPeriode.isEmpty()) {
            throw new InternalServerError("Fant ikke kvp periode (burde ikke skje)");
        }

        return ResponseEntity.ok(DtoMappers.kvpToDTO(maybeKvpPeriode.get()));
    }
}
