package no.nav.fo.veilarbsituasjon.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "AKTOER_ID_TO_VEILEDER")
public class AktoerIdToVeileder {

    @Id
    @Column(name = "AKTOERID")
    public String aktoerid;

    @Column(name = "VEILEDER")
    public String veileder;

    public AktoerIdToVeileder withAktoerId(String aktoerId) {
        this.aktoerid = aktoerId;
        return this;
    }

    public AktoerIdToVeileder withVeileder(String veileder) {
        this.veileder = veileder;
        return this;
    }

    public String toString() {
        return "{aktoerId:"+aktoerid+",identVeileder:"+veileder+"}";
    }
}
