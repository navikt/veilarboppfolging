package no.nav.veilarboppfolging.repository.entity;

import lombok.Data;
import lombok.experimental.Accessors;
import no.nav.veilarboppfolging.domain.StartetAvType;

import java.time.ZonedDateTime;

@Data
@Accessors(chain = true)
public class ManuellStatusEntity {
    private long id;
    private String aktorId;
    private boolean manuell;
    private ZonedDateTime dato;
    private String begrunnelse;
    private StartetAvType opprettetAv;
    private String opprettetAvBrukerId;
}
