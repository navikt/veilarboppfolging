package no.nav.veilarboppfolging.domain;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import no.nav.common.types.identer.AktorId;
import no.nav.pto_schema.enums.arena.Formidlingsgruppe;
import no.nav.pto_schema.enums.arena.Kvalifiseringsgruppe;
import no.nav.veilarboppfolging.controller.request.Innsatsgruppe;
import no.nav.veilarboppfolging.controller.request.SykmeldtBrukerType;
import no.nav.veilarboppfolging.repository.entity.OppfolgingStartBegrunnelse;

import java.util.Objects;

@NoArgsConstructor
@AllArgsConstructor
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
        return new Arbeissoker(aktorId, OppfolgingStartBegrunnelse.ARBEIDSSOKER_REGISTRERING, innsatsgruppe);
    }

    public static Oppfolgingsbruker arenaSyncOppfolgingBruker(AktorId aktorId) {
        return new Oppfolgingsbruker(aktorId.get(), OppfolgingStartBegrunnelse.ARENA_SYNC);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Oppfolgingsbruker oppfolgingsbruker) {
            return Objects.equals(oppfolgingsbruker.aktoerId, this.aktoerId)
                && oppfolgingsbruker.oppfolgingStartBegrunnelse == this.oppfolgingStartBegrunnelse;
        }
        return false;
    }
}

class Arbeissoker extends Oppfolgingsbruker {
    Innsatsgruppe innsatsgruppe;
    Arbeissoker(AktorId aktorId, OppfolgingStartBegrunnelse begrunnelse, Innsatsgruppe innsatsgruppe) {
        super(aktorId.get(), begrunnelse);
        this.innsatsgruppe = innsatsgruppe;
    }
}

class SykmeldtBruker extends Oppfolgingsbruker {
    SykmeldtBrukerType sykmeldtBrukerType;
    SykmeldtBruker(AktorId aktorId, OppfolgingStartBegrunnelse begrunnelse, SykmeldtBrukerType sykmeldtBrukerType) {
        super(aktorId.get(), begrunnelse);
        this.sykmeldtBrukerType = sykmeldtBrukerType;
    }
}
