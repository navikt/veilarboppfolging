package no.nav.veilarboppfolging.controller.v3.request;

import java.time.ZonedDateTime;

public record MaalInnhold(
        String maal,
        String endretAv,
        ZonedDateTime dato
) {
}
