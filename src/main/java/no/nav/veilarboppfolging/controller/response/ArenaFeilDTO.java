package no.nav.veilarboppfolging.controller.response;

import lombok.Data;
import lombok.experimental.Accessors;
import no.nav.veilarboppfolging.client.behandle_arbeidssoker.ArenaFeilException;

@Data
@Accessors(chain = true)
public class ArenaFeilDTO {
    ArenaFeilException.Type type;
}
