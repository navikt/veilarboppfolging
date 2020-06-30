package no.nav.veilarboppfolging.client.ytelseskontrakt;

import no.nav.common.health.HealthCheck;
import no.nav.tjeneste.virksomhet.ytelseskontrakt.v3.meldinger.WSHentYtelseskontraktListeResponse;
import no.nav.veilarboppfolging.client.veilarbarena.VeilarbArenaOppfolging;

import javax.xml.datatype.XMLGregorianCalendar;
import java.util.Optional;

public interface YtelseskontraktClient extends HealthCheck {

    WSHentYtelseskontraktListeResponse hentYtelseskontraktListe(XMLGregorianCalendar periodeFom, XMLGregorianCalendar periodeTom, String personId);

}
