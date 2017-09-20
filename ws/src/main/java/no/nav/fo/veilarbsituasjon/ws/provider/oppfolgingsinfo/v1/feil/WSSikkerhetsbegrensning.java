package no.nav.fo.veilarbsituasjon.ws.provider.oppfolgingsinfo.v1.feil;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.PROPERTY)
@XmlType(
        name = "WSSikkerhetsbegrensning",
        propOrder = {"feilkilde", "feilmelding"}
)
public class WSSikkerhetsbegrensning {
    private String feilkilde;
    private String feilmelding;

    public String getFeilkilde() {
        return feilkilde;
    }

    public void setFeilkilde(String feilkilde) {
        this.feilkilde = feilkilde;
    }

    public String getFeilmelding() {
        return feilmelding;
    }

    public void setFeilmelding(String feilmelding) {
        this.feilmelding = feilmelding;
    }
}
