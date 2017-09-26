package no.nav.fo.veilarboppfolging.domain;

import lombok.Data;
import lombok.experimental.Accessors;

import java.sql.Timestamp;

@Data
@Accessors(chain = true)
public class MalData {

    private long id;
    private String aktorId;
    private String mal;
    private String endretAv;
    private Timestamp dato;

    public String getEndretAvFormattert() {
        return erEndretAvBruker()? "BRUKER" : "VEILEDER";
    }

    public boolean erEndretAvBruker() {
        return endretAv != null && endretAv.equals(aktorId);
    }

}