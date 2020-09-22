package no.nav.veilarboppfolging.domain.arena;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.ZonedDateTime;
import java.util.Date;

@Data
@Accessors(chain = true)
public class MoteplanDTO {
    ZonedDateTime startDato; //startKlokkeslett kan også være i denne
    ZonedDateTime sluttDato;
    String sted;
}

