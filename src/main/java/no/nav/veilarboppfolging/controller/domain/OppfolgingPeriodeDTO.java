package no.nav.veilarboppfolging.controller.domain;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.ZonedDateTime;
import java.util.List;

@Data
@Accessors(chain = true)
public class OppfolgingPeriodeDTO {

    public String aktorId;
    public String veileder;
    public ZonedDateTime startDato;
    public ZonedDateTime sluttDato;
    public String begrunnelse;
    public List<KvpPeriodeDTO> kvpPerioder;

}
