package no.nav.fo.veilarbsituasjon.domain;

import lombok.Data;
import lombok.experimental.Accessors;

import java.sql.Timestamp;

@Data
@Accessors(chain = true)
public class Status {
    public Status(String aktorId, boolean manuell, Timestamp dato, String begrunnelse) {
        this.aktorId = aktorId;
        this.manuell = manuell;
        this.dato = dato;
        this.begrunnelse = begrunnelse;
    }

    private long id;
    private String aktorId;
    private boolean manuell;
    private Timestamp dato;
    private String begrunnelse;
}
