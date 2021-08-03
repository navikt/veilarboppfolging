package no.nav.veilarboppfolging.service;

import no.nav.common.health.HealthCheckResult;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.veilarboppfolging.client.dkif.DkifClient;
import no.nav.veilarboppfolging.client.dkif.DkifKontaktinfo;
import no.nav.veilarboppfolging.domain.Oppfolging;
import no.nav.veilarboppfolging.repository.*;
import no.nav.veilarboppfolging.repository.entity.MaalEntity;
import no.nav.veilarboppfolging.repository.entity.ManuellStatusEntity;
import no.nav.veilarboppfolging.repository.entity.OppfolgingsperiodeEntity;
import no.nav.veilarboppfolging.repository.enums.KodeverkBruker;
import no.nav.veilarboppfolging.test.DbTestUtils;
import no.nav.veilarboppfolging.test.IsolatedDatabaseTest;
import org.junit.Before;
import org.junit.Test;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static no.nav.veilarboppfolging.repository.enums.KodeverkBruker.NAV;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Dette lå orginalt i en annen service, men har blitt merget inn med OppfolgingService.
 * Det er kun testene fra den andre servicen som har blitt merget inn som blir testet her, resten av testene blir gjort i OppfolgingServiceTest.
 */
public class OppfolgingServiceTest2 extends IsolatedDatabaseTest {

    private static final AktorId AKTOR_ID = AktorId.of("aktorId");
    private static final AktorId AKTOR_ID2 = AktorId.of("2312321");

    private static final String ENHET = "enhet";
    private static final String VEILERDER = "veileder";
    private static final String BEGRUNNELSE = "begrunnelse";
    private static final String OTHER_ENHET = "otherEnhet";
    private static final Fnr FNR = Fnr.of("21432432423");

    private AuthService authService = mock(AuthService.class);
    
    private OppfolgingsStatusRepository oppfolgingsStatusRepository;

    private KvpRepository kvpRepository;

    private MaalRepository maalRepository;

    private OppfolgingsPeriodeRepository oppfolgingsPeriodeRepository;

    private ManuellStatusRepository manuellStatusRepository;

    private OppfolgingService oppfolgingService;

    private OppfolgingService oppfolgingServiceMock = mock(OppfolgingService.class);

    @Before
    public void setup() {
        TransactionTemplate transactor = DbTestUtils.createTransactor(db);

        oppfolgingsStatusRepository = new OppfolgingsStatusRepository(db);

        kvpRepository = new KvpRepository(db, transactor);

        maalRepository = new MaalRepository(db, transactor);

        oppfolgingsPeriodeRepository = new OppfolgingsPeriodeRepository(db, transactor);

        manuellStatusRepository = new ManuellStatusRepository(db, transactor);

        DkifClient dkifClient = new DkifClient() {
            @Override
            public Optional<DkifKontaktinfo> hentKontaktInfo(Fnr fnr) {
                return Optional.empty();
            }

            @Override
            public HealthCheckResult checkHealth() {
                return null;
            }
        };

        ManuellStatusService manuellStatusService = new ManuellStatusService(
                authService,
                manuellStatusRepository,
                null,
                oppfolgingServiceMock,
                dkifClient,
                null,
                transactor
        );

        oppfolgingService = new OppfolgingService(
                mock(KafkaProducerService.class), null,
                null, null, authService,
                oppfolgingsStatusRepository, oppfolgingsPeriodeRepository,
                manuellStatusService,
                null, new EskaleringsvarselRepository(db, transactor),
                new KvpRepository(db, transactor), new NyeBrukereFeedRepository(db), maalRepository,
                new BrukerOppslagFlereOppfolgingAktorRepository(db), transactor);

        when(authService.getFnrOrThrow(AKTOR_ID)).thenReturn(FNR);
    }

