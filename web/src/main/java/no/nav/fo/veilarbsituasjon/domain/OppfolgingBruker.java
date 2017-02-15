package no.nav.fo.veilarbsituasjon.domain;

import java.sql.Timestamp;

public class OppfolgingBruker {

    private String aktoerid;
    private String veileder;
    private Timestamp endret_timestamp;

    public OppfolgingBruker(){
        this.endret_timestamp = new Timestamp(System.currentTimeMillis());
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

    public Timestamp getEndret_date(){
        return endret_timestamp;
    }

    public String toString() {
        return "{\"aktoerid\":\""+aktoerid+"\",\"veileder\":\""+veileder+"\",\"oppdatert\":\""+endret_timestamp.toString()+"\"}";
    }
}
