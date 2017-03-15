package no.nav.fo.veilarbsituasjon.domain;

import lombok.Data;
import lombok.experimental.Accessors;

import java.sql.Timestamp;

@Data
@Accessors(chain = true)
public class Brukervilkar {
    public Brukervilkar(String aktorId, Timestamp dato, VilkarStatus vilkarstatus, String tekst) {
        this.aktorId = aktorId;
        this.dato = dato;
        this.vilkarstatus = vilkarstatus;
        this.tekst = tekst;
    }

    private long id;
    private String aktorId;
    private Timestamp dato;
    private VilkarStatus vilkarstatus;
    private String tekst;
}
