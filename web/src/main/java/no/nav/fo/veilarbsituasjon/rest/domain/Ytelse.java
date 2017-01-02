package no.nav.fo.veilarbsituasjon.rest.domain;


import java.time.LocalDate;

public class Ytelse {

    public Ytelse(String ytelseType, String status, LocalDate datoMottatt, LocalDate datoFra, LocalDate datoTil) {
        this.ytelseType = ytelseType;
        this.status = status;
        this.datoMottatt = datoMottatt;
        this.datoFra = datoFra;
        this.datoTil = datoTil;
    }

    private String ytelseType;

    public String getYtelseType() {
        return ytelseType;
    }

    public String getStatus() {
        return status;
    }

    public LocalDate getDatoMottatt() {
        return datoMottatt;
    }

    public LocalDate getDatoFra() {
        return datoFra;
    }

    public LocalDate getDatoTil() {
        return datoTil;
    }

    private String status;
    private LocalDate datoMottatt;
    private LocalDate datoFra;
    private LocalDate datoTil;



}
