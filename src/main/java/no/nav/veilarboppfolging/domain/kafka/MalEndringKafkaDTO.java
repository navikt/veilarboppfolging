package no.nav.veilarboppfolging.domain.kafka;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.ZonedDateTime;

@Data
@Accessors(chain = true)
public class MalEndringKafkaDTO {
        private String aktorId;
        private ZonedDateTime endretTidspunk;
        private InnsenderData lagtInnAv;

    public enum InnsenderData {
        BRUKER,
        NAV
    }
}
