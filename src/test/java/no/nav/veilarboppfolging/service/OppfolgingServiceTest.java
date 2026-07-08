package no.nav.veilarboppfolging.service;

import no.nav.common.client.aktorregister.IngenGjeldendeIdentException;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.EnhetId;
import no.nav.common.types.identer.Fnr;
import no.nav.common.types.identer.NavIdent;
import no.nav.pto_schema.enums.arena.Formidlingsgruppe;
import no.nav.pto_schema.enums.arena.Kvalifiseringsgruppe;
import no.nav.veilarboppfolging.ForbiddenException;
import no.nav.veilarboppfolging.client.digdir_krr.KRRData;
import no.nav.veilarboppfolging.client.tiltakshistorikk.TiltakshistorikkClient;
import no.nav.veilarboppfolging.client.ungdomsprogram.UngdomsprogramClient;
import no.nav.veilarboppfolging.client.arbeidssoekerregisteret.ArbeidssoekerregisteretClient;
import no.nav.veilarboppfolging.client.aap.AapClient;
import no.nav.veilarboppfolging.client.veilarbarena.ArenaOppfolgingTilstand;
import no.nav.veilarboppfolging.client.veilarbarena.VeilarbArenaOppfolgingsStatus;
import no.nav.veilarboppfolging.controller.response.UnderOppfolgingDTO;
import no.nav.veilarboppfolging.controller.response.VeilederTilgang;
import no.nav.veilarboppfolging.domain.AvslutningStatusData;
import no.nav.veilarboppfolging.domain.OppfolgingStatusData;
import no.nav.veilarboppfolging.eventsLogger.BigQueryClient;
import no.nav.veilarboppfolging.kafka.dto.OppfolgingsperiodeDTO;
import no.nav.veilarboppfolging.kandidatForUtmelding.KandidatForUtmeldingRepository;
import no.nav.veilarboppfolging.kandidatForUtmelding.KandidatForUtmeldingService;
import no.nav.veilarboppfolging.oppfolgingsbruker.BrukerRegistrant;
import no.nav.veilarboppfolging.oppfolgingsbruker.VeilederRegistrant;
import no.nav.veilarboppfolging.oppfolgingsbruker.arena.ArenaOppfolgingService;
import no.nav.veilarboppfolging.oppfolgingsbruker.arena.ArenaOppfolgingTilstandOppslagResult;
import no.nav.veilarboppfolging.oppfolgingsbruker.inngang.OppfolgingsRegistrering;
import no.nav.veilarboppfolging.oppfolgingsbruker.utgang.*;
import no.nav.veilarboppfolging.repository.*;
import no.nav.veilarboppfolging.repository.entity.OppfolgingsperiodeEntity;
import no.nav.veilarboppfolging.test.DbTestUtils;
import no.nav.veilarboppfolging.test.IsolatedDatabaseTest;
import no.nav.veilarboppfolging.utils.OppfolgingsperiodeUtils;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.Assert.*;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class OppfolgingServiceTest extends IsolatedDatabaseTest {

    private Fnr fnr;
    private AktorId aktorId;
    private static final String ENHET = "5563";
    private static final String VEILEDER = "veileder";
    private static final String BEGRUNNELSE = "begrunnelse";
    private static final NavIdent NAV_IDENT = NavIdent.of("Z1234");

    private ArenaOppfolgingTilstand arenaOppfolgingTilstand;
    private VeilarbArenaOppfolgingsStatus arenaOppfolgingStatus;

    private AuthService authService = mock(AuthService.class);
    private KafkaProducerService kafkaProducerService = mock(KafkaProducerService.class);
    private ArenaOppfolgingService arenaOppfolgingService = mock(ArenaOppfolgingService.class);
    private KvpService kvpService = mock(KvpService.class);
    private KvpRepository kvpRepository = mock(KvpRepository.class);
    private ManuellStatusService manuellStatusService = mock(ManuellStatusService.class);
    private TiltakshistorikkClient tiltakshistorikkClient = mock(TiltakshistorikkClient.class);
    private UngdomsprogramClient ungdomsprogramClient = mock(UngdomsprogramClient.class);
    private ArbeidssoekerregisteretClient arbeidssoekerregisteretClient = mock(ArbeidssoekerregisteretClient.class);
    private AapClient aapClient = mock(AapClient.class);
    private OppfolgingsStatusRepository oppfolgingsStatusRepository;
    private OppfolgingsPeriodeRepository oppfolgingsPeriodeRepository;
    private OppfolgingService oppfolgingService;
    private AvsluttOppfolgingService avsluttOppfolgingService;
    private StartOppfolgingService startOppfolgingService;
    private ArbeidsoppfolgingskontorRepository arbeidsoppfolgingskontorRepository;
    private BigQueryClient bigQueryClient = mock(BigQueryClient.class);
    private ArbeidsoppfolgingsKontorService arbeidsoppfolgingsKontorService = mock(ArbeidsoppfolgingsKontorService.class);
    private KandidatForUtmeldingService kandidatForUtmeldingService = mock(KandidatForUtmeldingService.class);
    private KandidatForUtmeldingRepository kandidatForUtmeldingRepository;

    @Before
    public void setup() {
        TransactionTemplate transactor = DbTestUtils.createTransactor(db);
        fnr = Fnr.of(randomString(11));
        aktorId = AktorId.of(randomString(13));

        arenaOppfolgingTilstand = new ArenaOppfolgingTilstand(null, null, null);
        arenaOppfolgingStatus = new VeilarbArenaOppfolgingsStatus(null, null, null, null, null, null);
        oppfolgingsStatusRepository = new OppfolgingsStatusRepository(new NamedParameterJdbcTemplate(db));
        oppfolgingsPeriodeRepository = new OppfolgingsPeriodeRepository(db, transactor);
        arbeidsoppfolgingskontorRepository = new ArbeidsoppfolgingskontorRepository(new NamedParameterJdbcTemplate(db));
        kandidatForUtmeldingRepository = new KandidatForUtmeldingRepository(new NamedParameterJdbcTemplate(db));

        avsluttOppfolgingService = new AvsluttOppfolgingService(
                authService,
                oppfolgingsStatusRepository,
                oppfolgingsPeriodeRepository,
                arenaOppfolgingService,
                kafkaProducerService,
                kvpService,
                tiltakshistorikkClient,
                ungdomsprogramClient,
                aapClient,
                arbeidssoekerregisteretClient,
                bigQueryClient,
                transactor,
                arbeidsoppfolgingskontorRepository,
                kandidatForUtmeldingRepository
        );
        oppfolgingService = new OppfolgingService(
                kvpService,
                arenaOppfolgingService,
                authService,
                oppfolgingsStatusRepository,
                oppfolgingsPeriodeRepository,
                manuellStatusService,
                kvpRepository,
                new MaalRepository(db, transactor),
                new BrukerOppslagFlereOppfolgingAktorRepository(db),
                arbeidsoppfolgingsKontorService,
                tiltakshistorikkClient);

        startOppfolgingService = new StartOppfolgingService(
                manuellStatusService,
                oppfolgingsStatusRepository,
                oppfolgingsPeriodeRepository,
                kafkaProducerService,
                kandidatForUtmeldingService,
                bigQueryClient,
                transactor,
                "https://test.nav.no"
                );


        gittArenaOppfolgingStatus("", "");

        when(authService.getAktorIdOrThrow(fnr)).thenReturn(aktorId);
        when(authService.getFnrOrThrow(aktorId)).thenReturn(fnr);
        stubArenaTilstand();
        stubArenaStatus();
        when(arbeidsoppfolgingsKontorService.hentOppfolgingsEnhetId(fnr)).thenReturn(EnhetId.of(ENHET));
        when(manuellStatusService.hentDigdirKontaktinfo(fnr)).thenReturn(new KRRData(false, fnr.get(), false, false));
        when(tiltakshistorikkClient.harAktiveTiltaksdeltakelser(fnr.get())).thenReturn(false);
    }

    @Test
    public void skal_publisere_paa_kafka_ved_start_paa_oppfolging() {
        settTilstandFormidlingsgruppe("IARBS");
        oppfolgingsStatusRepository.opprettOppfolging(aktorId);
        assertTrue(oppfolgingsPeriodeRepository.hentOppfolgingsperioder(aktorId).isEmpty());

        startOppfolgingService.startOppfolgingHvisIkkeAlleredeStartet(OppfolgingsRegistrering.Companion.arenaSyncOppfolgingBrukerRegistrering(fnr, aktorId, Formidlingsgruppe.IARBS, Kvalifiseringsgruppe.BATT));
        assertUnderOppfolgingLagret(aktorId);

        List<OppfolgingsperiodeEntity> oppfolgingsperioder = oppfolgingsPeriodeRepository.hentOppfolgingsperioder(aktorId);
        assertEquals(1, oppfolgingsperioder.size());
        verify(kafkaProducerService, times(1)).publiserOppfolgingsperiode(any(OppfolgingsperiodeDTO.class));
        verify(kafkaProducerService).publiserVisAoMinSideMicrofrontend(aktorId, fnr);
        verify(kafkaProducerService, times(1)).publiserMinSideBeskjed(any(Fnr.class), anyString(), anyString());
    }

    @Test
    public void skal_publisere_paa_kafka_ved_avsluttet_oppfolging() {
        startOppfolgingForBruker();

        settTilstandFormidlingsgruppe("ISERV");

        reset(kafkaProducerService);
        avsluttOppfolgingService.avsluttOppfolgingHvisKanAvsluttes(new ManuellAvregistrering(aktorId, new VeilederRegistrant(new NavIdent(VEILEDER)), ""));

        verify(kafkaProducerService).publiserOppfolgingsperiode(any(OppfolgingsperiodeDTO.class));
        verify(kafkaProducerService).publiserVeilederTilordnet(aktorId, null, null);
        verify(kafkaProducerService).publiserEndringPaNyForVeileder(aktorId, false);
        verify(kafkaProducerService).publiserEndringPaManuellStatus(aktorId, false);
        verify(kafkaProducerService).publiserSkjulAoMinSideMicrofrontend(aktorId, fnr);
//        verify(oppfolgingsperiodeEndretService).håndterOppfolgingAvsluttet(any(OppfolgingsperiodeEntity.class)); // TODO I en overgangsperiode lytter vi heller på tombstone fra ao-oppfolgingskontor
    }

    @Test
    public void adminAvsluttSpesifikkOppfolgingsperiode_UuidErNull_LoggWarning() {
        startOppfolgingForBruker();
        reset(kafkaProducerService);

        avsluttOppfolgingService.adminAvsluttSpesifikkOppfolgingsperiode(aktorId, VEILEDER, "en begrunnelse", null);

        UnderOppfolgingDTO underOppfolgingDTO2 = oppfolgingService.oppfolgingData(fnr);
        Assertions.assertThat(underOppfolgingDTO2.getUnderOppfolging()).isTrue();
        verify(kafkaProducerService, never()).publiserOppfolgingsperiode(any(OppfolgingsperiodeDTO.class));
    }


    @Test
    public void adminAvsluttSpesifikkOppfolgingsperiode_ValgtPeriodeFinnesIkke_LoggWarning() {
        startOppfolgingForBruker();
        reset(kafkaProducerService);
        var uuidSomIkkeFinnes = "beb16ce1-b2a7-4682-87dc-6bd97f43b9b6";

        avsluttOppfolgingService.adminAvsluttSpesifikkOppfolgingsperiode(aktorId, VEILEDER, "en begrunnelse", uuidSomIkkeFinnes);

        UnderOppfolgingDTO underOppfolgingDTO2 = oppfolgingService.oppfolgingData(fnr);
        Assertions.assertThat(underOppfolgingDTO2.getUnderOppfolging()).isTrue();
        verify(kafkaProducerService, never()).publiserOppfolgingsperiode(any(OppfolgingsperiodeDTO.class));
    }

    @Test
    public void adminAvsluttSpesifikkOppfolgingsperiode_ValgtPeriodeErAlleredeAvsluttet_IkkeAvsluttOgLoggWarning() {
        startOppfolgingForBruker();
        reset(kafkaProducerService);
        var perioder = oppfolgingsPeriodeRepository.hentOppfolgingsperioder(aktorId).stream().sorted(Comparator.comparing(OppfolgingsperiodeEntity::getStartDato)).toList();
        var uuidSomSkalAvsluttes = perioder.getFirst().getUuid();
        var sistePeriode = perioder.getLast();
        oppfolgingsPeriodeRepository.avsluttOppfolgingsperiode(uuidSomSkalAvsluttes, VEILEDER, "en begrunnelse", sistePeriode.getStartDato(), AvregistreringsType.AdminAvregistrering);

        avsluttOppfolgingService.adminAvsluttSpesifikkOppfolgingsperiode(aktorId, VEILEDER, "en begrunnelse", uuidSomSkalAvsluttes.toString());

        UnderOppfolgingDTO underOppfolgingDTO2 = oppfolgingService.oppfolgingData(fnr);
        Assertions.assertThat(underOppfolgingDTO2.getUnderOppfolging()).isTrue();
        verify(kafkaProducerService, never()).publiserOppfolgingsperiode(any(OppfolgingsperiodeDTO.class));
    }

    @Test
    public void adminAvsluttSpesifikkOppfolgingsperiode_ValgtPeriodeErSisteOgEneste_AvsluttOppfolging() {
        startOppfolgingForBruker();
        reset(kafkaProducerService);

        var perioder = oppfolgingsPeriodeRepository.hentOppfolgingsperioder(aktorId).stream().sorted(Comparator.comparing(OppfolgingsperiodeEntity::getStartDato)).toList();
        Assertions.assertThat(perioder.size()).isEqualTo(1);
        var uuidSomSkalAvsluttes = perioder.getFirst().getUuid();

        avsluttOppfolgingService.adminAvsluttSpesifikkOppfolgingsperiode(aktorId, VEILEDER, "en begrunnelse", uuidSomSkalAvsluttes.toString());

        UnderOppfolgingDTO underOppfolgingDTO2 = oppfolgingService.oppfolgingData(fnr);
        Assertions.assertThat(underOppfolgingDTO2.getUnderOppfolging()).isFalse();
        verify(kafkaProducerService).publiserOppfolgingsperiode(any(OppfolgingsperiodeDTO.class));
        verify(kafkaProducerService).publiserVeilederTilordnet(aktorId, null, null);
        verify(kafkaProducerService).publiserEndringPaNyForVeileder(aktorId, false);
        verify(kafkaProducerService).publiserEndringPaManuellStatus(aktorId, false);
        verify(kafkaProducerService).publiserSkjulAoMinSideMicrofrontend(aktorId, fnr);
    }

    @Test
    public void adminAvsluttSpesifikkOppfolgingsperiode_FlerePerioderMenKunEnErAktiv_AvsluttOppfolging() {
        startOppfolgingForBruker();
        reset(kafkaProducerService);

        oppfolgingsPeriodeRepository.avsluttSistePeriodeOgAvsluttOppfolging(aktorId, "veilederid", "begrunnelse", AvregistreringsType.ArenaIservKanIkkeReaktiveres);
        var oppfolgingsbruker = OppfolgingsRegistrering.Companion.arbeidssokerRegistrering(fnr, aktorId, new VeilederRegistrant(NAV_IDENT));
        oppfolgingsPeriodeRepository.start(oppfolgingsbruker);
        var perioder = oppfolgingsPeriodeRepository.hentOppfolgingsperioder(aktorId).stream().sorted(Comparator.comparing(OppfolgingsperiodeEntity::getStartDato)).toList();
        Assertions.assertThat(perioder.size()).isEqualTo(2);
        var uuidSomSkalAvsluttes = perioder.getLast().getUuid();

        avsluttOppfolgingService.adminAvsluttSpesifikkOppfolgingsperiode(aktorId, VEILEDER, "en begrunnelse", uuidSomSkalAvsluttes.toString());

        UnderOppfolgingDTO underOppfolgingDTO2 = oppfolgingService.oppfolgingData(fnr);
        Assertions.assertThat(underOppfolgingDTO2.getUnderOppfolging()).isFalse();
        verify(kafkaProducerService).publiserOppfolgingsperiode(any(OppfolgingsperiodeDTO.class));
        verify(kafkaProducerService).publiserVeilederTilordnet(aktorId, null, null);
        verify(kafkaProducerService).publiserEndringPaNyForVeileder(aktorId, false);
        verify(kafkaProducerService).publiserEndringPaManuellStatus(aktorId, false);
        verify(kafkaProducerService).publiserSkjulAoMinSideMicrofrontend(aktorId, fnr);
    }

    @Test
    public void adminAvsluttSpesifikkOppfolgingsperiode_PeriodeErSisteOgEnesteOgBrukerManglerFnr_AvsluttOppfolging() {
        startOppfolgingForBruker();
        reset(kafkaProducerService);

        var perioder = oppfolgingsPeriodeRepository.hentOppfolgingsperioder(aktorId).stream().sorted(Comparator.comparing(OppfolgingsperiodeEntity::getStartDato)).toList();
        Assertions.assertThat(perioder.size()).isEqualTo(1);
        var uuidSomSkalAvsluttes = perioder.getFirst().getUuid();

        when(authService.getFnrOrThrow(aktorId)).thenThrow(new IngenGjeldendeIdentException());
        assertThrows(IngenGjeldendeIdentException.class, () -> {
            avsluttOppfolgingService.adminAvsluttSpesifikkOppfolgingsperiode(aktorId, VEILEDER, "en begrunnelse", uuidSomSkalAvsluttes.toString());
        });
    }

    @Test(expected = ForbiddenException.class)
    
    public void avslutt_oppfolging_uten_skrivetilgang_til_bruker() {
        startOppfolgingForBruker();
        when(authService.erInternBruker()).thenReturn(true);
        doCallRealMethod().when(authService).sjekkTilgangTilEnhet(any());

        avsluttOppfolgingService.avsluttOppfolgingHvisKanAvsluttes(new ManuellAvregistrering(aktorId, new VeilederRegistrant(new NavIdent(VEILEDER)), BEGRUNNELSE));
    }

    @Test
    public void skal_ikke_avslutte_oppfolging_hvis_aktive_tiltaksdeltakelser() {
        when(tiltakshistorikkClient.harAktiveTiltaksdeltakelser(fnr.get())).thenReturn(true);
        settTilstandFormidlingsgruppe("IARBS");
        oppfolgingsStatusRepository.opprettOppfolging(aktorId);
        startOppfolgingService.startOppfolgingHvisIkkeAlleredeStartet(OppfolgingsRegistrering.Companion.arbeidssokerRegistrering(fnr, aktorId, new BrukerRegistrant(fnr)));

        assertUnderOppfolgingLagret(aktorId);

        settTilstandFormidlingsgruppe("ISERV");

        reset(kafkaProducerService);
        avsluttOppfolgingService.avsluttOppfolgingHvisKanAvsluttes(new ManuellAvregistrering(aktorId, new VeilederRegistrant(new NavIdent(VEILEDER)), ""));

        verify(kafkaProducerService, never()).publiserOppfolgingsperiode(any(OppfolgingsperiodeDTO.class));
        assertHarGjeldendeOppfolgingsperiode(aktorId);
    }

    @Test
    public void skal_ikke_avslutte_oppfolging_hvis_deltaker_i_ungdomsprogrammet() {
        when(ungdomsprogramClient.erDeltakerIUngdomsprogrammet(fnr.get())).thenReturn(true);
        settTilstandFormidlingsgruppe("IARBS");
        oppfolgingsStatusRepository.opprettOppfolging(aktorId);
        startOppfolgingService.startOppfolgingHvisIkkeAlleredeStartet(OppfolgingsRegistrering.Companion.arbeidssokerRegistrering(fnr, aktorId, new BrukerRegistrant(fnr)));

        assertUnderOppfolgingLagret(aktorId);

        settTilstandFormidlingsgruppe("ISERV");

        reset(kafkaProducerService);
        avsluttOppfolgingService.avsluttOppfolgingHvisKanAvsluttes(new ManuellAvregistrering(aktorId, new VeilederRegistrant(new NavIdent(VEILEDER)), ""));

        verify(kafkaProducerService, never()).publiserOppfolgingsperiode(any(OppfolgingsperiodeDTO.class));
        assertHarGjeldendeOppfolgingsperiode(aktorId);
    }

    @Test
    public void harVeilederTilgang__medEnhetTilgangTilBrukersEnhet() {
        when(authService.harTilgangTilEnhet(any())).thenReturn(true);
        when(arbeidsoppfolgingsKontorService.hentOppfolgingsEnhetId(fnr)).thenReturn(EnhetId.of(ENHET));

        var tilgang = oppfolgingService.harVeilederTilgangTilBrukersEnhet(fnr);
        assertNotNull(tilgang);
        assertTrue(tilgang.getTilgangTilBrukersKontor());
    }

    @Test
    public void harVeilederTilgang__utenEnhetTilgangTilBrukersEnhet() {
        when(arbeidsoppfolgingsKontorService.hentOppfolgingsEnhetId(fnr)).thenReturn(EnhetId.of(ENHET));

        VeilederTilgang veilederIkkeTilgang = oppfolgingService.harVeilederTilgangTilBrukersEnhet(fnr);
        assertFalse(veilederIkkeTilgang.getTilgangTilBrukersKontor());
    }

    @Test
    public void harVeilederTilgang__skal_ikke_ha_tilgang_TilBrukersEnhet_nar_bruker_ikke_har_enhet() {
        when(arbeidsoppfolgingsKontorService.hentOppfolgingsEnhetId(fnr)).thenReturn(EnhetId.of(ENHET));

        VeilederTilgang veilederIkkeTilgang = oppfolgingService.harVeilederTilgangTilBrukersEnhet(fnr);
        assertFalse(veilederIkkeTilgang.getTilgangTilBrukersKontor());
    }

    @Test
    public void skal_krasje_nar_aktorId_er_ukjent() {
        doNothing().when(authService).sjekkLesetilgangMedFnr(fnr);
        doThrow(new IngenGjeldendeIdentException()).when(authService).getAktorIdOrThrow(fnr);
        assertThrows(IngenGjeldendeIdentException.class, this::hentOppfolgingStatus);
    }

    @Test
    public void riktigFnr() {
        OppfolgingStatusData oppfolgingStatusData = hentOppfolgingStatus();
        assertEquals(fnr.get(), oppfolgingStatusData.getFnr());
    }

    @Test
    public void riktigServicegruppe() {
        String servicegruppe = "BATT";
        settTilstandServicegruppe(servicegruppe);
        settStatus(null, servicegruppe, null);

        OppfolgingStatusData oppfolgingStatusData = hentOppfolgingStatus();
        assertEquals(servicegruppe, oppfolgingStatusData.getServicegruppe());
    }

    @Test
    public void hentOppfolgingStatus_brukerSomIkkeErUnderOppfolgingOppdateresIkkeDersomIkkeUnderOppfolgingIArena() {
        OppfolgingStatusData oppfolgingStatusData = hentOppfolgingStatus();

        assertFalse(oppfolgingStatusData.getUnderOppfolging());
    }

    @Test
    public void hentOppfolgingStatus_brukerSomErUnderOppfolgingOgISERVSkalReaktiveresDersomArenaSierReaktiveringErMulig() {
        startOppfolgingService.startOppfolgingHvisIkkeAlleredeStartet(OppfolgingsRegistrering.Companion.arbeidssokerRegistrering(fnr, aktorId, new VeilederRegistrant(NAV_IDENT)));
        assertUnderOppfolgingLagret(aktorId);

        gittInaktivOppfolgingStatus(true);

        OppfolgingStatusData status = hentOppfolgingStatus();

        assertTrue(status.getKanReaktiveres());
        assertTrue(status.getInaktivIArena());
        assertTrue(status.getUnderOppfolging());
    }

    @Test
    public void hentOppfolgingStatus_brukerSomErKRRSkalVareManuell() {
        gittReservasjonIKrr(true);
        OppfolgingStatusData status = hentOppfolgingStatus();

        assertTrue(status.getReservasjonKRR());
        assertTrue(status.getManuell());
    }

    @Test
    public void utenReservasjon() {
        gittReservasjonIKrr(false);
        OppfolgingStatusData oppfolgingStatusData = hentOppfolgingStatus();
        assertFalse(oppfolgingStatusData.getReservasjonKRR());
    }

    @Test
    public void ikkeArbeidssokerIkkeUnderOppfolging() {
        gittArenaOppfolgingStatus("IARBS", "");

        var oppfolgingOgVilkarStatus = hentOppfolgingStatus();

        assertFalse(oppfolgingOgVilkarStatus.getUnderOppfolging());
    }

    @Test
    public void kanIkkeAvslutteNarManIkkeErUnderOppfolging() {
        oppfolgingsStatusRepository.opprettOppfolging(aktorId);

        AvslutningStatusData avslutningStatusData = avsluttOppfolgingService.hentAvslutningstatusForManuellAvslutning(fnr);

        assertFalse(avslutningStatusData.getKanAvslutte());
    }

    @Test
    public void kanAvslutteSelvOmManErAktivIArenaHvisAvslutningGjøresManuelt() {
        startOppfolgingService.startOppfolgingHvisIkkeAlleredeStartet(OppfolgingsRegistrering.Companion.arbeidssokerRegistrering(fnr, aktorId, new VeilederRegistrant(NAV_IDENT)));
        assertUnderOppfolgingLagret(aktorId);
        gittArenaOppfolgingStatus("ARBS", null);

        KunneAvsluttesResultat avslutningStatusData = avsluttOppfolgingService.avsluttOppfolgingHvisKanAvsluttes(
                new ManuellAvregistrering(aktorId, new VeilederRegistrant(new NavIdent(VEILEDER)), "")
        );

        assertInstanceOf(KunneAvsluttes.class, avslutningStatusData);
    }

    @Test
    public void kanAvslutteSelvOmManErAktivIArenaHvisAvslutningGjøresAvAdmin() {
        startOppfolgingService.startOppfolgingHvisIkkeAlleredeStartet(OppfolgingsRegistrering.Companion.arbeidssokerRegistrering(fnr, aktorId, new VeilederRegistrant(NAV_IDENT)));
        assertUnderOppfolgingLagret(aktorId);
        gittArenaOppfolgingStatus("ARBS", null);
        var oppfolgingsperiodeId = oppfolgingsPeriodeRepository.hentOppfolgingsperioder(aktorId).getFirst().getUuid();

        KunneAvsluttesResultat avslutningStatusData = avsluttOppfolgingService.avsluttOppfolgingHvisKanAvsluttes(new AdminAvregistrering(aktorId, new VeilederRegistrant(new NavIdent(VEILEDER)), "", oppfolgingsperiodeId));

        assertInstanceOf(KunneAvsluttes.class, avslutningStatusData);
    }

    @Test
    public void kanIkkeAvslutteOmManErAktivIArenaHvisAvslutningEr_UtmeldtEtter28Dager() {
        startOppfolgingService.startOppfolgingHvisIkkeAlleredeStartet(OppfolgingsRegistrering.Companion.arbeidssokerRegistrering(fnr, aktorId, new VeilederRegistrant(NAV_IDENT)));
        assertUnderOppfolgingLagret(aktorId);
        gittArenaOppfolgingStatus("ARBS", null);

        KunneAvsluttesResultat avslutningStatusData = avsluttOppfolgingService.avsluttOppfolgingHvisKanAvsluttes(new UtmeldtEtter28Dager(aktorId));

        assertInstanceOf(KunneIkkeAvsluttes.class, avslutningStatusData);
    }

    @Test
    public void kanIkkeAvslutteOmManErAktivIArenaHvisAvslutningEr_ArenaIservKanIkkeReaktiveres() {
        startOppfolgingService.startOppfolgingHvisIkkeAlleredeStartet(OppfolgingsRegistrering.Companion.arbeidssokerRegistrering(fnr, aktorId, new VeilederRegistrant(NAV_IDENT)));
        assertUnderOppfolgingLagret(aktorId);
        when(arbeidssoekerregisteretClient.erArbeidssoeker(fnr.get())).thenReturn(true);

        KunneAvsluttesResultat avslutningStatusData = avsluttOppfolgingService.avsluttOppfolgingHvisKanAvsluttes(new ArenaIservKanIkkeReaktiveres(aktorId));

        assertInstanceOf(KunneIkkeAvsluttes.class, avslutningStatusData);
    }

    @Test
    public void kanIkkeAvslutteHvisManHarAktiveTiltaksdeltakelser() {
        when(tiltakshistorikkClient.harAktiveTiltaksdeltakelser(fnr.get())).thenReturn(true);
        startOppfolgingService.startOppfolgingHvisIkkeAlleredeStartet(OppfolgingsRegistrering.Companion.arbeidssokerRegistrering(fnr, aktorId, new VeilederRegistrant(NAV_IDENT)));
        assertUnderOppfolgingLagret(aktorId);

        gittArenaOppfolgingStatus("ISERV", "");

        AvslutningStatusData avslutningStatusData = avsluttOppfolgingService.hentAvslutningstatusForManuellAvslutning(fnr);

        assertFalse(avslutningStatusData.getKanAvslutte());
        assertTrue(avslutningStatusData.getHarAktiveTiltaksdeltakelser());
    }

    @Test
    public void kanIkkeAvslutteHvisManErDeltakerIUngdomsprogrammet() {
        when(ungdomsprogramClient.erDeltakerIUngdomsprogrammet(fnr.get())).thenReturn(true);
        startOppfolgingService.startOppfolgingHvisIkkeAlleredeStartet(OppfolgingsRegistrering.Companion.arbeidssokerRegistrering(fnr, aktorId, new VeilederRegistrant(NAV_IDENT)));
        assertUnderOppfolgingLagret(aktorId);

        gittArenaOppfolgingStatus("ISERV", "");

        AvslutningStatusData avslutningStatusData = avsluttOppfolgingService.hentAvslutningstatusForManuellAvslutning(fnr);

        assertFalse(avslutningStatusData.getKanAvslutte());
        assertTrue(avslutningStatusData.getErDeltakerIUngdomsprogrammet());
    }

    @Test
    public void kanIkkeAvslutteHvisManErArbeidssoeker() {
        when(arbeidssoekerregisteretClient.erArbeidssoeker(fnr.get())).thenReturn(true);
        startOppfolgingService.startOppfolgingHvisIkkeAlleredeStartet(OppfolgingsRegistrering.Companion.arbeidssokerRegistrering(fnr, aktorId, new VeilederRegistrant(NAV_IDENT)));
        assertUnderOppfolgingLagret(aktorId);

        gittArenaOppfolgingStatus("ISERV", "");

        AvslutningStatusData avslutningStatusData = avsluttOppfolgingService.hentAvslutningstatusForManuellAvslutning(fnr);

        assertFalse(avslutningStatusData.getKanAvslutte());
        assertTrue(avslutningStatusData.getErArbeidssoeker());
    }

    @Test
    public void kanIkkeAvslutteHvisManHarAap() {
        when(aapClient.harAap(fnr.get())).thenReturn(true);
        startOppfolgingService.startOppfolgingHvisIkkeAlleredeStartet(OppfolgingsRegistrering.Companion.arbeidssokerRegistrering(fnr, aktorId, new VeilederRegistrant(NAV_IDENT)));
        assertUnderOppfolgingLagret(aktorId);

        gittArenaOppfolgingStatus("ISERV", "");

        AvslutningStatusData avslutningStatusData = avsluttOppfolgingService.hentAvslutningstatusForManuellAvslutning(fnr);

        assertFalse(avslutningStatusData.getKanAvslutte());
        assertTrue(avslutningStatusData.getHarAap());
    }

    @Test(expected = ForbiddenException.class)
    public void underOppfolgingNiva3_skalFeileHvisIkkeTilgang() {
        doThrow(new ForbiddenException("Hei"))
                .when(authService).sjekkTilgangTilPersonMedNiva3(aktorId);

        oppfolgingService.erUnderOppfolgingNiva3(fnr);
    }

    @Test
    public void underOppfolgingNiva3_skalReturnereFalseHvisIngenDataOmBruker() {
        assertFalse(oppfolgingService.erUnderOppfolgingNiva3(fnr));
    }

    @Test
    public void underOppfolgingNiva3_skalReturnereTrueHvisBrukerHarOppfolgingsflagg() {
        startOppfolgingService.startOppfolgingHvisIkkeAlleredeStartet(OppfolgingsRegistrering.Companion.arenaSyncOppfolgingBrukerRegistrering(fnr, aktorId, Formidlingsgruppe.IARBS, Kvalifiseringsgruppe.BKART));
        assertUnderOppfolgingLagret(aktorId);

        assertTrue(oppfolgingService.erUnderOppfolgingNiva3(fnr));
    }

    @Test
    public void startOppfolgingHvisIkkeAlleredeStartet__skal_opprette_ikke_opprette_manuell_status_hvis_ikke_reservert_i_krr() {
        gittReservasjonIKrr(false);

        startOppfolgingService.startOppfolgingHvisIkkeAlleredeStartet(OppfolgingsRegistrering.Companion.arenaSyncOppfolgingBrukerRegistrering(fnr, aktorId, Formidlingsgruppe.IARBS, Kvalifiseringsgruppe.IKVAL));

        verify(manuellStatusService, never()).settBrukerTilManuellGrunnetReservertIKRR(any());
    }

    @Test
    public void startOppfolgingHvisIkkeAlleredeStartet__skal_opprette_manuell_status_hvis_reservert_i_krr() {
        gittReservasjonIKrr(true);

        startOppfolgingService.startOppfolgingHvisIkkeAlleredeStartet(OppfolgingsRegistrering.Companion.arenaSyncOppfolgingBrukerRegistrering(fnr, aktorId, Formidlingsgruppe.IARBS, Kvalifiseringsgruppe.IVURD));

        verify(manuellStatusService, times(1)).settBrukerTilManuellGrunnetReservertIKRR(aktorId);
    }

    private void assertUnderOppfolgingLagret(AktorId aktorId) {
        assertTrue(oppfolgingsStatusRepository.hentOppfolging(aktorId).orElseThrow().getUnderOppfolging());

        assertHarGjeldendeOppfolgingsperiode(aktorId);

        assertEquals(
                oppfolgingsPeriodeRepository.hentOppfolgingsperioder(aktorId).size(),
                oppfolgingsPeriodeRepository.hentAvsluttetOppfolgingsperioder(aktorId).size() + 1
        );
    }


    private void assertHarGjeldendeOppfolgingsperiode(AktorId aktorId) {
        assertTrue(harGjeldendeOppfolgingsperiode(aktorId));
    }

    private boolean harGjeldendeOppfolgingsperiode(AktorId aktorId) {
        List<OppfolgingsperiodeEntity> oppfolgingsperioder = oppfolgingsPeriodeRepository.hentOppfolgingsperioder(aktorId);
        OppfolgingsperiodeEntity sisteOppfolgingsperiode = OppfolgingsperiodeUtils.hentSisteOppfolgingsperiode(oppfolgingsperioder);
        return sisteOppfolgingsperiode != null && sisteOppfolgingsperiode.getStartDato() != null && sisteOppfolgingsperiode.getSluttDato() == null;
    }

    private void stubArenaTilstand() {
        when(arenaOppfolgingService.hentArenaOppfolgingTilstand(fnr))
                .thenReturn(new ArenaOppfolgingTilstandOppslagResult.Success(arenaOppfolgingTilstand));
    }

    private void stubArenaStatus() {
        when(arenaOppfolgingService.hentArenaOppfolgingsStatus(fnr)).thenReturn(Optional.of(arenaOppfolgingStatus));
    }

    private void settTilstandFormidlingsgruppe(String formidlingsgruppe) {
        arenaOppfolgingTilstand = new ArenaOppfolgingTilstand(
                formidlingsgruppe, arenaOppfolgingTilstand.getServicegruppe(), arenaOppfolgingTilstand.getInaktiveringsdato());
        stubArenaTilstand();
    }

    private void settTilstandServicegruppe(String servicegruppe) {
        arenaOppfolgingTilstand = new ArenaOppfolgingTilstand(
                arenaOppfolgingTilstand.getFormidlingsgruppe(), servicegruppe, arenaOppfolgingTilstand.getInaktiveringsdato());
        stubArenaTilstand();
    }

    private void settStatus(String formidlingsgruppe, String servicegruppe, Boolean kanEnkeltReaktiveres) {
        arenaOppfolgingStatus = new VeilarbArenaOppfolgingsStatus(
                arenaOppfolgingStatus.getRettighetsgruppe(),
                formidlingsgruppe != null ? formidlingsgruppe : arenaOppfolgingStatus.getFormidlingsgruppe(),
                servicegruppe != null ? servicegruppe : arenaOppfolgingStatus.getServicegruppe(),
                arenaOppfolgingStatus.getOppfolgingsenhet(),
                arenaOppfolgingStatus.getInaktiveringsdato(),
                kanEnkeltReaktiveres != null ? kanEnkeltReaktiveres : arenaOppfolgingStatus.getKanEnkeltReaktiveres()
        );
        stubArenaStatus();
    }

    private void gittInaktivOppfolgingStatus(Boolean kanEnkeltReaktiveres) {
        settTilstandFormidlingsgruppe("ISERV");
        settStatus("ISERV", null, kanEnkeltReaktiveres);
    }

    private void gittArenaOppfolgingStatus(String formidlingskode, String kvalifiseringsgruppekode) {
        arenaOppfolgingTilstand = new ArenaOppfolgingTilstand(
                formidlingskode, kvalifiseringsgruppekode, arenaOppfolgingTilstand.getInaktiveringsdato());
        stubArenaTilstand();
    }

    private OppfolgingStatusData hentOppfolgingStatus() {
        return oppfolgingService.hentOppfolgingsStatus(fnr);
    }

    private void gittReservasjonIKrr(boolean reservert) {
        KRRData kontaktinfo = new KRRData(false, "fnr", false, reservert);

        when(manuellStatusService.hentDigdirKontaktinfo(fnr)).thenReturn(kontaktinfo);
    }

    private void startOppfolgingForBruker() {
        settTilstandFormidlingsgruppe("IARBS");
        oppfolgingsStatusRepository.opprettOppfolging(aktorId);
        startOppfolgingService.startOppfolgingHvisIkkeAlleredeStartet(OppfolgingsRegistrering.Companion.arenaSyncOppfolgingBrukerRegistrering(fnr, aktorId, Formidlingsgruppe.IARBS, Kvalifiseringsgruppe.BATT));

        var periode = oppfolgingsPeriodeRepository.hentGjeldendeOppfolgingsperiode(aktorId).orElseThrow();
        arbeidsoppfolgingskontorRepository.settNavKontor(fnr.get(), aktorId.get(), periode.getUuid(), ENHET);

        UnderOppfolgingDTO underOppfolgingDTO = oppfolgingService.oppfolgingData(fnr);
        Assertions.assertThat(underOppfolgingDTO.getUnderOppfolging()).isTrue();
        Assertions.assertThat(underOppfolgingDTO.getErManuell()).isFalse();
        assertUnderOppfolgingLagret(aktorId);
    }

    private String randomString(int length) {
        return IntStream.range(0, length)
                .map(i -> (int) ('0' + Math.random() * 10))
                .mapToObj(c -> String.valueOf((char) c))
                .collect(Collectors.joining());
    }
}
