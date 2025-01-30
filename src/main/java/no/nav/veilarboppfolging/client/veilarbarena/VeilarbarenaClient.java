package no.nav.veilarboppfolging.client.veilarbarena;

import no.nav.common.health.HealthCheck;
import no.nav.common.types.identer.Fnr;

import java.util.Optional;

public interface VeilarbarenaClient extends HealthCheck {

    Optional<VeilarbArenaOppfolgingsBruker> hentOppfolgingsbruker(Fnr fnr);

    Optional<VeilarbArenaOppfolgingsStatus> getArenaOppfolgingsstatus(Fnr fnr);

    Optional<YtelserDTO> getArenaYtelser(Fnr fnr);

    Optional<RegistrerIkkeArbeidsokerRespons> registrerIkkeArbeidsoker(Fnr fnr);
}
