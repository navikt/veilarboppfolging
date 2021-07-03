package no.nav.veilarboppfolging.service;

import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.veilarboppfolging.repository.KvpRepository;
import no.nav.veilarboppfolging.repository.MaalRepository;
import no.nav.veilarboppfolging.repository.OppfolgingsStatusRepository;
import no.nav.veilarboppfolging.repository.entity.KvpPeriodeEntity;
import no.nav.veilarboppfolging.repository.entity.MaalEntity;
import no.nav.veilarboppfolging.repository.entity.OppfolgingEntity;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static java.time.temporal.ChronoUnit.DAYS;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class MalServiceTest {

    private static final Fnr FNR = Fnr.of("fnr");
    private static final AktorId AKTOR_ID = AktorId.of("aktorId");
    private static final long MAL_ID = 1L;
    private static final long KVP_ID = 1L;
    private static final String ENHET = "enhet";
    private static final ZonedDateTime START_KVP = ZonedDateTime.now();
    private static final ZonedDateTime BEFORE_KVP = START_KVP.minus(1, DAYS);
    private static final ZonedDateTime IN_KVP = START_KVP.plus(1, DAYS);
    private static final ZonedDateTime STOP_KVP = START_KVP.plus(2, DAYS);
    private static final ZonedDateTime AFTER_KVP = START_KVP.plus(3, DAYS);
    private static final String VEILEDER = "veileder";

    @Mock
    private KvpRepository kvpRepositoryMock;

    @Mock
    private AuthService authService;

    @Mock
    private MaalRepository maalRepository;

    @Mock
    private MetricsService metricsService;

    @Mock
    private KafkaProducerService kafkaProducerService;

    @Mock
    private OppfolgingsStatusRepository oppfolgingsStatusRepository;

    @Mock
    private TransactionTemplate transactor;

    @InjectMocks
    private MaalService maalService;

    @Before
    public void setup() {
        when(authService.getAktorIdOrThrow(FNR)).thenReturn(AKTOR_ID);
        when(oppfolgingsStatusRepository.hentOppfolging(AKTOR_ID)).thenReturn(Optional.of(new OppfolgingEntity().setGjeldendeMaalId(MAL_ID)));
        when(maalRepository.hentMaal(MAL_ID)).thenReturn(Optional.of(mal(BEFORE_KVP)));
        doAnswer((mock) -> {
            Consumer consumer = mock.getArgument(0);
            consumer.accept(null);
            return null;
        }).when(transactor).executeWithoutResult(any(Consumer.class));
    }

    @Test(expected = ResponseStatusException.class)
    public void oppdaterMal_veilederUtenTilgang_KvpBruker_kasterException() {
        when(kvpRepositoryMock.gjeldendeKvp(any())).thenReturn(KVP_ID);
        when(kvpRepositoryMock.hentKvpPeriode(anyLong())).thenReturn(Optional.of(aktivKvp()));
        when(authService.harTilgangTilEnhetMedSperre(ENHET)).thenReturn(false);

        maalService.oppdaterMal("mal", FNR, VEILEDER);
    }

    @Test
    public void oppdaterMal_veilederMedTilgang_KvpBruker_kasterIkkeException() {
        when(kvpRepositoryMock.gjeldendeKvp(any())).thenReturn(KVP_ID);
        when(kvpRepositoryMock.hentKvpPeriode(anyLong())).thenReturn(Optional.of(aktivKvp()));
        when(authService.harTilgangTilEnhetMedSperre(ENHET)).thenReturn(true);
        MaalEntity resultat = maalService.oppdaterMal("mal", FNR, VEILEDER);

        assertEquals("mal", resultat.getMal());
    }

    @Test
    public void gjeldendeMal_ikke_satt() {
        when(oppfolgingsStatusRepository.hentOppfolging(AKTOR_ID)).thenReturn(Optional.of(new OppfolgingEntity().setGjeldendeMaalId(0)));

        MaalEntity malData = maalService.hentMal(FNR);
        assertThat(malData.getId()).isEqualTo(0L);
    }

    @Test
    public void hent_mal_ingen_kvp() {
        MaalEntity malData = maalService.hentMal(FNR);
        assertThat(malData.getId()).isEqualTo(MAL_ID);
    }

    @Test
    public void hent_mal_opprettet_for_kvp() {
        when(kvpRepositoryMock.hentKvpHistorikk(AKTOR_ID)).thenReturn(kvpHistorikk());

        MaalEntity malData = maalService.hentMal(FNR);
        assertThat(malData.getId()).isEqualTo(MAL_ID);
    }

    @Test
    public void hent_mal_opprettet_etter_kvp_veileder_har_ikke_tilgang() {
        when(kvpRepositoryMock.hentKvpHistorikk(AKTOR_ID)).thenReturn(kvpHistorikk());
        when(maalRepository.hentMaal(MAL_ID)).thenReturn(Optional.of(mal(IN_KVP)));
        when(authService.harTilgangTilEnhetMedSperre(ENHET)).thenReturn(false);

        MaalEntity malData = maalService.hentMal(FNR);
        assertThat(malData.getId()).isEqualTo(0L);
    }

    @Test
    public void hent_mal_opprettet_etter_kvp_veileder_har_tilgang() {
        when(kvpRepositoryMock.hentKvpHistorikk(AKTOR_ID)).thenReturn(kvpHistorikk());
        when(maalRepository.hentMaal(MAL_ID)).thenReturn(Optional.of(mal(IN_KVP)));
        when(authService.harTilgangTilEnhetMedSperre(ENHET)).thenReturn(true);

        MaalEntity malData = maalService.hentMal(FNR);
        assertThat(malData.getId()).isEqualTo(MAL_ID);
    }

    @Test
    public void hent_mal_historikk_med_kvp_i_midten_veileder_har_Tilgang() {
        when(kvpRepositoryMock.hentKvpHistorikk(AKTOR_ID)).thenReturn(kvpHistorikk());
        when(authService.harTilgangTilEnhetMedSperre(ENHET)).thenReturn(true);
        when(maalRepository.aktorMal(AKTOR_ID)).thenReturn(malList());

        List<MaalEntity> malData = maalService.hentMalList(FNR);
        List<Long> ids = malData.stream().map(MaalEntity::getId).collect(toList());
        assertThat(ids).containsExactly(1L, 2L, 3L);
    }

    @Test
    public void hent_mal_historikk_med_kvp_i_midten_veileder_har_ikke_Tilgang() {
        when(kvpRepositoryMock.hentKvpHistorikk(AKTOR_ID)).thenReturn(kvpHistorikk());
        when(authService.harTilgangTilEnhetMedSperre(ENHET)).thenReturn(false);
        when(maalRepository.aktorMal(AKTOR_ID)).thenReturn(malList());

        List<MaalEntity> malData = maalService.hentMalList(FNR);
        List<Long> ids = malData.stream().map(MaalEntity::getId).collect(toList());
        assertThat(ids).containsExactly(1L, 3L);
    }

    private List<MaalEntity> malList() {
        return asList(new MaalEntity()
                        .setId(1L)
                        .setAktorId(AKTOR_ID.get())
                        .setDato(BEFORE_KVP),
                new MaalEntity()
                        .setId(2L)
                        .setAktorId(AKTOR_ID.get())
                        .setDato(IN_KVP),
                new MaalEntity()
                        .setId(3L)
                        .setAktorId(AKTOR_ID.get())
                        .setDato(AFTER_KVP)
        );
    }

    private KvpPeriodeEntity aktivKvp() {
        return KvpPeriodeEntity.builder()
                .kvpId(KVP_ID)
                .enhet(ENHET)
                .opprettetDato(START_KVP)
                .build();
    }

    private List<KvpPeriodeEntity> kvpHistorikk() {
        return singletonList(KvpPeriodeEntity.builder()
                .kvpId(KVP_ID)
                .enhet(ENHET)
                .opprettetDato(START_KVP)
                .avsluttetDato(STOP_KVP)
                .build());
    }

    private MaalEntity mal(ZonedDateTime dateTime) {
        return new MaalEntity()
                .setId(MAL_ID)
                .setAktorId(AKTOR_ID.get())
                .setDato(dateTime);
    }

}
