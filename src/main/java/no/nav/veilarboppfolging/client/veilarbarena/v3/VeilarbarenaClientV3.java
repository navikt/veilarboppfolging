package no.nav.veilarboppfolging.client.veilarbarena.v3;

import no.nav.common.health.HealthCheck;
import no.nav.common.types.identer.Fnr;
import no.nav.veilarboppfolging.client.veilarbarena.ArenaOppfolging;
import no.nav.veilarboppfolging.client.veilarbarena.VeilarbArenaOppfolging;
import no.nav.veilarboppfolging.domain.PersonRequest;

import java.util.Optional;

public interface VeilarbarenaClientV3 extends HealthCheck {

    Optional<VeilarbArenaOppfolging> hentOppfolgingsbrukerV3(PersonRequest personRequest);

    Optional<ArenaOppfolging> getArenaOppfolgingsstatusV3(PersonRequest personRequest);
}
