package no.nav.fo.veilarbsituasjon.domain;

import lombok.Data;
import lombok.experimental.Accessors;
import no.nav.tjeneste.virksomhet.behandlesituasjon.v1.informasjon.Vilkaarsstatuser;

import java.sql.Timestamp;

@Data
@Accessors(chain = true)
public class Brukervilkar {
    public Brukervilkar(String aktorId, Timestamp dato, Vilkaarsstatuser vilkarstatus, String tekst, String hash) {
        this.aktorId = aktorId;
        this.dato = dato;
        this.vilkarstatus = vilkarstatus;
        this.tekst = tekst;
        this.hash = hash;
    }

    private long id;
    private String aktorId;
    private Timestamp dato;
    private Vilkaarsstatuser vilkarstatus;
    private String tekst;
    private String hash;
}
