package no.nav.fo.veilarboppfolging.services;

import lombok.SneakyThrows;
import lombok.val;
import no.nav.apiapp.feil.Feil;
import no.nav.apiapp.feil.FeilType;
import no.nav.apiapp.feil.IngenTilgang;
import no.nav.apiapp.feil.UlovligHandling;
import no.nav.apiapp.security.veilarbabac.Bruker;
import no.nav.apiapp.security.veilarbabac.VeilarbAbacPepClient;
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

import javax.inject.Inject;

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
    private VeilarbAbacPepClient pepClient;

    @Inject
    private OppfolgingsStatusRepository oppfolgingsStatusRepository;

    @Inject 
    private EskaleringsvarselRepository eskaleringsvarselRepository;
    
    @SneakyThrows
    public void startKvp(String fnr, String begrunnelse) {
        Bruker bruker = Bruker.fraFnr(fnr)
                .medAktoerIdSupplier(() -> aktorService.getAktorId(fnr).orElseThrow(IngenTilgang::new));

        pepClient.sjekkLesetilgangTilBruker(bruker);

        String aktorId = bruker.getAktoerId();
        OppfolgingTable oppfolgingTable = oppfolgingsStatusRepository.fetch(aktorId);

        if (oppfolgingTable == null || !oppfolgingTable.isUnderOppfolging()) {
            throw new UlovligHandling();
        }

        String enhet = getEnhet(fnr);
        if(!pepClient.harTilgangTilEnhet(enhet)) {
            throw new IngenTilgang(String.format("Ingen tilgang til enhet '%s'", enhet));
        }

        if (oppfolgingTable.getGjeldendeKvpId() != 0) {
            throw new Feil(FeilType.UGYLDIG_REQUEST, "Aktøren er allerede under en KVP-periode.");
        }

        String veilederId = SubjectHandler.getIdent().orElseThrow(RuntimeException::new);
        kvpRepository.startKvp(
                aktorId,
                enhet,
                veilederId,
                begrunnelse);

        FunksjonelleMetrikker.startKvp();
    }

    @SneakyThrows
    public void stopKvp(String fnr, String begrunnelse) {
        Bruker bruker = Bruker.fraFnr(fnr)
                .medAktoerIdSupplier(() -> aktorService.getAktorId(fnr).orElseThrow(IngenTilgang::new));

        pepClient.sjekkLesetilgangTilBruker(bruker);
        if(!pepClient.harTilgangTilEnhet(getEnhet(fnr))){
            throw new IngenTilgang();
        }

        stopKvpUtenEnhetSjekk(bruker.getAktoerId(), begrunnelse, NAV);
    }

    public void stopKvpUtenEnhetSjekk(String aktorId, String begrunnelse, KodeverkBruker kodeverkBruker) {
        String veilederId = SubjectHandler.getIdent().orElseThrow(RuntimeException::new);

        OppfolgingTable oppfolgingTable = oppfolgingsStatusRepository.fetch(aktorId);

        long gjeldendeKvp = oppfolgingTable.getGjeldendeKvpId();
        if (gjeldendeKvp == 0) {
            throw new Feil(FeilType.UGYLDIG_REQUEST, "Aktøren har ingen KVP-periode.");
        }

        if(oppfolgingTable.getGjeldendeEskaleringsvarselId() != 0) {
            eskaleringsvarselRepository.finish(
                    aktorId, 
                    oppfolgingTable.getGjeldendeEskaleringsvarselId(), 
                    veilederId, 
                    ESKALERING_AVSLUTTET_FORDI_KVP_BLE_AVSLUTTET);
        }

        kvpRepository.stopKvp(
                gjeldendeKvp,
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
