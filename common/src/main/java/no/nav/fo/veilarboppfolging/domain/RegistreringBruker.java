package no.nav.fo.veilarboppfolging.domain;

import lombok.Data;
import lombok.experimental.Accessors;
import java.util.Date;

@Data
@Accessors(chain = true)
public class RegistreringBruker {
    private String nusKode;
    private String yrkesPraksis;
    private Date opprettetDato;
    private boolean enigIOppsummering;
    private String oppsummering;
    private boolean utdanningBestatt;
    private boolean utdanningGodkjentNorge;
    private boolean harJobbetSammenhengende;
    private boolean harHelseutfordringer;
    private String situasjon;
}
