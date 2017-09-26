package no.nav.fo.veilarboppfolging.services;

import no.nav.apiapp.feil.Feil;
import no.nav.tjeneste.virksomhet.varseloppgave.v1.VarseloppgaveV1;
import no.nav.tjeneste.virksomhet.varseloppgave.v1.informasjon.*;
import no.nav.tjeneste.virksomhet.varseloppgave.v1.meldinger.BestillVarselOppgaveRequest;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.UUID;

import static no.nav.apiapp.util.PropertyUtils.getRequiredProperty;
import static org.slf4j.LoggerFactory.getLogger;

@Component
public class EskaleringsvarselService {

    private static final Logger LOG  = getLogger(EskaleringsvarselService.class);
    private static final String VARSELTYPE_ID = "DittNAV_000008";

    @Inject
    private VarseloppgaveV1 varseloppgaveV1;

    private String aktivitetsplanBaseUrl = getRequiredProperty("aktivitetsplan.url");

    public void sendEskaleringsvarsel(String aktorId, long dialogId) {
        try {
            Aktoer aktor = new AktoerId().withAktoerId(aktorId);
            varseloppgaveV1.bestillVarselOppgave(lagBestillVarselOppgaveRequest(aktor, dialogId));
        } catch (Exception e) {
            LOG.error(e.toString());
            throw new Feil(Feil.Type.UKJENT);
        }
    }

    protected String dialogUrl(long dialogId) {
        return aktivitetsplanBaseUrl + "/dialog/" + dialogId;
    }

    private BestillVarselOppgaveRequest lagBestillVarselOppgaveRequest(Aktoer aktoer, long dialogId) {
        return new BestillVarselOppgaveRequest()
                .withVarselOppgaveBestilling(lagVarselOppgaveBestilling(aktoer))
                .withOppgaveHenvendelse(lagOppgaveHenvendelse(dialogId))
                .withVarselMedHandling(lagVarselMedHandling());
    }

    private VarselOppgaveBestilling lagVarselOppgaveBestilling(Aktoer aktoer) {
        String uuid = UUID.randomUUID().toString();
        return new VarselOppgaveBestilling()
                .withVarselbestillingId(uuid)
                .withMottaker(aktoer);
    }

    private OppgaveHenvendelse lagOppgaveHenvendelse(long dialogId) {
        OppgaveType oppgaveType = new OppgaveType().withValue(VARSELTYPE_ID);
        return new OppgaveHenvendelse()
                .withOppgaveType(oppgaveType)
                .withOppgaveURL(dialogUrl(dialogId))
                .withStoppRepeterendeVarsel(true);
    }

    private VarselMedHandling lagVarselMedHandling() {
        return new VarselMedHandling().withVarseltypeId(VARSELTYPE_ID);
    }
}
