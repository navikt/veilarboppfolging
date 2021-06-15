package no.nav.veilarboppfolging.controller.response;

import lombok.Builder;
import lombok.Value;
import no.nav.veilarboppfolging.domain.KodeverkBruker;

import java.time.ZonedDateTime;

@Value
@Builder
public class InnstillingsHistorikk {
    private Type type;
    private ZonedDateTime dato;
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
