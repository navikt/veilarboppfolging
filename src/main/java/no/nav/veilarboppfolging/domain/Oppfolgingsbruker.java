package no.nav.veilarboppfolging.domain;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import no.nav.common.types.identer.AktorId;
import no.nav.veilarboppfolging.controller.request.Innsatsgruppe;
import no.nav.veilarboppfolging.controller.request.SykmeldtBrukerType;
import no.nav.veilarboppfolging.repository.entity.OppfolgingStartBegrunnelse;

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

    public static Oppfolgingsbruker manueltStartetBruker(AktorId aktorId) {
        return new Oppfolgingsbruker(aktorId.get(), OppfolgingStartBegrunnelse.MANUELL);
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
