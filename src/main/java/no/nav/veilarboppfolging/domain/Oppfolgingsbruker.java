package no.nav.veilarboppfolging.domain;

import lombok.Builder;
import lombok.Value;
import no.nav.veilarboppfolging.controller.request.Innsatsgruppe;
import no.nav.veilarboppfolging.controller.request.SykmeldtBrukerType;
import no.nav.veilarboppfolging.repository.entity.OppfolgingStartBegrunnelse;

@Value
@Builder
public class Oppfolgingsbruker {
    String aktoerId;
    Innsatsgruppe innsatsgruppe;
    SykmeldtBrukerType sykmeldtBrukerType;

    public OppfolgingStartBegrunnelse getOppfolgingStartBegrunnelse() {
        if (sykmeldtBrukerType != null) return OppfolgingStartBegrunnelse.SYKMELDT_MER_OPPFOLGING;
        return OppfolgingStartBegrunnelse.ARBEIDSSOKER;
    }
}
