package no.nav.veilarboppfolging.domain;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import no.nav.common.types.identer.AktorId;
import no.nav.pto_schema.enums.arena.Formidlingsgruppe;
import no.nav.veilarboppfolging.controller.request.Innsatsgruppe;
import no.nav.veilarboppfolging.controller.request.SykmeldtBrukerType;
import no.nav.veilarboppfolging.repository.entity.OppfolgingStartBegrunnelse;

@AllArgsConstructor
@EqualsAndHashCode
public class Oppfolgingsbruker {
    String aktoerId;
    OppfolgingStartBegrunnelse oppfolgingStartBegrunnelse;
    StartetAvType startetAvType;

    public String getAktoerId() {
        return aktoerId;
    }

    public OppfolgingStartBegrunnelse getOppfolgingStartBegrunnelse() {
        return oppfolgingStartBegrunnelse;
    }
    public StartetAvType getStartetAvType() {
        return startetAvType;
    }

    public static Oppfolgingsbruker sykmeldtMerOppfolgingsBruker(AktorId aktorId, SykmeldtBrukerType sykmeldtBrukerType) {
        return new SykmeldtBruker(aktorId, OppfolgingStartBegrunnelse.SYKMELDT_MER_OPPFOLGING, sykmeldtBrukerType, StartetAvType.Bruker);
    }

    public static Oppfolgingsbruker arbeidssokerOppfolgingsBruker(AktorId aktorId, Innsatsgruppe innsatsgruppe, StartetAvType startetAvType) {
        return new Arbeidssoker(aktorId, OppfolgingStartBegrunnelse.ARBEIDSSOKER_REGISTRERING, innsatsgruppe, startetAvType);
    }

    public static Oppfolgingsbruker arenaSyncOppfolgingBruker(AktorId aktorId, Formidlingsgruppe formidlingsgruppe) {
        if (formidlingsgruppe == Formidlingsgruppe.ISERV) throw new IllegalStateException("ISERV skal ikke starte oppfølging");
        var startBegrunnelse = formidlingsgruppe == Formidlingsgruppe.IARBS ? OppfolgingStartBegrunnelse.ARENA_SYNC_IARBS : OppfolgingStartBegrunnelse.ARENA_SYNC_ARBS;
        return new Oppfolgingsbruker(aktorId.get(), startBegrunnelse, StartetAvType.System);
    }

}

@EqualsAndHashCode(callSuper = true)
class Arbeidssoker extends Oppfolgingsbruker {
    Innsatsgruppe innsatsgruppe;
    Arbeidssoker(AktorId aktorId, OppfolgingStartBegrunnelse begrunnelse, Innsatsgruppe innsatsgruppe, StartetAvType startetAvType) {
        super(aktorId.get(), begrunnelse, startetAvType);
        this.innsatsgruppe = innsatsgruppe;
    }
}

@EqualsAndHashCode(callSuper = true)
class SykmeldtBruker extends Oppfolgingsbruker {
    SykmeldtBrukerType sykmeldtBrukerType;
    SykmeldtBruker(AktorId aktorId, OppfolgingStartBegrunnelse begrunnelse, SykmeldtBrukerType sykmeldtBrukerType, StartetAvType startetAvType) {
        super(aktorId.get(), begrunnelse, startetAvType);
        this.sykmeldtBrukerType = sykmeldtBrukerType;
    }
}
