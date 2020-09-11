package no.nav.veilarboppfolging.controller.domain;

import lombok.AllArgsConstructor;
import no.nav.veilarboppfolging.client.behandle_arbeidssoker.ArenaFeilException;

@AllArgsConstructor
public class ArenaFeilDTO {
    ArenaFeilException.Type type;
}
