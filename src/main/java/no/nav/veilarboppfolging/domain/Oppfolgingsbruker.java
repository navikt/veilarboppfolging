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

    public String getAktoerId() {
        return aktoerId;
    }

    public OppfolgingStartBegrunnelse getOppfolgingStartBegrunnelse() {
        return oppfolgingStartBegrunnelse;
    }

    public static Oppfolgingsbruker reaktivertBruker(AktorId aktorId) {
        return new Oppfolgingsbruker(aktorId.get(), OppfolgingStartBegrunnelse.REAKTIVERT);
    }

    public static Oppfolgingsbruker sykmeldtMerOppfolgingsBruker(AktorId aktorId, SykmeldtBrukerType sykmeldtBrukerType) {
        return new SykmeldtBruker(aktorId, OppfolgingStartBegrunnelse.SYKMELDT_MER_OPPFOLGING, sykmeldtBrukerType);
    }

    public static Oppfolgingsbruker arbeidssokerOppfolgingsBruker(AktorId aktorId, Innsatsgruppe innsatsgruppe) {
        return new Arbeidssoker(aktorId, OppfolgingStartBegrunnelse.ARBEIDSSOKER_REGISTRERING, innsatsgruppe);
    }

    public static Oppfolgingsbruker nyttArbeidssøkerregisterBruker(AktorId aktorId) {
        return new NyttArbeidssokerRegisterBruker(aktorId, OppfolgingStartBegrunnelse.NYTT_ARBEIDSSØKERREGISTER);
    }

    public static Oppfolgingsbruker arenaSyncOppfolgingBruker(AktorId aktorId, Formidlingsgruppe formidlingsgruppe) {
        if (formidlingsgruppe == Formidlingsgruppe.ISERV) throw new IllegalStateException("ISERV skal ikke starte oppfølging");
        return new Oppfolgingsbruker(aktorId.get(), formidlingsgruppe == Formidlingsgruppe.IARBS ? OppfolgingStartBegrunnelse.ARENA_SYNC_IARBS : OppfolgingStartBegrunnelse.ARENA_SYNC_ARBS);
    }

}

@EqualsAndHashCode(callSuper = true)
class Arbeidssoker extends Oppfolgingsbruker {
    Innsatsgruppe innsatsgruppe;
    Arbeidssoker(AktorId aktorId, OppfolgingStartBegrunnelse begrunnelse, Innsatsgruppe innsatsgruppe) {
        super(aktorId.get(), begrunnelse);
        this.innsatsgruppe = innsatsgruppe;
    }
}

@EqualsAndHashCode(callSuper = true)
class SykmeldtBruker extends Oppfolgingsbruker {
    SykmeldtBrukerType sykmeldtBrukerType;
    SykmeldtBruker(AktorId aktorId, OppfolgingStartBegrunnelse begrunnelse, SykmeldtBrukerType sykmeldtBrukerType) {
        super(aktorId.get(), begrunnelse);
        this.sykmeldtBrukerType = sykmeldtBrukerType;
    }
}

@EqualsAndHashCode(callSuper = true)
class NyttArbeidssokerRegisterBruker extends Oppfolgingsbruker {
    NyttArbeidssokerRegisterBruker(AktorId aktorId, OppfolgingStartBegrunnelse begrunnelse) {
        super(aktorId.get(), begrunnelse);
    }
}
