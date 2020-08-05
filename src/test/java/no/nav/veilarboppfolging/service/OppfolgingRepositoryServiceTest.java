package no.nav.veilarboppfolging.service;

import no.nav.veilarboppfolging.domain.*;
import no.nav.veilarboppfolging.repository.*;
import no.nav.veilarboppfolging.test.IsolatedDatabaseTest;
import org.junit.Before;
import org.junit.Test;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

import static java.lang.System.currentTimeMillis;
import static no.nav.veilarboppfolging.domain.KodeverkBruker.NAV;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OppfolgingRepositoryServiceTest extends IsolatedDatabaseTest {

    private static final String AKTOR_ID = "aktorId";
    private static final String ENHET = "enhet";
    private static final String VEILERDER = "veileder";
    private static final String BEGRUNNELSE = "begrunnelse";
    private static final String OTHER_ENHET = "otherEnhet";

    private AuthService authService = mock(AuthService.class);
    
    private OppfolgingsStatusRepository oppfolgingsStatusRepository;

    private KvpRepository kvpRepository;

    private MaalRepository maalRepository;

    private OppfolgingsPeriodeRepository oppfolgingsPeriodeRepository;

    private ManuellStatusRepository manuellStatusRepository;

    private OppfolgingRepositoryService oppfolgingRepositoryService;
    
    @Before
    public void setup() {
        oppfolgingsStatusRepository = new OppfolgingsStatusRepository(db);

        kvpRepository = new KvpRepository(db);

        maalRepository = new MaalRepository(db);

        oppfolgingsPeriodeRepository = new OppfolgingsPeriodeRepository(db);

        manuellStatusRepository = new ManuellStatusRepository(db);

        oppfolgingRepositoryService = new OppfolgingRepositoryService(
                authService, oppfolgingsStatusRepository, oppfolgingsPeriodeRepository,
                maalRepository, manuellStatusRepository, new EskaleringsvarselRepository(db),
                new KvpRepository(db), new NyeBrukereFeedRepository(db)
        );
    }

    @Test
    public void oppfolging_periode_uten_kvp_perioder() {
        gittOppfolgingForAktor(AKTOR_ID);
        Oppfolging oppfolging = oppfolgingRepositoryService.hentOppfolging(AKTOR_ID).get();

        assertThat(oppfolging.getOppfolgingsperioder().get(0).getKvpPerioder()).hasSize(0);
    }

    @Test
    public void oppfolging_periode_med_kvp_perioder() {
        when(authService.harTilgangTilEnhet(ENHET)).thenReturn(true);

        gittOppfolgingForAktor(AKTOR_ID);
        gitt_kvp_periode(ENHET);
        gitt_kvp_periode(ENHET);
        Oppfolging oppfolging = oppfolgingRepositoryService.hentOppfolging(AKTOR_ID).get();

        assertThat(oppfolging.getOppfolgingsperioder().get(0).getKvpPerioder()).hasSize(2);
    }

    @Test
    public void oppfolging_periode_med_kvp_perioder_bare_tilgang_til_en() {
        when(authService.harTilgangTilEnhet(ENHET)).thenReturn(true);
        when(authService.harTilgangTilEnhet(OTHER_ENHET)).thenReturn(false);

        gittOppfolgingForAktor(AKTOR_ID);
        gitt_kvp_periode(ENHET);
        gitt_kvp_periode(OTHER_ENHET);
        Oppfolging oppfolging = oppfolgingRepositoryService.hentOppfolging(AKTOR_ID).get();

        assertThat(oppfolging.getOppfolgingsperioder().get(0).getKvpPerioder()).hasSize(1);
    }

    @Test
    public void opprettOghentMal() {
        gittOppfolgingForAktor(AKTOR_ID);
        opprettMal(AKTOR_ID, "Dette er et mål");

        MalData mal = getGjeldendeMal(AKTOR_ID);
        assertThat(mal.getAktorId(), equalTo(AKTOR_ID));
        assertThat(mal.getMal(), equalTo("Dette er et mål"));
    }

    @Test
    public void hentMalListe() {
        gittOppfolgingForAktor(AKTOR_ID);
        opprettMal(AKTOR_ID, "Dette er et mål");
        opprettMal(AKTOR_ID, "Dette er et oppdatert mål");

        MalData mal = getGjeldendeMal(AKTOR_ID);
        assertThat(mal.getMal(), equalTo("Dette er et oppdatert mål"));
        List<MalData> malList = hentMal(AKTOR_ID);
        assertThat(malList.size(), greaterThan(1));
    }

    private MalData getGjeldendeMal(String aktorId) {
        return oppfolgingRepositoryService.hentOppfolging(aktorId).get().getGjeldendeMal();
    }

    private List<MalData> hentMal(String aktorId) {
        return maalRepository.aktorMal(aktorId);
    }

    @Test
    public void manglerOppfolging() {
        sjekkAtOppfolgingMangler(hentOppfolging("ukjentAktorId"));
        sjekkAtOppfolgingMangler(hentOppfolging(null));
    }

    @Test
    public void kanHenteForventetOppfolging() {
        oppfolgingsStatusRepository.opprettOppfolging(AKTOR_ID);
        oppfolgingRepositoryService.startOppfolgingHvisIkkeAlleredeStartet(AKTOR_ID);
        Oppfolging uthentetOppfolging = hentOppfolging(AKTOR_ID).get();
        assertThat(uthentetOppfolging.getAktorId(), equalTo(AKTOR_ID));
        assertThat(uthentetOppfolging.isUnderOppfolging(), is(true));
        assertThat(uthentetOppfolging.getOppfolgingsperioder().size(), is(1));
    }

    @Test
    public void avsluttOppfolgingResetterVeileder_Manuellstatus_Mal_Og_Vilkar() {
        oppfolgingsStatusRepository.opprettOppfolging(AKTOR_ID);
        oppfolgingRepositoryService.startOppfolgingHvisIkkeAlleredeStartet(AKTOR_ID);
        String veilederId = "veilederId";
        String maal = "Mål";
        settVeileder(veilederId, AKTOR_ID);
        manuellStatusRepository.create(
                new ManuellStatus()
                        .setAktorId(AKTOR_ID)
                        .setManuell(true)
                        .setDato(new Timestamp(currentTimeMillis()))
                        .setBegrunnelse("Test")
                        .setOpprettetAv(KodeverkBruker.SYSTEM));
        maalRepository.opprett(new MalData().setAktorId(AKTOR_ID).setMal(maal).setEndretAv("bruker").setDato(new Timestamp(currentTimeMillis())));
        Oppfolging oppfolging = hentOppfolging(AKTOR_ID).get();
        assertThat(oppfolging.isUnderOppfolging(), is(true));
        assertThat(oppfolging.getVeilederId(), equalTo(veilederId));
        assertThat(oppfolging.getGjeldendeManuellStatus().isManuell(), is(true));
        assertThat(oppfolging.getGjeldendeMal().getMal(), equalTo(maal));

        oppfolgingsPeriodeRepository.avslutt(AKTOR_ID, veilederId, "Funnet arbeid");
        Oppfolging avsluttetOppfolging = hentOppfolging(AKTOR_ID).get();
        assertThat(avsluttetOppfolging.isUnderOppfolging(), is(false));
        assertThat(avsluttetOppfolging.getVeilederId(), nullValue());
        assertThat(avsluttetOppfolging.getGjeldendeManuellStatus(), nullValue());
        assertThat(avsluttetOppfolging.getGjeldendeMal(), nullValue());

        List<Oppfolgingsperiode> oppfolgingsperioder = avsluttetOppfolging.getOppfolgingsperioder();
        assertThat(oppfolgingsperioder.size(), is(1));
    }


    @Test
    public void kanHenteOppfolgingMedIngenOppfolgingsperioder() {
        oppfolgingsStatusRepository.opprettOppfolging(AKTOR_ID);
        Oppfolging oppfolging = oppfolgingRepositoryService.hentOppfolging(AKTOR_ID).get();
        assertThat(oppfolging.getOppfolgingsperioder(), empty());
    }

    @Test
    public void kanHenteOppfolgingMedOppfolgingsperioder() {
        oppfolgingsStatusRepository.opprettOppfolging(AKTOR_ID);
        oppfolgingRepositoryService.startOppfolgingHvisIkkeAlleredeStartet(AKTOR_ID);
        List<Oppfolgingsperiode> oppfolgingsperioder = oppfolgingRepositoryService.hentOppfolging(AKTOR_ID).get().getOppfolgingsperioder();
        assertThat(oppfolgingsperioder, hasSize(1));
        assertThat(oppfolgingsperioder.get(0).getStartDato(), not(nullValue()));
        assertThat(oppfolgingsperioder.get(0).getSluttDato(), nullValue());

        oppfolgingsPeriodeRepository.avslutt(AKTOR_ID, "veileder", "begrunnelse");
        oppfolgingsperioder = oppfolgingRepositoryService.hentOppfolging(AKTOR_ID).get().getOppfolgingsperioder();
        assertThat(oppfolgingsperioder, hasSize(1));
        assertThat(oppfolgingsperioder.get(0).getSluttDato(), not(nullValue()));

        oppfolgingRepositoryService.startOppfolgingHvisIkkeAlleredeStartet(AKTOR_ID);
        oppfolgingsperioder = oppfolgingRepositoryService.hentOppfolging(AKTOR_ID).get().getOppfolgingsperioder();
        assertThat(oppfolgingsperioder, hasSize(2));
    }


    @Test
    public void utenVeileder() {
        oppfolgingsStatusRepository.opprettOppfolging(AKTOR_ID);
        Oppfolging oppfolging = oppfolgingRepositoryService.hentOppfolging(AKTOR_ID).get();
        assertThat(oppfolging.getVeilederId(), nullValue());
    }

    @Test
    public void medVeilederPaaNyBruker() {
        String veilederId = "veilederId";
        oppfolgingsStatusRepository.opprettOppfolging(AKTOR_ID);
        settVeileder(veilederId, AKTOR_ID);
        Oppfolging oppfolging = oppfolgingRepositoryService.hentOppfolging(AKTOR_ID).get();
        assertThat(oppfolging.getVeilederId(), equalTo(veilederId));
    }


    //Setter veileder direkte vha. sql, siden det ikke finnes funksjonalitet for tildeling av veileder i
    //OppfolgingRepository. Dette finnes kun i OppfolgingFeedRepository (og tilbys i PortefoljeRessurs) p.t.
    //Men siden hentOppfolging henter opp veilder er det likevel aktuelt å teste her at veileder returneres
    //dersom det er satt i databasen. 
    private void settVeileder(String veilederId, String aktorId) {
        db.update("UPDATE OPPFOLGINGSTATUS SET VEILEDER = ? where aktor_id = ?", veilederId, aktorId);
    }

    private Oppfolging gittOppfolgingForAktor(String aktorId) {
        Oppfolging oppfolging = oppfolgingRepositoryService.hentOppfolging(aktorId)
                .orElseGet(() -> oppfolgingsStatusRepository.opprettOppfolging(aktorId));

        oppfolgingRepositoryService.startOppfolgingHvisIkkeAlleredeStartet(aktorId);
        oppfolging.setUnderOppfolging(true);
        return oppfolging;
    }

    private void sjekkAtOppfolgingMangler(Optional<Oppfolging> oppfolging) {
        assertThat(oppfolging.isPresent(), is(false));
    }

    private Optional<Oppfolging> hentOppfolging(String ukjentAktorId) {
        return oppfolgingRepositoryService.hentOppfolging(ukjentAktorId);
    }

    private void opprettMal(String aktorId, String mal) {
        MalData input = new MalData()
                .setAktorId(aktorId)
                .setMal(mal)
                .setEndretAv(aktorId)
                .setDato(new Timestamp(System.currentTimeMillis()));
        maalRepository.opprett(input);
    }

    private void gitt_kvp_periode(String enhet) {
        kvpRepository.startKvp(AKTOR_ID, enhet, VEILERDER, BEGRUNNELSE);
        long kvpId = kvpRepository.gjeldendeKvp(AKTOR_ID);
        kvpRepository.stopKvp(kvpId, AKTOR_ID, VEILERDER, BEGRUNNELSE, NAV);
    }
}