package no.nav.veilarboppfolging.client.varseloppgave;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.cxf.CXFClient;
import no.nav.common.cxf.StsConfig;
import no.nav.common.health.HealthCheckResult;
import no.nav.tjeneste.virksomhet.varseloppgave.v1.binding.BestillVarselOppgaveBrukerHarIkkeTilstrekkeligPaaloggingsnivaa;
import no.nav.tjeneste.virksomhet.varseloppgave.v1.binding.BestillVarselOppgaveBrukerIkkeRegistrertIIdporten;
import no.nav.tjeneste.virksomhet.varseloppgave.v1.binding.BestillVarselOppgaveSikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.varseloppgave.v1.binding.VarseloppgaveV1;
import no.nav.tjeneste.virksomhet.varseloppgave.v1.informasjon.*;
import no.nav.tjeneste.virksomhet.varseloppgave.v1.meldinger.BestillVarselOppgaveRequest;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

/**
 * Brukes for å sende eskaleringsvarsel
 */
@Slf4j
public class VarseloppgaveClientImpl implements VarseloppgaveClient {

    private final String arbeidsrettetDialogUrl;

    private final VarseloppgaveV1 varseloppgave;

    private final VarseloppgaveV1 varseloppgavePing;

    public VarseloppgaveClientImpl(String arbeidsrettetDialogUrl, String varselOppgaveV1Endpoint, StsConfig stsConfig) {
        this.arbeidsrettetDialogUrl = arbeidsrettetDialogUrl;
        varseloppgave = new CXFClient<>(VarseloppgaveV1.class)
                .withOutInterceptor(new LoggingOutInterceptor())
                .configureStsForSubject(stsConfig)
                .address(varselOppgaveV1Endpoint)
                .build();

        varseloppgavePing = new CXFClient<>(VarseloppgaveV1.class)
                .withOutInterceptor(new LoggingOutInterceptor())
                .configureStsForSystemUser(stsConfig)
                .address(varselOppgaveV1Endpoint)
                .build();
    }

    @SneakyThrows
    @Override
    public void sendEskaleringsvarsel(String aktorId, long dialogId) {
        AktoerId aktor = new AktoerId();
        aktor.setAktoerId(aktorId);
        try {
            varseloppgave.bestillVarselOppgave(lagBestillVarselOppgaveRequest(aktor, dialogId));
        } catch (BestillVarselOppgaveSikkerhetsbegrensning e) {
            log.error("Sikkerhetsbegrensning ved kall mot varseloppgaveV1 aktørId {} ", aktorId);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        } catch (BestillVarselOppgaveBrukerIkkeRegistrertIIdporten e) {
            log.error("Bruker aktørId {} ikke registert i id porten", aktorId);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "BRUKER_IKKE_REGISTRERT_I_IDPORTEN");
        } catch (BestillVarselOppgaveBrukerHarIkkeTilstrekkeligPaaloggingsnivaa e) {
            log.error("Bruker aktørId {} har ikke tilstrekkelig innloggingsnivå", aktorId);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "BRUKER_HAR_IKKE_TILSTREKKELIG_PAALOGGINGSNIVAA");
        } catch (Exception e) {
            log.error("Sending av eskaleringsvarsel feilet for aktørId {} og dialogId {}", aktorId, dialogId, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    protected String dialogUrl(long dialogId) {
        return arbeidsrettetDialogUrl + "/" + dialogId;
    }

    private BestillVarselOppgaveRequest lagBestillVarselOppgaveRequest(Aktoer aktoer, long dialogId) {
        BestillVarselOppgaveRequest bestillVarselOppgaveRequest = new BestillVarselOppgaveRequest();
        bestillVarselOppgaveRequest.setVarselOppgaveBestilling(lagVarselOppgaveBestilling(aktoer));
        bestillVarselOppgaveRequest.setOppgaveHenvendelse(lagOppgaveHenvendelse(dialogId));
        bestillVarselOppgaveRequest.setVarselMedHandling(lagVarselMedHandling());

        return bestillVarselOppgaveRequest;
    }

    private VarselOppgaveBestilling lagVarselOppgaveBestilling(Aktoer aktoer) {
        String uuid = UUID.randomUUID().toString();
        VarselOppgaveBestilling varselOppgaveBestilling = new VarselOppgaveBestilling();
        varselOppgaveBestilling.setVarselbestillingId(uuid);
        varselOppgaveBestilling.setMottaker(aktoer);

        return varselOppgaveBestilling;
    }

    private OppgaveHenvendelse lagOppgaveHenvendelse(long dialogId) {
        OppgaveType oppgaveType = new OppgaveType();
        oppgaveType.setValue("0004");

        OppgaveHenvendelse oppgaveHenvendelse = new OppgaveHenvendelse();
        oppgaveHenvendelse.setOppgaveType(oppgaveType);
        oppgaveHenvendelse.setOppgaveURL(dialogUrl(dialogId));
        oppgaveHenvendelse.setStoppRepeterendeVarsel(true);

        return oppgaveHenvendelse;
    }

    private VarselMedHandling lagVarselMedHandling() {
        VarselMedHandling varselMedHandling = new VarselMedHandling();
        varselMedHandling.setVarseltypeId("DittNAV_000008");

        return varselMedHandling;
    }

    @Override
    public HealthCheckResult checkHealth() {
        try {
            varseloppgavePing.ping();
            return HealthCheckResult.healthy();
        } catch (Exception e) {
            return HealthCheckResult.unhealthy(e);
        }
    }

}
