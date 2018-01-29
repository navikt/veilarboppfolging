package no.nav.fo.veilarboppfolging.services;

import no.nav.dialogarena.aktor.AktorService;
import no.nav.fo.veilarboppfolging.db.KvpRepository;
import no.nav.fo.veilarboppfolging.db.OppfolgingRepository;
import no.nav.fo.veilarboppfolging.domain.Kvp;
import no.nav.fo.veilarboppfolging.domain.MalData;
import no.nav.fo.veilarboppfolging.domain.Oppfolging;
import no.nav.fo.veilarboppfolging.services.OppfolgingResolver.OppfolgingResolverDependencies;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;

import static java.sql.Timestamp.from;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class MalServiceTest {

    private static final String FNR = "12345678901";
    private static final String AKTOR_ID = "123456";
    private static final long MAL_ID = 1L;
    private static final long KVP_ID = 1L;
    private static final String ENHET = "1234";
    private static final Instant START_KVP = OffsetDateTime.now().toInstant();
    private static final Instant BEFORE_KVP = START_KVP.minus(1, DAYS);
    private static final Instant IN_KVP = START_KVP.plus(1, DAYS);
    private static final Instant STOP_KVP = START_KVP.plus(2, DAYS);
    private static final Instant AFTER_KVP = START_KVP.plus(3, DAYS);

    @Mock(answer = Answers.RETURNS_MOCKS)
    private OppfolgingResolverDependencies oppfolgingResolverDependenciesMock;

    @Mock
    private EnhetPepClient enhetPepClientMock;

    @Mock
    private KvpRepository kvpRepositoryMock;

    @Mock
    private OppfolgingRepository oppfolgingRepositoryMock;

    @Mock
    private AktorService aktorServiceMock;

    @InjectMocks
    private MalService malService;

    @Before
    public void setup() {
        when(oppfolgingResolverDependenciesMock.getAktorService()).thenReturn(aktorServiceMock);
        when(oppfolgingResolverDependenciesMock.getOppfolgingRepository()).thenReturn(oppfolgingRepositoryMock);

        when(aktorServiceMock.getAktorId(FNR)).thenReturn(of(AKTOR_ID));
        when(oppfolgingRepositoryMock.hentOppfolging(AKTOR_ID)).thenReturn(of(oppfolging(from(BEFORE_KVP))));

    }

    @Test
    public void gjeldendeMal_ikke_satt_med_kvp() {
        when(kvpRepositoryMock.hentKvpHistorikk(AKTOR_ID)).thenReturn(kvp());
        when(oppfolgingRepositoryMock.hentOppfolging(AKTOR_ID)).thenReturn(of(new Oppfolging()));

        MalData malData = malService.hentMal(FNR);
        assertThat(malData.getId()).isEqualTo(0L);
    }

    @Test
    public void hent_mal_ingen_kvp() {
        MalData malData = malService.hentMal(FNR);
        assertThat(malData.getId()).isEqualTo(MAL_ID);
    }

    @Test
    public void hent_mal_opprettet_for_kvp() {
        when(kvpRepositoryMock.hentKvpHistorikk(AKTOR_ID)).thenReturn(kvp());

        MalData malData = malService.hentMal(FNR);
        assertThat(malData.getId()).isEqualTo(MAL_ID);
    }

    @Test
    public void hent_mal_opprettet_etter_kvp_veileder_har_ikke_tilgang() {
        when(kvpRepositoryMock.hentKvpHistorikk(AKTOR_ID)).thenReturn(kvp());
        when(oppfolgingRepositoryMock.hentOppfolging(AKTOR_ID)).thenReturn(of(oppfolging(from(IN_KVP))));
        when(enhetPepClientMock.harTilgang(ENHET)).thenReturn(false);

        MalData malData = malService.hentMal(FNR);
        assertThat(malData.getId()).isEqualTo(0L);
    }

    @Test
    public void hent_mal_opprettet_etter_kvp_veileder_har_tilgang() {
        when(kvpRepositoryMock.hentKvpHistorikk(AKTOR_ID)).thenReturn(kvp());
        when(oppfolgingRepositoryMock.hentOppfolging(AKTOR_ID)).thenReturn(of(oppfolging(from(IN_KVP))));
        when(enhetPepClientMock.harTilgang(ENHET)).thenReturn(true);

        MalData malData = malService.hentMal(FNR);
        assertThat(malData.getId()).isEqualTo(MAL_ID);
    }

    @Test
    public void hent_mal_historikk_med_kvp_i_midten_veileder_har_Tilgang() {
        when(kvpRepositoryMock.hentKvpHistorikk(AKTOR_ID)).thenReturn(kvp());
        when(enhetPepClientMock.harTilgang(ENHET)).thenReturn(true);
        when(oppfolgingRepositoryMock.hentMalList(AKTOR_ID)).thenReturn(malList());

        List<MalData> malData = malService.hentMalList(FNR);
        List<Long> ids = malData.stream().map(MalData::getId).collect(toList());
        assertThat(ids).containsExactly(1L, 2L, 3L);
    }

    @Test
    public void hent_mal_historikk_med_kvp_i_midten_veileder_har_ikke_Tilgang() {
        when(kvpRepositoryMock.hentKvpHistorikk(AKTOR_ID)).thenReturn(kvp());
        when(enhetPepClientMock.harTilgang(ENHET)).thenReturn(false);
        when(oppfolgingRepositoryMock.hentMalList(AKTOR_ID)).thenReturn(malList());

        List<MalData> malData = malService.hentMalList(FNR);
        List<Long> ids = malData.stream().map(MalData::getId).collect(toList());
        assertThat(ids).containsExactly(1L, 3L);
    }

    private List<MalData> malList() {

        return asList(new MalData()
                        .setId(1L)
                        .setAktorId(AKTOR_ID)
                        .setDato(from(BEFORE_KVP)),
                new MalData()
                        .setId(2L)
                        .setAktorId(AKTOR_ID)
                        .setDato(from(IN_KVP)),
                new MalData()
                        .setId(3L)
                        .setAktorId(AKTOR_ID)
                        .setDato(from(AFTER_KVP))
        );
    }


    private List<Kvp> kvp() {
        return singletonList(Kvp.builder()
                .kvpId(KVP_ID)
                .enhet(ENHET)
                .opprettetDato(from(START_KVP))
                .avsluttetDato(from(STOP_KVP))
                .build());
    }

    private Oppfolging oppfolging(Timestamp malDato) {
        MalData mal = new MalData()
                .setId(MAL_ID)
                .setAktorId(AKTOR_ID)
                .setDato(malDato);
        return new Oppfolging().setGjeldendeMal(mal);
    }

}