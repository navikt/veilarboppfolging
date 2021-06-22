package no.nav.veilarboppfolging.repository.entity;

import lombok.Builder;
import lombok.Value;
import lombok.With;
import no.nav.veilarboppfolging.repository.enums.KodeverkBruker;

import java.time.ZonedDateTime;

@Value
@Builder
@With
public class KvpEntity {
    long kvpId;
    long serial;
    String aktorId;
    String enhet;
    String opprettetAv;
    ZonedDateTime opprettetDato;
    String opprettetBegrunnelse;
    KodeverkBruker opprettetKodeverkbruker;
    String avsluttetAv;
    ZonedDateTime avsluttetDato;
    String avsluttetBegrunnelse;
    KodeverkBruker avsluttetKodeverkbruker;
}
