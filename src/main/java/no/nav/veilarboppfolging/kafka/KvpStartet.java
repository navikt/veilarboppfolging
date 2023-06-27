package no.nav.veilarboppfolging.kafka;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Value;

import java.time.ZonedDateTime;

@Value
@NoArgsConstructor(force = true)
@AllArgsConstructor
@Builder
public class KvpStartet {
    String opprettetAv;
    ZonedDateTime opprettetDato;
    String opprettetBegrunnelse;
}
