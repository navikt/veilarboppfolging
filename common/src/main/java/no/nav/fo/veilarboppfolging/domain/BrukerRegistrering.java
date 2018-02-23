package no.nav.fo.veilarboppfolging.domain;

import lombok.Builder;
import lombok.Value;
import java.util.Date;

@Value
@Builder
public class BrukerRegistrering {
    String nusKode;
    String yrkesPraksis;
    Date opprettetDato;
    boolean enigIOppsummering;
    String oppsummering;
    boolean utdanningBestatt;
    boolean utdanningGodkjentNorge;
    boolean harHelseutfordringer;
    String situasjon;
}
