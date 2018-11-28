package no.nav.fo.veilarboppfolging.rest.domain;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class Bruker {

    public String id;
    public boolean erVeileder;
    public boolean erBruker;

}
