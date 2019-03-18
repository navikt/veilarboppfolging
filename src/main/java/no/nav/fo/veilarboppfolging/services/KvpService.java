package no.nav.fo.veilarboppfolging.services;

import lombok.SneakyThrows;
import lombok.val;
import no.nav.apiapp.feil.Feil;
import no.nav.apiapp.feil.UlovligHandling;
import no.nav.apiapp.security.PepClient;
import no.nav.common.auth.SubjectHandler;
import no.nav.dialogarena.aktor.AktorService;
import no.nav.fo.veilarboppfolging.db.EskaleringsvarselRepository;
import no.nav.fo.veilarboppfolging.db.KvpRepository;
import no.nav.fo.veilarboppfolging.db.OppfolgingsStatusRepository;
import no.nav.fo.veilarboppfolging.domain.KodeverkBruker;
import no.nav.fo.veilarboppfolging.domain.Kvp;
import no.nav.fo.veilarboppfolging.domain.OppfolgingTable;
import no.nav.fo.veilarboppfolging.utils.FunksjonelleMetrikker;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.OppfoelgingPortType;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.meldinger.HentOppfoelgingsstatusRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Inject;
import java.util.function.Supplier;

import static no.nav.apiapp.feil.FeilType.UKJENT;
import static no.nav.fo.veilarboppfolging.domain.KodeverkBruker.NAV;

@Component
public class KvpService {

    static final String ESKALERING_AVSLUTTET_FORDI_KVP_BLE_AVSLUTTET = "Eskalering avsluttet fordi KVP ble avsluttet";

    @Inject
    private KvpRepository kvpRepository;

    @Inject
    private AktorService aktorService;

    @Inject
    private OppfoelgingPortType oppfoelgingPortType;

    @Inject
    private PepClient pepClient;

    @Inject
    private OppfolgingsStatusRepository oppfolgingsStatusRepository;

    @Inject 
    private EskaleringsvarselRepository eskaleringsvarselRepository;

    private static final Supplier<Feil> AKTOR_ID_FEIL = () -> new Feil(UKJENT, "Fant ikke aktørId for fnr");

    @SneakyThrows
    public void startKvp(String fnr, String begrunnelse) {
        pepClient.sjekkLeseTilgangTilFnr(fnr);
        String aktorId = aktorService.getAktorId(fnr).orElseThrow(AKTOR_ID_FEIL);
        OppfolgingTable oppfolgingTable = oppfolgingsStatusRepository.fetch(aktorId);
        
        if (oppfolgingTable == null || !oppfolgingTable.isUnderOppfolging()) {
            throw new UlovligHandling();
        }

        String enhet = getEnhet(fnr);
        pepClient.sjekkTilgangTilEnhet(enhet);

        String veilederId = SubjectHandler.getIdent().orElseThrow(RuntimeException::new);
        kvpRepository.startKvp(
                aktorId,
                enhet,
                veilederId,
                begrunnelse);

        FunksjonelleMetrikker.startKvp();
    }

    @SneakyThrows
    @Transactional
    public void stopKvp(String fnr, String begrunnelse) {
        pepClient.sjekkLeseTilgangTilFnr(fnr);
        pepClient.sjekkTilgangTilEnhet(getEnhet(fnr));
        String aktorId = aktorService.getAktorId(fnr).orElseThrow(AKTOR_ID_FEIL);
        stopKvpUtenEnhetSjekk(aktorId, begrunnelse, NAV);
    }

    void stopKvpUtenEnhetSjekk(String aktorId, String begrunnelse, KodeverkBruker kodeverkBruker) {
        String veilederId = SubjectHandler.getIdent().orElseThrow(RuntimeException::new);

        OppfolgingTable oppfolgingTable = oppfolgingsStatusRepository.fetch(aktorId);
        if(oppfolgingTable != null && oppfolgingTable.getGjeldendeEskaleringsvarselId() != 0) {
            eskaleringsvarselRepository.finish(
                    aktorId, 
                    oppfolgingTable.getGjeldendeEskaleringsvarselId(), 
                    veilederId, 
                    ESKALERING_AVSLUTTET_FORDI_KVP_BLE_AVSLUTTET);
        }

        kvpRepository.stopKvp(
                aktorId,
                veilederId,
                begrunnelse,
                kodeverkBruker);

        FunksjonelleMetrikker.stopKvp();
    }

    Kvp gjeldendeKvp(String aktorId) {
        return kvpRepository.fetch(kvpRepository.gjeldendeKvp(aktorId));
    }

    @SneakyThrows
    private String getEnhet(String fnr) {
        val req = new HentOppfoelgingsstatusRequest();
        req.setPersonidentifikator(fnr);
        val res = oppfoelgingPortType.hentOppfoelgingsstatus(req);
        return res.getNavOppfoelgingsenhet();
    }

}
