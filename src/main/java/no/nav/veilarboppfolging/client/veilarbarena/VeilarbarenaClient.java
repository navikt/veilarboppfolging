package no.nav.veilarboppfolging.client.veilarbarena;

import no.nav.common.health.HealthCheck;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.meldinger.HentOppfoelgingskontraktListeResponse;
import no.nav.veilarboppfolging.client.veilarbaktivitet.ArenaAktivitetDTO;
import no.nav.veilarboppfolging.domain.ArenaOppfolging;

import javax.xml.datatype.XMLGregorianCalendar;
import java.util.List;
import java.util.Optional;

public interface VeilarbarenaClient extends HealthCheck {

    Optional<VeilarbArenaOppfolging> hentOppfolgingsbruker(String fnr);

    ArenaOppfolging getArenaOppfolgingsstatus(String fnr);

}
