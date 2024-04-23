package no.nav.veilarboppfolging.domain;

import lombok.Builder;
import lombok.Value;
import no.nav.veilarboppfolging.controller.request.Innsatsgruppe;
import no.nav.veilarboppfolging.controller.request.SykmeldtBrukerType;
import no.nav.veilarboppfolging.repository.entity.OppfolgingStartAarsak;

@Value
@Builder
public class Oppfolgingsbruker {
    String aktoerId;
    Innsatsgruppe innsatsgruppe;
    SykmeldtBrukerType sykmeldtBrukerType;

    public OppfolgingStartAarsak getOppfolgingStartAarsak() {
        if (sykmeldtBrukerType == null) return OppfolgingStartAarsak.SYKMELDT_MER_OPPFOLGING;
        return OppfolgingStartAarsak.ARBEIDSSOKER;
    }
}
