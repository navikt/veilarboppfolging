package no.nav.veilarboppfolging.client.varseloppgave;

import no.nav.common.health.HealthCheck;

public interface VarseloppgaveClient extends HealthCheck {

    void sendEskaleringsvarsel(String aktorId, long dialogId);
}
