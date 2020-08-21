package no.nav.veilarboppfolging.client.veilarbaktivitet;

import no.nav.common.health.HealthCheck;

import java.util.List;

public interface VeilarbaktivitetClient extends HealthCheck {

    List<ArenaAktivitetDTO> hentArenaAktiviteter(String fnr);

}
