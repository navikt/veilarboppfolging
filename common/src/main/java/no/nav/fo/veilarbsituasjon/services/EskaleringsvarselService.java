package no.nav.fo.veilarbsituasjon.services;

import no.nav.tjeneste.virksomhet.varseloppgave.v1.VarseloppgaveV1;
import no.nav.tjeneste.virksomhet.varseloppgave.v1.informasjon.*;
import no.nav.tjeneste.virksomhet.varseloppgave.v1.meldinger.BestillVarselOppgaveRequest;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.UUID;

@Component
public class EskaleringsvarselService {
    private static final String VARSELTYPE_ID = "DittNAV_000008";
    @Inject
    VarseloppgaveV1 varseloppgaveV1;

    public void sendEskaleringsvarsel(String aktorId, String varselUrl) {
        try {
            Aktoer aktor = new AktoerId().withAktoerId(aktorId);
            varseloppgaveV1.bestillVarselOppgave(lagBestillVarselOppgaveRequest(aktor, varselUrl));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private BestillVarselOppgaveRequest lagBestillVarselOppgaveRequest(Aktoer aktoer, String varselUrl) {
        return new BestillVarselOppgaveRequest()
                .withVarselOppgaveBestilling(lagVarselOppgaveBestilling(aktoer))
                .withOppgaveHenvendelse(lagOppgaveHenvendelse(varselUrl))
                .withVarselMedHandling(lagVarselMedHandling(varselUrl));
    }

    private VarselMedHandling lagVarselMedHandling(String varselUrl) {
        return new VarselMedHandling()
                .withVarseltypeId(VARSELTYPE_ID)
                .withParameterListe(new Parameter().withKey("url").withValue(varselUrl));
    }

    private OppgaveHenvendelse lagOppgaveHenvendelse(String url) {
        OppgaveType oppgaveType = new OppgaveType().withValue(VARSELTYPE_ID);
        return new OppgaveHenvendelse()
                .withOppgaveType(oppgaveType)
                .withOppgaveURL(url)
                .withStoppRepeterendeVarsel(true);
    }

    private VarselOppgaveBestilling lagVarselOppgaveBestilling(Aktoer aktoer) {
        String uuid = UUID.randomUUID().toString();
        return new VarselOppgaveBestilling()
                .withVarselbestillingId(uuid)
                .withMottaker(aktoer);
    }
}
