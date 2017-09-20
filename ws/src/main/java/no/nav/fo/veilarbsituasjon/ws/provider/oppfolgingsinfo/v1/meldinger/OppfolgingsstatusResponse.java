package no.nav.fo.veilarbsituasjon.ws.provider.oppfolgingsinfo.v1.meldinger;

import no.nav.fo.veilarbsituasjon.ws.provider.oppfolgingsinfo.v1.feil.WSSikkerhetsbegrensning;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.PROPERTY)
@XmlType(
        name = "OppfolgingsstatusResponse",
        propOrder = {"personident", "erUnderOppfolging", "veilederIdent", "wsSikkerhetsbegrensning"}
)
public class OppfolgingsstatusResponse {
    private String personident;
    private boolean erUnderOppfolging;
    private String veilederIdent;
    private WSSikkerhetsbegrensning wsSikkerhetsbegrensning;

    public String getPersonident() {
        return personident;
    }

    public void setPersonident(String personident) {
        this.personident = personident;
    }

    public boolean isErUnderOppfolging() {
        return erUnderOppfolging;
    }

    public void setErUnderOppfolging(boolean erUnderOppfolging) {
        this.erUnderOppfolging = erUnderOppfolging;
    }

    public String getVeilederIdent() {
        return veilederIdent;
    }

    public void setVeilederIdent(String veilederIdent) {
        this.veilederIdent = veilederIdent;
    }

    public WSSikkerhetsbegrensning getWsSikkerhetsbegrensning() {
        return wsSikkerhetsbegrensning;
    }

    public void setWsSikkerhetsbegrensning(WSSikkerhetsbegrensning wsSikkerhetsbegrensning) {
        this.wsSikkerhetsbegrensning = wsSikkerhetsbegrensning;
    }
}
