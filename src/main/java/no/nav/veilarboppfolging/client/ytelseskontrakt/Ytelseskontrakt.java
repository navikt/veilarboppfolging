package no.nav.veilarboppfolging.client.ytelseskontrakt;


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

    public Ytelseskontrakt withDatoFra(XMLGregorianCalendar datoFra) {
        this.datoFra = new Dato(datoFra.getYear(), datoFra.getMonth(), datoFra.getDay());
        return this;
    }

    public Ytelseskontrakt withDatoTil(XMLGregorianCalendar datoTil) {
        this.datoTil = new Dato(datoTil.getYear(), datoTil.getMonth(), datoTil.getDay());
        return this;
    }

    public void setDatoFra(XMLGregorianCalendar datoFra) {
        this.datoFra = new Dato(datoFra.getYear(), datoFra.getMonth(), datoFra.getDay());
    }

    public void setDatoTil(XMLGregorianCalendar datoTil) {
        this.datoTil = new Dato(datoTil.getYear(), datoTil.getMonth(), datoTil.getDay());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Ytelseskontrakt that = (Ytelseskontrakt) o;

        if (!getStatus().equals(that.getStatus())) return false;
        if (!getYtelsestype().equals(that.getYtelsestype())) return false;
        if (!getDatoMottatt().equals(that.getDatoMottatt())) return false;
        if (getDatoFra() != null ? !getDatoFra().equals(that.getDatoFra()) : that.getDatoFra() != null) return false;
        return getDatoTil() != null ? getDatoTil().equals(that.getDatoTil()) : that.getDatoTil() == null;

    }

    @Override
    public int hashCode() {
        int result = getStatus() != null ? getStatus().hashCode() : 0;
        result = 31 * result + (getYtelsestype() != null ? getYtelsestype().hashCode() : 0);
        result = 31 * result + (getDatoMottatt() != null ? getDatoMottatt().hashCode() : 0);
        result = 31 * result + (getDatoFra() != null ? getDatoFra().hashCode() : 0);
        result = 31 * result + (getDatoTil() != null ? getDatoTil().hashCode() : 0);
        return result;
    }

}
