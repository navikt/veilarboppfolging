package no.nav.veilarboppfolging.client.ytelseskontrakt;


import lombok.AllArgsConstructor;
import lombok.Data;

import javax.xml.datatype.XMLGregorianCalendar;
import java.time.LocalDate;

@Data
public class YtelseskontraktDto {
    String status;
    String ytelsestype;
    Dato datoMottatt;
    Dato datoFra;
    Dato datoTil;

    public YtelseskontraktDto withYtelsestype(String ytelsestype) {
        this.ytelsestype = ytelsestype;
        return this;
    }

    public YtelseskontraktDto withStatus(String status) {
        this.status = status;
        return this;
    }

    public YtelseskontraktDto withDatoMottatt(XMLGregorianCalendar datoKravMottatt) {
        datoMottatt = new Dato(datoKravMottatt.getYear(), datoKravMottatt.getMonth(), datoKravMottatt.getDay());
        return this;
    }

    public YtelseskontraktDto withDatoMottatt(LocalDate datoKravMottatt) {
        datoMottatt = new Dato(datoKravMottatt.getYear(), datoKravMottatt.getMonthValue(), datoKravMottatt.getDayOfMonth());
        return this;
    }

    public YtelseskontraktDto withDatoTil(XMLGregorianCalendar datoTil) {
        this.datoTil = new Dato(datoTil.getYear(), datoTil.getMonth(), datoTil.getDay());
        return this;
    }

    public YtelseskontraktDto withDatoTil(LocalDate datoTil) {
        this.datoTil = new Dato(datoTil.getYear(), datoTil.getMonthValue(), datoTil.getDayOfMonth());
        return this;
    }

    public YtelseskontraktDto withDatoFra(XMLGregorianCalendar datoFra) {
        this.datoFra = new Dato(datoFra.getYear(), datoFra.getMonth(), datoFra.getDay());
        return this;
    }

    public YtelseskontraktDto withDatoFra(LocalDate datoFra) {
        this.datoFra = new Dato(datoFra.getYear(), datoFra.getMonthValue(), datoFra.getDayOfMonth());
        return this;
    }

    public void setDatoFra(XMLGregorianCalendar datoFra) {
        this.datoFra = new Dato(datoFra.getYear(), datoFra.getMonth(), datoFra.getDay());
    }

    public void setDatoTil(XMLGregorianCalendar datoTil) {
        this.datoTil = new Dato(datoTil.getYear(), datoTil.getMonth(), datoTil.getDay());
    }

}
