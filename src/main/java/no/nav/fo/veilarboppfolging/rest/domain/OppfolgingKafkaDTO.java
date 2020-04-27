package no.nav.fo.veilarboppfolging.rest.domain;

import lombok.Builder;
import lombok.Value;

import java.sql.Timestamp;

@Value
@Builder
public class OppfolgingKafkaDTO {
    String aktoerid;
    String veileder;
    boolean oppfolging;
    boolean nyForVeileder;
    boolean manuell;
    Timestamp endretTimestamp;
    Timestamp startDato;
}
