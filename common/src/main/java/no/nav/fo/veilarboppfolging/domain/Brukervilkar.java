package no.nav.fo.veilarboppfolging.domain;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;

@Data
@Accessors(chain = true)
public class Brukervilkar {
    public Brukervilkar() {

    }

    public Brukervilkar(String aktorId, Date dato, VilkarStatus vilkarstatus, String tekst, String hash) {
        this.aktorId = aktorId;
        this.dato = dato;
        this.vilkarstatus = vilkarstatus;
        this.tekst = tekst;
        this.hash = hash;
    }

    private long id;
    private String aktorId;
    private Date dato;
    private VilkarStatus vilkarstatus;
    private String tekst;
    private String hash;
}
