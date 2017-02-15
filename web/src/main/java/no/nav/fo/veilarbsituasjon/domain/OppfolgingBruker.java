package no.nav.fo.veilarbsituasjon.domain;

import java.sql.Timestamp;

public class OppfolgingBruker {

    private String aktoerid;
    private String veileder;
    private Timestamp endretTimestamp;

    public OppfolgingBruker(){
        this.endretTimestamp = new Timestamp(System.currentTimeMillis());
    }

    public String getAktoerid() {
        return aktoerid;
    }

    public OppfolgingBruker withAktoerid(String aktoerid) {
        this.aktoerid = aktoerid;
        return this;
    }

    public String getVeileder() {
        return veileder;
    }

    public OppfolgingBruker withVeileder(String veileder) {
        this.veileder = veileder;
        return this;
    }

    public Timestamp getEndretTimestamp(){
        return endretTimestamp;
    }

    public String toString() {
        return "{\"aktoerid\":\""+aktoerid+"\",\"veileder\":\""+veileder+"\",\"oppdatert\":\""+endretTimestamp.toString()+"\"}";
    }
}
