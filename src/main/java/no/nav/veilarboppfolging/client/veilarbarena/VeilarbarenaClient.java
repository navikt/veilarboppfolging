package no.nav.veilarboppfolging.client.veilarbarena;

import no.nav.common.health.HealthCheck;
import no.nav.common.types.identer.Fnr;

import java.util.Optional;

public interface VeilarbarenaClient extends HealthCheck {

    Optional<VeilarbArenaOppfolging> hentOppfolgingsbruker(Fnr fnr);

    Optional<ArenaOppfolging> getArenaOppfolgingsstatus(Fnr fnr);

}
