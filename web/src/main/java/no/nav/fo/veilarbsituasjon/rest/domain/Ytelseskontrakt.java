package no.nav.fo.veilarbsituasjon.rest.domain;


import javax.xml.datatype.XMLGregorianCalendar;

@SuppressWarnings("unused")
public class Ytelseskontrakt {

    private String status;

    private String ytelsestype;
    private Dato datoMottatt;
    private Dato datoFra;
    private Dato datoTil;

    public String getYtelsestype() {
        return ytelsestype;
    }

    public String getStatus() {
        return status;
    }

    public Dato getDatoMottatt() {
        return datoMottatt;
    }

    public Dato getDatoFra() {
        return datoFra;
    }

    public Dato getDatoTil() {
        return datoTil;
    }


    public Ytelseskontrakt withYtelsestype(String ytelsestype) {
        this.ytelsestype = ytelsestype;
        return this;
    }

    public Ytelseskontrakt withStatus(String status) {
        this.status = status;
        return this;
    }

    public Ytelseskontrakt withDatoMottatt(XMLGregorianCalendar datoKravMottatt) {
        datoMottatt = new Dato(datoKravMottatt.getYear(), datoKravMottatt.getMonth(), datoKravMottatt.getDay());
        return this;
    }

    public void setDatoFra(XMLGregorianCalendar datoFra) {
        this.datoFra = new Dato(datoFra.getYear(), datoFra.getMonth(), datoFra.getDay());
    }

    public void setDatoTil(XMLGregorianCalendar datoTil) {
        this.datoTil = new Dato(datoTil.getYear(), datoTil.getMonth(), datoTil.getDay());
    }
}
