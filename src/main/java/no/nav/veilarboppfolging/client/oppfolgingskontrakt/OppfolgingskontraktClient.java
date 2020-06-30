package no.nav.veilarboppfolging.client.oppfolgingskontrakt;

import no.nav.common.health.HealthCheck;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.meldinger.HentOppfoelgingskontraktListeResponse;

import javax.xml.datatype.XMLGregorianCalendar;

public interface OppfolgingskontraktClient extends HealthCheck {

    HentOppfoelgingskontraktListeResponse hentOppfolgingskontraktListe(XMLGregorianCalendar fom, XMLGregorianCalendar tom, String fnr);

}
