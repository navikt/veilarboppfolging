package no.nav.fo.veilarboppfolging.domain;

import lombok.Data;
import lombok.experimental.Accessors;
import java.util.Date;

@Data
@Accessors(chain = true)
public class BrukerRegistrering {
    private Date opprettetDato;
    private String aktorId;
    private boolean enigIOppsummering;
    private String besvarelse;
    private String oppsummering;
}
