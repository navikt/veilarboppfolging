package no.nav.fo.veilarbsituasjon.rest.feed;

import lombok.Value;

import java.time.LocalDateTime;

@Value
class FeedElement<T>
{
    LocalDateTime timestamp;
    T data;
}
