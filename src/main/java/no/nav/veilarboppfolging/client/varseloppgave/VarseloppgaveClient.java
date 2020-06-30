package no.nav.veilarboppfolging.client.varseloppgave;

import no.nav.common.health.HealthCheck;
import no.nav.veilarboppfolging.domain.Fnr;
import no.nav.veilarboppfolging.domain.Innsatsgruppe;

public interface VarseloppgaveClient extends HealthCheck {

    void sendEskaleringsvarsel(String aktorId, long dialogId);
}
