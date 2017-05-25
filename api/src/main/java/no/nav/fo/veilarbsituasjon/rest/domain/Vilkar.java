package no.nav.fo.veilarbsituasjon.rest.domain;

import lombok.Data;
import lombok.experimental.Accessors;

import java.sql.Timestamp;

@Data
@Accessors(chain = true)
public class Vilkar {

    private Timestamp dato;
    private VilkarStatusApi vilkarstatus;
    private String tekst;
    private String hash;
}
