package no.nav.veilarboppfolging.domain;

import lombok.Builder;
import lombok.Value;
import lombok.With;

import java.time.ZonedDateTime;

@Value
@Builder
@With
public class Kvp {
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
