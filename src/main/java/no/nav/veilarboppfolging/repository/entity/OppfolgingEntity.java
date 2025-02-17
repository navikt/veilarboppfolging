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
    public String aktorId;
    public String veilederId;
    public boolean underOppfolging;
    public Long gjeldendeManuellStatusId;
    public long gjeldendeMaalId;
    public long gjeldendeKvpId;
    public Optional<LocalArenaOppfolging> localArenaOppfolging;
}
