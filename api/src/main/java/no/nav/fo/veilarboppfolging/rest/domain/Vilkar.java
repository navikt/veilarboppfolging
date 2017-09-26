package no.nav.fo.veilarboppfolging.rest.domain;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;

@Data
@Accessors(chain = true)
public class Vilkar {

    private Date dato;
    private VilkarStatusApi vilkarstatus;
    private String tekst;
    private String hash;
}