    @Test
    public void hentHarFlereAktorIderMedOppfolging_harEnAktorIdMedOppfolging_returnererFalse(){
        oppfolgingsStatusRepository.opprettOppfolging(AKTOR_ID);
        oppfolgingsPeriodeRepository.start(AKTOR_ID);

        when(authService.getAlleAktorIderOrThrow(FNR)).thenReturn(List.of(AKTOR_ID));
        assertFalse(oppfolgingService.hentHarFlereAktorIderMedOppfolging(FNR));
    }

    @Test
    public void hentHarFlereAktorIderMedOppfolging_harFlereAktorIdUtenOppfolging_returnererFalse(){
        oppfolgingsStatusRepository.opprettOppfolging(AKTOR_ID);
        oppfolgingsPeriodeRepository.start(AKTOR_ID);

        when(authService.getAlleAktorIderOrThrow(FNR)).thenReturn(List.of(AKTOR_ID, AKTOR_ID2));

        assertFalse(oppfolgingService.hentHarFlereAktorIderMedOppfolging(FNR));
    }

    @Test
    public void hentHarFlereAktorIderMedOppfolging_harFlereAktorIdMedOppf_returnererTrue(){
        oppfolgingsStatusRepository.opprettOppfolging(AKTOR_ID);
        oppfolgingsPeriodeRepository.start(AKTOR_ID);

        oppfolgingsStatusRepository.opprettOppfolging(AKTOR_ID2);
        oppfolgingsPeriodeRepository.start(AKTOR_ID2);

        when(authService.getAlleAktorIderOrThrow(FNR)).thenReturn(List.of(AKTOR_ID, AKTOR_ID2));

        assertTrue(oppfolgingService.hentHarFlereAktorIderMedOppfolging(FNR));
    }

    @Test
    public void skalIkkeKasteExceptionVedFlereKall(){
        oppfolgingsStatusRepository.opprettOppfolging(AKTOR_ID);
        oppfolgingsPeriodeRepository.start(AKTOR_ID);

        oppfolgingsStatusRepository.opprettOppfolging(AKTOR_ID2);
        oppfolgingsPeriodeRepository.start(AKTOR_ID2);

        when(authService.getAlleAktorIderOrThrow(FNR)).thenReturn(List.of(AKTOR_ID, AKTOR_ID2));

        assertTrue(oppfolgingService.hentHarFlereAktorIderMedOppfolging(FNR));
        assertTrue(oppfolgingService.hentHarFlereAktorIderMedOppfolging(FNR));
    }

    @Test
    public void oppfolging_periode_uten_kvp_perioder() {
        gittOppfolgingForAktor(AKTOR_ID);
        Oppfolging oppfolging = oppfolgingService.hentOppfolging(AKTOR_ID).get();

        assertThat(oppfolging.getOppfolgingsperioder().get(0).getKvpPerioder()).hasSize(0);
    }

    @Test
    public void oppfolging_periode_med_kvp_perioder() {
        when(authService.harTilgangTilEnhetMedSperre(ENHET)).thenReturn(true);

        gittOppfolgingForAktor(AKTOR_ID);
        gitt_kvp_periode(ENHET);
        gitt_kvp_periode(ENHET);
        Oppfolging oppfolging = oppfolgingService.hentOppfolging(AKTOR_ID).get();

        assertThat(oppfolging.getOppfolgingsperioder().get(0).getKvpPerioder()).hasSize(2);
    }

    @Test
    public void oppfolging_periode_med_kvp_perioder_bare_tilgang_til_en() {
        when(authService.harTilgangTilEnhetMedSperre(ENHET)).thenReturn(true);
        when(authService.harTilgangTilEnhetMedSperre(OTHER_ENHET)).thenReturn(false);

        gittOppfolgingForAktor(AKTOR_ID);
        gitt_kvp_periode(ENHET);
        gitt_kvp_periode(OTHER_ENHET);
        Oppfolging oppfolging = oppfolgingService.hentOppfolging(AKTOR_ID).get();

        assertThat(oppfolging.getOppfolgingsperioder().get(0).getKvpPerioder()).hasSize(1);
    }

