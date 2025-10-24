package no.nav.veilarboppfolging.service;

import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.EnhetId;
import no.nav.common.types.identer.Fnr;
import no.nav.common.types.identer.NavIdent;
import no.nav.pto_schema.enums.arena.Formidlingsgruppe;
import no.nav.pto_schema.enums.arena.Kvalifiseringsgruppe;
import no.nav.veilarboppfolging.client.amtdeltaker.AmtDeltakerClient;
import no.nav.veilarboppfolging.client.digdir_krr.DigdirClient;
import no.nav.veilarboppfolging.domain.Oppfolging;
import no.nav.veilarboppfolging.eventsLogger.BigQueryClient;
import no.nav.veilarboppfolging.oppfolgingsbruker.BrukerRegistrant;
import no.nav.veilarboppfolging.oppfolgingsbruker.arena.ArenaOppfolgingService;
import no.nav.veilarboppfolging.oppfolgingsbruker.inngang.OppfolgingsRegistrering;
import no.nav.veilarboppfolging.oppfolgingsbruker.VeilederRegistrant;
import no.nav.veilarboppfolging.repository.*;
import no.nav.veilarboppfolging.repository.entity.MaalEntity;
import no.nav.veilarboppfolging.repository.entity.ManuellStatusEntity;
import no.nav.veilarboppfolging.repository.entity.OppfolgingsperiodeEntity;
import no.nav.veilarboppfolging.repository.enums.KodeverkBruker;
import no.nav.veilarboppfolging.test.DbTestUtils;
import no.nav.veilarboppfolging.test.IsolatedDatabaseTest;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
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
    private static final NavIdent NAV_IDENT = NavIdent.of("G321312");

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
    private StartOppfolgingService startOppfolgingService;
    private ArenaYtelserService arenaYtelserService = mock(ArenaYtelserService.class);
    private ArenaOppfolgingService arenaOppfolgingService = mock(ArenaOppfolgingService.class);
    private OppfolgingsperiodeEndretService oppfolgingsperiodeEndretService = mock(OppfolgingsperiodeEndretService.class);

    @Before
    public void setup() {
        TransactionTemplate transactor = DbTestUtils.createTransactor(db);
        oppfolgingsStatusRepository = new OppfolgingsStatusRepository(new NamedParameterJdbcTemplate(db));
        kvpRepository = new KvpRepository(db, namedParameterJdbcTemplate, transactor);
        maalRepository = new MaalRepository(db, transactor);
        oppfolgingsPeriodeRepository = new OppfolgingsPeriodeRepository(db, transactor);
        manuellStatusRepository = new ManuellStatusRepository(db, transactor);
        DigdirClient digdirClient = fnr -> Optional.empty();

        ManuellStatusService manuellStatusService = new ManuellStatusService(
                authService,
                manuellStatusRepository,
                null,
                oppfolgingServiceMock,
                oppfolgingsStatusRepository,
                digdirClient,
                null,
                transactor
        );

        oppfolgingService = new OppfolgingService(
                mock(KafkaProducerService.class), null,
                null, authService,
                oppfolgingsStatusRepository, oppfolgingsPeriodeRepository,
                manuellStatusService,
                mock(AmtDeltakerClient.class),
                new KvpRepository(db, namedParameterJdbcTemplate, transactor), maalRepository,
                new BrukerOppslagFlereOppfolgingAktorRepository(db), transactor, arenaYtelserService, mock(BigQueryClient.class), oppfolgingsperiodeEndretService,"https://test.nav.no");

        startOppfolgingService = new StartOppfolgingService(
                manuellStatusService,
                oppfolgingsStatusRepository,
                oppfolgingsPeriodeRepository,
                mock(),
                mock(),
                transactor,
                arenaOppfolgingService,
                "https://test.nav.no"
        );

        when(authService.getFnrOrThrow(AKTOR_ID)).thenReturn(FNR);
    }

    @Test
    public void hentHarFlereAktorIderMedOppfolging_harEnAktorIdMedOppfolging_returnererFalse(){
        var oppfolgingsbruker = OppfolgingsRegistrering.Companion.manuellRegistrering(FNR, AKTOR_ID, new VeilederRegistrant(NavIdent.of("G123123")), null);
        oppfolgingsStatusRepository.opprettOppfolging(AKTOR_ID);
        oppfolgingsPeriodeRepository.start(oppfolgingsbruker);

        when(authService.getAlleAktorIderOrThrow(FNR)).thenReturn(List.of(AKTOR_ID));
        assertFalse(oppfolgingService.hentHarFlereAktorIderMedOppfolging(FNR));
    }

    @Test
    public void hentHarFlereAktorIderMedOppfolging_harFlereAktorIdUtenOppfolging_returnererFalse(){
        var oppfolgingsbruker = OppfolgingsRegistrering.Companion.manuellRegistrering(FNR, AKTOR_ID, new VeilederRegistrant(NAV_IDENT), null);
        oppfolgingsStatusRepository.opprettOppfolging(AKTOR_ID);
        oppfolgingsPeriodeRepository.start(oppfolgingsbruker);

        when(authService.getAlleAktorIderOrThrow(FNR)).thenReturn(List.of(AKTOR_ID, AKTOR_ID2));

        assertFalse(oppfolgingService.hentHarFlereAktorIderMedOppfolging(FNR));
    }

    @Test
    public void hentHarFlereAktorIderMedOppfolging_harFlereAktorIdMedOppf_returnererTrue(){
        var oppfolgingsbruker = OppfolgingsRegistrering.Companion.manuellRegistrering(FNR, AKTOR_ID, new VeilederRegistrant(NAV_IDENT), null);
        oppfolgingsStatusRepository.opprettOppfolging(AKTOR_ID);
        oppfolgingsPeriodeRepository.start(oppfolgingsbruker);

        var oppfolgingsbruker2 = OppfolgingsRegistrering.Companion.manuellRegistrering(FNR, AKTOR_ID2, new VeilederRegistrant(NAV_IDENT), null);
        oppfolgingsStatusRepository.opprettOppfolging(AKTOR_ID2);
        oppfolgingsPeriodeRepository.start(oppfolgingsbruker2);

        when(authService.getAlleAktorIderOrThrow(FNR)).thenReturn(List.of(AKTOR_ID, AKTOR_ID2));

        assertTrue(oppfolgingService.hentHarFlereAktorIderMedOppfolging(FNR));
    }

    @Test
    public void skalIkkeKasteExceptionVedFlereKall(){
        var oppfolgingsbruker = OppfolgingsRegistrering.Companion.manuellRegistrering(FNR, AKTOR_ID, new VeilederRegistrant(NAV_IDENT), null);
        oppfolgingsStatusRepository.opprettOppfolging(AKTOR_ID);
        oppfolgingsPeriodeRepository.start(oppfolgingsbruker);

        var oppfolgingsbruker2 = OppfolgingsRegistrering.Companion.manuellRegistrering(FNR, AKTOR_ID2, new VeilederRegistrant(NAV_IDENT), null);
        oppfolgingsStatusRepository.opprettOppfolging(AKTOR_ID2);
        oppfolgingsPeriodeRepository.start(oppfolgingsbruker2);

        when(authService.getAlleAktorIderOrThrow(FNR)).thenReturn(List.of(AKTOR_ID, AKTOR_ID2));

        assertTrue(oppfolgingService.hentHarFlereAktorIderMedOppfolging(FNR));
        assertTrue(oppfolgingService.hentHarFlereAktorIderMedOppfolging(FNR));
    }

    @Test
    public void oppfolging_periode_uten_kvp_perioder() {
        gittOppfolgingForAktor(AKTOR_ID, FNR);
        Oppfolging oppfolging = oppfolgingService.hentOppfolging(AKTOR_ID).get();

        assertThat(oppfolging.getOppfolgingsperioder().get(0).getKvpPerioder()).hasSize(0);
    }

    @Test
    public void oppfolging_periode_med_kvp_perioder() {
        when(authService.harTilgangTilEnhetMedSperre(ENHET)).thenReturn(true);

        gittOppfolgingForAktor(AKTOR_ID, FNR);
        gitt_kvp_periode(ENHET);
        gitt_kvp_periode(ENHET);
        Oppfolging oppfolging = oppfolgingService.hentOppfolging(AKTOR_ID).get();

        assertThat(oppfolging.getOppfolgingsperioder().get(0).getKvpPerioder()).hasSize(2);
    }

    @Test
    public void oppfolging_periode_med_kvp_perioder_bare_tilgang_til_en() {
        when(authService.harTilgangTilEnhetMedSperre(ENHET)).thenReturn(true);
        when(authService.harTilgangTilEnhetMedSperre(OTHER_ENHET)).thenReturn(false);

        gittOppfolgingForAktor(AKTOR_ID, FNR);
        gitt_kvp_periode(ENHET);
        gitt_kvp_periode(OTHER_ENHET);
        Oppfolging oppfolging = oppfolgingService.hentOppfolging(AKTOR_ID).get();

        assertThat(oppfolging.getOppfolgingsperioder().get(0).getKvpPerioder()).hasSize(1);
    }

    @Test
    public void opprettOghentMal() {
        gittOppfolgingForAktor(AKTOR_ID, FNR);
        opprettMal(AKTOR_ID, "Dette er et mål");

        MaalEntity mal = getGjeldendeMal(AKTOR_ID);
        assertThat(mal.getAktorId(), equalTo(AKTOR_ID.get()));
        assertThat(mal.getMal(), equalTo("Dette er et mål"));
    }

    @Test
    public void hentMalListe() {
        gittOppfolgingForAktor(AKTOR_ID, FNR);
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
        startOppfolgingService.startOppfolgingHvisIkkeAlleredeStartet(OppfolgingsRegistrering.Companion.arbeidssokerRegistrering(FNR, AKTOR_ID, new VeilederRegistrant(NAV_IDENT)));
        Oppfolging uthentetOppfolging = hentOppfolging(AKTOR_ID).get();
        assertThat(uthentetOppfolging.getAktorId(), equalTo(AKTOR_ID.get()));
        assertThat(uthentetOppfolging.isUnderOppfolging(), is(true));
        assertThat(uthentetOppfolging.getOppfolgingsperioder().size(), is(1));
    }

    @Test
    public void avsluttOppfolgingResetterVeileder_Manuellstatus_Mal_Og_Vilkar() {
        oppfolgingsStatusRepository.opprettOppfolging(AKTOR_ID);
        startOppfolgingService.startOppfolgingHvisIkkeAlleredeStartet(OppfolgingsRegistrering.Companion.manuellRegistrering(FNR, AKTOR_ID, new VeilederRegistrant(NAV_IDENT), null));
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

        oppfolgingsPeriodeRepository.avsluttSistePeriodeOgAvsluttOppfolging(AKTOR_ID, veilederId, "Funnet arbeid");
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
        startOppfolgingService.startOppfolgingHvisIkkeAlleredeStartet(OppfolgingsRegistrering.Companion.arenaSyncOppfolgingBrukerRegistrering(FNR, AKTOR_ID, Formidlingsgruppe.IARBS, Kvalifiseringsgruppe.IKVAL, EnhetId.of("1131")));
        List<OppfolgingsperiodeEntity> oppfolgingsperioder = oppfolgingService.hentOppfolging(AKTOR_ID).get().getOppfolgingsperioder();
        assertThat(oppfolgingsperioder, hasSize(1));
        assertThat(oppfolgingsperioder.get(0).getStartDato(), not(nullValue()));
        assertThat(oppfolgingsperioder.get(0).getSluttDato(), nullValue());

        oppfolgingsPeriodeRepository.avsluttSistePeriodeOgAvsluttOppfolging(AKTOR_ID, "veileder", "begrunnelse");
        oppfolgingsperioder = oppfolgingService.hentOppfolging(AKTOR_ID).get().getOppfolgingsperioder();
        assertThat(oppfolgingsperioder, hasSize(1));
        assertThat(oppfolgingsperioder.get(0).getSluttDato(), not(nullValue()));

        startOppfolgingService.startOppfolgingHvisIkkeAlleredeStartet(OppfolgingsRegistrering.Companion.arenaSyncOppfolgingBrukerRegistrering(FNR, AKTOR_ID, Formidlingsgruppe.IARBS, Kvalifiseringsgruppe.BATT, EnhetId.of("3131")));
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
    //OppfolgingRepository.
    //Men siden hentOppfolging henter opp veilder er det likevel aktuelt å teste her at veileder returneres
    //dersom det er satt i databasen.
    private void settVeileder(String veilederId, AktorId aktorId) {
        db.update("UPDATE OPPFOLGINGSTATUS SET VEILEDER = ? where aktor_id = ?", veilederId, aktorId.get());
    }

    private Oppfolging gittOppfolgingForAktor(AktorId aktorId, Fnr fnr) {
        Oppfolging oppfolging = oppfolgingService.hentOppfolging(aktorId)
                .orElseGet(() -> oppfolgingsStatusRepository.opprettOppfolging(aktorId));

        startOppfolgingService.startOppfolgingHvisIkkeAlleredeStartet(OppfolgingsRegistrering.Companion.arbeidssokerRegistrering(fnr, aktorId, new BrukerRegistrant(fnr)));
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
