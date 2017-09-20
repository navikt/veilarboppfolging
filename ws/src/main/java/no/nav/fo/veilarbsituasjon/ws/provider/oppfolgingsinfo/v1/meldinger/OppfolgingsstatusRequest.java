package no.nav.fo.veilarbsituasjon.ws.provider.oppfolgingsinfo.v1.meldinger;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.PROPERTY)
@XmlType(
        name = "OppfolgingsstatusRequest",
        propOrder = {"personident"}
)
public class OppfolgingsstatusRequest {
    private String personident;

    @XmlElement(required = true)
    public String getPersonident() {
        return personident;
    }

    public void setPersonident(String personident) {
        this.personident = personident;
    }
}