    @Test
    public void opprettOghentMal() {
        gittOppfolgingForAktor(AKTOR_ID);
        opprettMal(AKTOR_ID, "Dette er et mål");

        MaalEntity mal = getGjeldendeMal(AKTOR_ID);
        assertThat(mal.getAktorId(), equalTo(AKTOR_ID.get()));
        assertThat(mal.getMal(), equalTo("Dette er et mål"));
    }

    @Test
    public void hentMalListe() {
        gittOppfolgingForAktor(AKTOR_ID);
        opprettMal(AKTOR_ID, "Dette er et mål");
        opprettMal(AKTOR_ID, "Dette er et oppdatert mål");

        MaalEntity mal = getGjeldendeMal(AKTOR_ID);
        assertThat(mal.getMal(), equalTo("Dette er et oppdatert mål"));
        List<MaalEntity> malList = hentMal(AKTOR_ID);
        assertThat(malList.size(), greaterThan(1));
    }

    private MaalEntity getGjeldendeMal(AktorId aktorId) {
        return oppfolgingService.hentOppfolging(aktorId).get().getGjeldendeMal();
    }

    private List<MaalEntity> hentMal(AktorId aktorId) {
        return maalRepository.aktorMal(aktorId);
    }

    @Test
    public void manglerOppfolging() {
        sjekkAtOppfolgingMangler(hentOppfolging(AktorId.of("ukjentAktorId")));
    }

    @Test
    public void kanHenteForventetOppfolging() {
        oppfolgingsStatusRepository.opprettOppfolging(AKTOR_ID);
        oppfolgingService.startOppfolgingHvisIkkeAlleredeStartet(AKTOR_ID);
        Oppfolging uthentetOppfolging = hentOppfolging(AKTOR_ID).get();
        assertThat(uthentetOppfolging.getAktorId(), equalTo(AKTOR_ID.get()));
        assertThat(uthentetOppfolging.isUnderOppfolging(), is(true));
        assertThat(uthentetOppfolging.getOppfolgingsperioder().size(), is(1));
    }

    @Test
    public void avsluttOppfolgingResetterVeileder_Manuellstatus_Mal_Og_Vilkar() {
        oppfolgingsStatusRepository.opprettOppfolging(AKTOR_ID);
        oppfolgingService.startOppfolgingHvisIkkeAlleredeStartet(AKTOR_ID);
        String veilederId = "veilederId";
        String maal = "Mål";
        settVeileder(veilederId, AKTOR_ID);
        manuellStatusRepository.create(
                new ManuellStatusEntity()
                        .setAktorId(AKTOR_ID.get())
                        .setManuell(true)
                        .setDato(ZonedDateTime.now())
                        .setBegrunnelse("Test")
                        .setOpprettetAv(KodeverkBruker.SYSTEM));
        
        maalRepository.opprett(new MaalEntity().setAktorId(AKTOR_ID.get()).setMal(maal).setEndretAv("bruker").setDato(ZonedDateTime.now()));
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

        List<OppfolgingsperiodeEntity> oppfolgingsperioder = avsluttetOppfolging.getOppfolgingsperioder();
        assertThat(oppfolgingsperioder.size(), is(1));
    }


    @Test
    public void kanHenteOppfolgingMedIngenOppfolgingsperioder() {
        oppfolgingsStatusRepository.opprettOppfolging(AKTOR_ID);
        Oppfolging oppfolging = oppfolgingService.hentOppfolging(AKTOR_ID).get();
        assertThat(oppfolging.getOppfolgingsperioder(), empty());
    }

