package no.nav.fo.veilarbsituasjon.domain;

import lombok.Builder;
import lombok.Value;

import java.util.Date;

@Value
@Builder
public class InnstillingsHistorikk {
    private Type type;
    private Date dato;
    private String begrunnelse;
    private KodeverkBruker opprettetAv;
    private String opprettetAvBrukerId;

    public enum Type {
        SATT_TIL_MANUELL,
        SATT_TIL_DIGITAL,
        AVSLUTTET_OPPFOLGINGSPERIODE
    }

}
