package no.nav.veilarboppfolging.client.veilarbarena;

import no.nav.common.health.HealthCheck;

import java.util.Optional;

public interface VeilarbarenaClient extends HealthCheck {

    Optional<VeilarbArenaOppfolging> hentOppfolgingsbruker(String fnr);

    Optional<ArenaOppfolging> getArenaOppfolgingsstatus(String fnr);

}
