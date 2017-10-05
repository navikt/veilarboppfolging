package no.nav.fo.veilarboppfolging.domain;

import lombok.Data;
import lombok.experimental.Accessors;

import java.sql.Timestamp;

@Data
@Accessors(chain = true)
public class ManuellStatus {
    public ManuellStatus(String aktorId, boolean manuell, Timestamp dato, String begrunnelse, KodeverkBruker opprettetAv, String opprettetAvBrukerId) {
        this.aktorId = aktorId;
        this.manuell = manuell;
        this.dato = dato;
        this.begrunnelse = begrunnelse;
        this.opprettetAv = opprettetAv;
        this.opprettetAvBrukerId = opprettetAvBrukerId;
    }

    private long id;
    private String aktorId;
    private boolean manuell;
    private Timestamp dato;
    private String begrunnelse;
    private KodeverkBruker opprettetAv;
    private String opprettetAvBrukerId;
}
