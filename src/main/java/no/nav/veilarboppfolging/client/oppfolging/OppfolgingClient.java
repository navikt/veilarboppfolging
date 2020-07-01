package no.nav.veilarboppfolging.client.oppfolging;

import no.nav.common.health.HealthCheck;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.meldinger.HentOppfoelgingskontraktListeResponse;

import javax.xml.datatype.XMLGregorianCalendar;

public interface OppfolgingClient extends HealthCheck {

    HentOppfoelgingskontraktListeResponse hentOppfolgingskontraktListe(XMLGregorianCalendar fom, XMLGregorianCalendar tom, String fnr);

    String finnEnhetId(String fnr);

}
