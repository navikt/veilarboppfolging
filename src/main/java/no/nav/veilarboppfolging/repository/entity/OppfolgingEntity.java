package no.nav.veilarboppfolging.repository.entity;

import lombok.Data;
import lombok.experimental.Accessors;
import no.nav.veilarboppfolging.oppfolgingsbruker.arena.LocalArenaOppfolging;

import java.time.ZonedDateTime;
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
    String aktorId;
    String veilederId;
    boolean underOppfolging;
    Long gjeldendeManuellStatusId;
    long gjeldendeMaalId;
    long gjeldendeKvpId;
    Optional<LocalArenaOppfolging> localArenaOppfolging;
    ZonedDateTime oppdatert;
}
