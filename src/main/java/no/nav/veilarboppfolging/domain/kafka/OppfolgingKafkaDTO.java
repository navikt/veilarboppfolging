package no.nav.veilarboppfolging.domain.kafka;

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
    Boolean manuell;
    Timestamp endretTimestamp;
    Timestamp startDato;
}
