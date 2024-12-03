package no.nav.veilarboppfolging.repository.entity;

import lombok.Data;
import lombok.experimental.Accessors;
import no.nav.veilarboppfolging.oppfolgingsbruker.arena.LocalArenaOppfolging;

import java.util.Optional;

/**
 * The OppfolgingTable class is used as a transient data carrier from the
 * database layer (OppfolgingsStatusRepository) to the service layer.
 * Please avoid downstream use of this class. Instead, try to use
 * {@link no.nav.veilarboppfolging.domain.Oppfolging} and related methods.
 */
@Data
@Accessors(chain = true)
public class OppfolgingEntity {
    private String aktorId;
    private String veilederId;
    private boolean underOppfolging;
    private Long gjeldendeManuellStatusId;
    private long gjeldendeMaalId;
    private long gjeldendeKvpId;
    private Optional<LocalArenaOppfolging> localArenaOppfolging;
}
