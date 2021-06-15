package no.nav.veilarboppfolging.controller.response;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Accessors(chain = true)
public class OppfolgingPeriodeDTO {

    public UUID uuid;
    public String aktorId;
    public String veileder;
    public ZonedDateTime startDato;
    public ZonedDateTime sluttDato;
    public String begrunnelse;
    public List<KvpPeriodeDTO> kvpPerioder;

}
