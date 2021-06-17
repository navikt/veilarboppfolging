package no.nav.veilarboppfolging.client.varseloppgave;

import no.nav.common.health.HealthCheck;
import no.nav.common.types.identer.AktorId;

public interface VarseloppgaveClient extends HealthCheck {

    void sendEskaleringsvarsel(AktorId aktorId, long dialogId);
}
