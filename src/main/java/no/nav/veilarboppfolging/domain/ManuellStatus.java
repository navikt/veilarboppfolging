package no.nav.veilarboppfolging.domain;

import lombok.Data;
import lombok.experimental.Accessors;

import java.sql.Timestamp;

@Data
@Accessors(chain = true)
public class ManuellStatus {
    private long id;
    private String aktorId;
    private boolean manuell;
    private Timestamp dato;
    private String begrunnelse;
    private KodeverkBruker opprettetAv;
    private String opprettetAvBrukerId;
}
