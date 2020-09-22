package no.nav.veilarboppfolging.domain;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.ZonedDateTime;

@Data
@Accessors(chain = true)
public class MalData {

    private long id;
    private String aktorId;
    private String mal;
    private String endretAv;
    private ZonedDateTime dato;

    public String getEndretAvFormattert() {
        return erEndretAvBruker()? "BRUKER" : "VEILEDER";
    }

    public boolean erEndretAvBruker() {
        return endretAv != null && endretAv.equals(aktorId);
    }

}