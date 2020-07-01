package no.nav.veilarboppfolging.client.ytelseskontrakt;

import no.nav.common.health.HealthCheck;

import javax.xml.datatype.XMLGregorianCalendar;

public interface YtelseskontraktClient extends HealthCheck {

    YtelseskontraktResponse hentYtelseskontraktListe(XMLGregorianCalendar periodeFom, XMLGregorianCalendar periodeTom, String personId);

}
