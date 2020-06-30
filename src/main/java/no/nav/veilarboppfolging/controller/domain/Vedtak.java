package no.nav.veilarboppfolging.controller.domain;

import javax.xml.datatype.XMLGregorianCalendar;

@SuppressWarnings("unused")
public class Vedtak {
    private String vedtakstype;
    private String status;
    private String aktivitetsfase;
    private String rettighetsgruppe;
    private Dato fradato;
    private Dato tildato;

    public String getVedtakstype() {
        return vedtakstype;
    }

    public String getStatus() {
        return status;
    }

    public String getAktivitetsfase() {
        return aktivitetsfase;
    }

    public String getRettighetsgruppe() {
        return rettighetsgruppe;
    }

    public Dato getFomDato() { return fradato; }

    public Dato getTomdato() { return tildato; }

    public Vedtak withVedtakstype(String vedtakstype) {
        this.vedtakstype = vedtakstype;
        return this;
    }

    public Vedtak withStatus(String status) {
        this.status = status;
        return this;
    }

    public Vedtak withAktivitetsfase(String aktivitetsfase) {
        this.aktivitetsfase = aktivitetsfase;
        return this;
    }

    public Vedtak withRettighetsgruppe(String rettighetsGruppe) {
        this.rettighetsgruppe = rettighetsGruppe;
        return this;
    }

    public Vedtak withFradato(XMLGregorianCalendar fomdato) {
        fradato = new Dato(fomdato.getYear(), fomdato.getMonth(), fomdato.getDay());
        return this;
    }

    public Vedtak withTildato(XMLGregorianCalendar tomdato) {
        tildato = new Dato(tomdato.getYear(), tomdato.getMonth(), tomdato.getDay());
        return this;
    }

    public void setRettighetsgruppe(String rettighetsGruppe) {
        this.rettighetsgruppe = rettighetsGruppe;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Vedtak vedtak = (Vedtak) o;

        if (getVedtakstype() != null ? !getVedtakstype().equals(vedtak.getVedtakstype()) : vedtak.getVedtakstype() != null)
            return false;
        if (!getStatus().equals(vedtak.getStatus())) return false;
        if (getAktivitetsfase() != null ? !getAktivitetsfase().equals(vedtak.getAktivitetsfase()) : vedtak.getAktivitetsfase() != null)
            return false;
        return getRettighetsgruppe() != null ? getRettighetsgruppe().equals(vedtak.getRettighetsgruppe()) : vedtak.getRettighetsgruppe() == null;

    }

    @Override
    public int hashCode() {
        int result = getVedtakstype() != null ? getVedtakstype().hashCode() : 0;
        result = 31 * result + (getStatus() != null ? getStatus().hashCode() : 0);
        result = 31 * result + (getAktivitetsfase() != null ? getAktivitetsfase().hashCode() : 0);
        result = 31 * result + (getRettighetsgruppe() != null ? getRettighetsgruppe().hashCode() : 0);
        return result;
    }

}