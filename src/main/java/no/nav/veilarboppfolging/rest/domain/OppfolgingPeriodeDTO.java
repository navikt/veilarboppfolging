package no.nav.veilarboppfolging.rest.domain;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;
import java.util.List;

@Data
@Accessors(chain = true)
public class OppfolgingPeriodeDTO {

    public String aktorId;
    public String veileder;
    public Date startDato;
    public Date sluttDato;
    public String begrunnelse;
    public List<KvpPeriodeDTO> kvpPerioder;

}
