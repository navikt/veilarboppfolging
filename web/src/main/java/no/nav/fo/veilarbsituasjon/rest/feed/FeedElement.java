package no.nav.fo.veilarbsituasjon.rest.feed;

import lombok.Value;

import java.time.LocalDateTime;

@Value
class FeedElement {
    LocalDateTime timestamp;
    String aktorId;
    String veilederId;
    boolean oppfolging;
}
