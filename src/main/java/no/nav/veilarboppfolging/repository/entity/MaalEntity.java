package no.nav.veilarboppfolging.repository.entity;


import java.time.ZonedDateTime;

public class MaalEntity {
    private long id;
    private String aktorId;
    private String mal;
    private String endretAv;
    private ZonedDateTime dato;

    public String getEndretAvFormattert() {
        return erEndretAvBruker() ? "BRUKER" : "VEILEDER";
    }

    public boolean erEndretAvBruker() {
        return endretAv != null && endretAv.equals(aktorId);
    }
}