package no.nav.veilarboppfolging.client.ytelseskontrakt;

import no.nav.common.health.HealthCheck;
import no.nav.common.types.identer.Fnr;

import javax.xml.datatype.XMLGregorianCalendar;

public interface YtelseskontraktClient extends HealthCheck {

    YtelseskontraktResponse hentYtelseskontraktListe(XMLGregorianCalendar periodeFom, XMLGregorianCalendar periodeTom, Fnr personId);

    YtelseskontraktResponse hentYtelseskontraktListe(Fnr personId);

}
