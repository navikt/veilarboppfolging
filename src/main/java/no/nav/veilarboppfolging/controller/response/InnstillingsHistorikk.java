package no.nav.veilarboppfolging.controller.response;

import lombok.Builder;
import lombok.Value;
import no.nav.veilarboppfolging.repository.enums.KodeverkBruker;

import java.time.ZonedDateTime;

@Value
@Builder
public class InnstillingsHistorikk {
    Type type;
    ZonedDateTime dato;
    String begrunnelse;
    KodeverkBruker opprettetAv;
    String opprettetAvBrukerId;
    Long dialogId;
    String enhet;

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
