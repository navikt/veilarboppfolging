package no.nav.veilarboppfolging.domain;

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
    private Long dialogId;
    private String enhet;

    public enum Type {
        SATT_TIL_MANUELL,
        SATT_TIL_DIGITAL,
        AVSLUTTET_OPPFOLGINGSPERIODE,
        ESKALERING_STARTET,
        ESKALERING_STOPPET,
        KVP_STARTET,
        KVP_STOPPET,
        VEILEDER_TILORDNET,
        OPPFOLGINGSENHET_ENDRET
    }
}