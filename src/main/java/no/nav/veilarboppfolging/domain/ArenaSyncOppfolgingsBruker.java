package no.nav.veilarboppfolging.domain;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import no.nav.common.types.identer.AktorId;
import no.nav.pto_schema.enums.arena.Formidlingsgruppe;
import no.nav.pto_schema.enums.arena.Kvalifiseringsgruppe;
import no.nav.veilarboppfolging.repository.entity.OppfolgingStartBegrunnelse;

@EqualsAndHashCode(callSuper = true)
public class ArenaSyncOppfolgingsBruker extends Oppfolgingsbruker {
    @Getter
    Kvalifiseringsgruppe kvalifiseringsgruppe;
    Formidlingsgruppe formidlingsgruppe;

    ArenaSyncOppfolgingsBruker(AktorId aktorId, Formidlingsgruppe formidlingsgruppe, Kvalifiseringsgruppe kvalifiseringsgruppe) {
        super(aktorId.get(), formidlingsgruppe == Formidlingsgruppe.IARBS ? OppfolgingStartBegrunnelse.ARENA_SYNC_IARBS : OppfolgingStartBegrunnelse.ARENA_SYNC_ARBS, StartetAvType.SYSTEM);
        this.kvalifiseringsgruppe = kvalifiseringsgruppe;
    }
}
