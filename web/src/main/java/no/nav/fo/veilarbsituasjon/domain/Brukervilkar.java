package no.nav.fo.veilarbsituasjon.domain;

import lombok.Data;
import lombok.experimental.Accessors;

import java.sql.Timestamp;

@Data
@Accessors(chain = true)
public class Brukervilkar {
    public Timestamp dato;
    public VilkarStatus vilkarstatus;
    public String tekst;
}
