package no.nav.veilarboppfolging.kafka;


import lombok.*;
import no.nav.common.types.identer.AktorId;

import java.time.ZonedDateTime;

@Value
@NoArgsConstructor(force = true)
@AllArgsConstructor
@Builder
@With
public class KvpPeriode {
    KvpPeriodeEventType event;
    String aktorId;
    String enhetId;
    KvpStartet startet;
    KvpAvsluttet avsluttet;

    public static KvpPeriode start(AktorId aktorId, String enhetId, String opprettetAv, ZonedDateTime startDato, String begrunnelse) {
        var kvpStartet = new KvpStartet(opprettetAv, startDato, begrunnelse);
        return new KvpPeriode(KvpPeriodeEventType.STARTET, aktorId.get(), enhetId, kvpStartet, null);
    }

    public KvpPeriode avslutt(String avsluttetAv, ZonedDateTime avsluttetTidspunkt, String begrunnelse) {
        return this.withAvsluttet(new KvpAvsluttet(avsluttetAv, avsluttetTidspunkt, begrunnelse)).withEvent(KvpPeriodeEventType.AVSLUTTET);
    }
}


