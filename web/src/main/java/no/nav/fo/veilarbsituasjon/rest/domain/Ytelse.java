package no.nav.fo.veilarbsituasjon.rest.domain;


import java.time.LocalDate;

public class Ytelse {

    private String status;
    private LocalDate datoMottatt;
    private LocalDate datoFra;
    private LocalDate datoTil;

    public Ytelse(String ytelseType, String status, LocalDate datoMottatt, LocalDate datoFra, LocalDate datoTil) {
        this.ytelseType = ytelseType;
        this.status = status;
        this.datoMottatt = datoMottatt;
        this.datoFra = datoFra;
        this.datoTil = datoTil;
    }

    final private String ytelseType;

    final public String getYtelseType() {
        return ytelseType;
    }

    final public String getStatus() {
        return status;
    }

    final public LocalDate getDatoMottatt() {
        return datoMottatt;
    }

    final public LocalDate getDatoFra() {
        return datoFra;
    }

    final public LocalDate getDatoTil() {
        return datoTil;
    }


}
