package no.nav.fo.veilarbsituasjon.domain;

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
        return endretAv.equals(aktorId) ? "BRUKER" : "VEILEDER";
    }

}