    @Test
    public void kanHenteOppfolgingMedOppfolgingsperioder() {
        oppfolgingsStatusRepository.opprettOppfolging(AKTOR_ID);
        oppfolgingService.startOppfolgingHvisIkkeAlleredeStartet(AKTOR_ID);
        List<OppfolgingsperiodeEntity> oppfolgingsperioder = oppfolgingService.hentOppfolging(AKTOR_ID).get().getOppfolgingsperioder();
        assertThat(oppfolgingsperioder, hasSize(1));
        assertThat(oppfolgingsperioder.get(0).getStartDato(), not(nullValue()));
        assertThat(oppfolgingsperioder.get(0).getSluttDato(), nullValue());

        oppfolgingsPeriodeRepository.avslutt(AKTOR_ID, "veileder", "begrunnelse");
        oppfolgingsperioder = oppfolgingService.hentOppfolging(AKTOR_ID).get().getOppfolgingsperioder();
        assertThat(oppfolgingsperioder, hasSize(1));
        assertThat(oppfolgingsperioder.get(0).getSluttDato(), not(nullValue()));

        oppfolgingService.startOppfolgingHvisIkkeAlleredeStartet(AKTOR_ID);
        oppfolgingsperioder = oppfolgingService.hentOppfolging(AKTOR_ID).get().getOppfolgingsperioder();
        assertThat(oppfolgingsperioder, hasSize(2));
    }


    @Test
    public void utenVeileder() {
        oppfolgingsStatusRepository.opprettOppfolging(AKTOR_ID);
        Oppfolging oppfolging = oppfolgingService.hentOppfolging(AKTOR_ID).get();
        assertThat(oppfolging.getVeilederId(), nullValue());
    }

    @Test
    public void medVeilederPaaNyBruker() {
        String veilederId = "veilederId";
        oppfolgingsStatusRepository.opprettOppfolging(AKTOR_ID);
        settVeileder(veilederId, AKTOR_ID);
        Oppfolging oppfolging = oppfolgingService.hentOppfolging(AKTOR_ID).get();
        assertThat(oppfolging.getVeilederId(), equalTo(veilederId));
    }


    //Setter veileder direkte vha. sql, siden det ikke finnes funksjonalitet for tildeling av veileder i
    //OppfolgingRepository. Dette finnes kun i OppfolgingFeedRepository (og tilbys i PortefoljeRessurs) p.t.
    //Men siden hentOppfolging henter opp veilder er det likevel aktuelt å teste her at veileder returneres
    //dersom det er satt i databasen. 
    private void settVeileder(String veilederId, AktorId aktorId) {
        db.update("UPDATE OPPFOLGINGSTATUS SET VEILEDER = ? where aktor_id = ?", veilederId, aktorId.get());
    }

    private Oppfolging gittOppfolgingForAktor(AktorId aktorId) {
        Oppfolging oppfolging = oppfolgingService.hentOppfolging(aktorId)
                .orElseGet(() -> oppfolgingsStatusRepository.opprettOppfolging(aktorId));

        oppfolgingService.startOppfolgingHvisIkkeAlleredeStartet(aktorId);
        oppfolging.setUnderOppfolging(true);
        return oppfolging;
    }

    private void sjekkAtOppfolgingMangler(Optional<Oppfolging> oppfolging) {
        assertThat(oppfolging.isPresent(), is(false));
    }

    private Optional<Oppfolging> hentOppfolging(AktorId ukjentAktorId) {
        return oppfolgingService.hentOppfolging(ukjentAktorId);
    }

    private void opprettMal(AktorId aktorId, String mal) {
        MaalEntity input = new MaalEntity()
                .setAktorId(aktorId.get())
                .setMal(mal)
                .setEndretAv(aktorId.get())
                .setDato(ZonedDateTime.now());

        maalRepository.opprett(input);
    }

    private void gitt_kvp_periode(String enhet) {
        kvpRepository.startKvp(AKTOR_ID, enhet, VEILERDER, BEGRUNNELSE, ZonedDateTime.now());
        long kvpId = kvpRepository.gjeldendeKvp(AKTOR_ID);
        kvpRepository.stopKvp(kvpId, AKTOR_ID, VEILERDER, BEGRUNNELSE, NAV, ZonedDateTime.now());
    }
}
