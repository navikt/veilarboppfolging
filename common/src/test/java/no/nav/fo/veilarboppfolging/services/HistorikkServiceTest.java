package no.nav.fo.veilarboppfolging.services;

import no.nav.dialogarena.aktor.AktorService;
import no.nav.fo.veilarboppfolging.db.KvpRepository;
import no.nav.fo.veilarboppfolging.db.OppfolgingRepository;
import no.nav.fo.veilarboppfolging.domain.EskaleringsvarselData;
import no.nav.fo.veilarboppfolging.domain.InnstillingsHistorikk;
import no.nav.fo.veilarboppfolging.domain.Kvp;
import no.nav.fo.veilarboppfolging.domain.ManuellStatus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;

import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Date.from;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class HistorikkServiceTest {

    @Mock
    private AktorService aktorServiceMock;

    @Mock
    private KvpRepository kvpRepositoryMock;

    @Mock
    private EnhetPepClient enhetPepClientMock;

    @Mock
    private OppfolgingRepository oppfolgingRepositoryMock;

    @InjectMocks
    private HistorikkService historikkService;

    private static final String FNR = "12345678901";
    private static final String AKTOR_ID = "123456";
    private static final String ENHET = "1234";

    private static final Instant BEFORE_KVP = OffsetDateTime.now().toInstant();
    private static final Instant ALSO_BEFORE_KVP = BEFORE_KVP.plus(1, HOURS);
    private static final Instant KVP_START = BEFORE_KVP.plus(1, DAYS);
    private static final Instant IN_KVP = BEFORE_KVP.plus(2, DAYS);
    private static final Instant KVP_STOP = BEFORE_KVP.plus(3, DAYS);
    private static final Instant AFTER_KVP = BEFORE_KVP.plus(4, DAYS);


    @Before
    public void setup() {
        when(aktorServiceMock.getAktorId(FNR)).thenReturn(of(AKTOR_ID));

        gitt_kvp();
        gitt_eskaleringsvarsel_historikk();
        gitt_manuell_hitsorikk();
    }

    @Test
    public void saksbehandler_har_ikke_tilgang_til_enhet() {
        when(enhetPepClientMock.harTilgang(ENHET)).thenReturn(false);

        List<InnstillingsHistorikk> historikk = historikkService.hentInstillingsHistorikk(FNR);
        List<String> begrunnelser = historikk.stream()
                .map(InnstillingsHistorikk::getBegrunnelse).collect(toList());

        assertThat(begrunnelser).doesNotContain("IN_KVP");
        assertThat(begrunnelser).contains("OUTSIDE_KVP");
    }

    @Test
    public void saksbehandler_har_tilgang_til_enhet() {
        when(enhetPepClientMock.harTilgang(ENHET)).thenReturn(true);

        List<InnstillingsHistorikk> historikk = historikkService.hentInstillingsHistorikk(FNR);
        List<String> begrunnelser = historikk.stream()
                .map(InnstillingsHistorikk::getBegrunnelse).collect(toList());

        assertThat(begrunnelser).contains("IN_KVP", "OUTSIDE_KVP");
    }

    private void gitt_eskaleringsvarsel_historikk() {
        List<EskaleringsvarselData> eskaleringsvarsel = asList(
                EskaleringsvarselData.builder()
                        .aktorId(AKTOR_ID)
                        .varselId(1L)
                        .opprettetDato(from(BEFORE_KVP))
                        .opprettetBegrunnelse("OUTSIDE_KVP")
                        .avsluttetDato(from(ALSO_BEFORE_KVP))
                        .avsluttetBegrunnelse("OUTSIDE_KVP")
                        .build(),
                EskaleringsvarselData.builder()
                        .aktorId(AKTOR_ID)
                        .varselId(2L)
                        .opprettetDato(from(BEFORE_KVP))
                        .opprettetBegrunnelse("OUTSIDE_KVP")
                        .avsluttetDato(from(IN_KVP))
                        .avsluttetBegrunnelse("IN_KVP")
                        .build(),
                EskaleringsvarselData.builder()
                        .aktorId(AKTOR_ID)
                        .varselId(3L)
                        .opprettetDato(from(IN_KVP))
                        .opprettetBegrunnelse("IN_KVP")
                        .avsluttetDato(from(AFTER_KVP))
                        .avsluttetBegrunnelse("OUTSIDE_KVP")
                        .build()
        );
        when(oppfolgingRepositoryMock.hentEskaleringhistorikk(AKTOR_ID)).thenReturn(eskaleringsvarsel);

    }

    private void gitt_manuell_hitsorikk() {

        List<ManuellStatus> manuellStatus = asList(
                new ManuellStatus()
                        .setAktorId(AKTOR_ID)
                        .setDato(Timestamp.from(BEFORE_KVP))
                        .setBegrunnelse("OUTSIDE_KVP")
                        .setManuell(true),
                new ManuellStatus()
                        .setAktorId(AKTOR_ID)
                        .setDato(Timestamp.from(BEFORE_KVP))
                        .setBegrunnelse("OUTSIDE_KVP")
                        .setManuell(false),
                new ManuellStatus()
                        .setAktorId(AKTOR_ID)
                        .setDato(Timestamp.from(IN_KVP))
                        .setBegrunnelse("IN_KVP")
                        .setManuell(true),
                new ManuellStatus()
                        .setAktorId(AKTOR_ID)
                        .setDato(Timestamp.from(IN_KVP))
                        .setBegrunnelse("IN_KVP")
                        .setManuell(false),
                new ManuellStatus()
                        .setAktorId(AKTOR_ID)
                        .setDato(Timestamp.from(AFTER_KVP))
                        .setBegrunnelse("OUTSIDE_KVP")
                        .setManuell(true),
                new ManuellStatus()
                        .setAktorId(AKTOR_ID)
                        .setDato(Timestamp.from(AFTER_KVP))
                        .setBegrunnelse("OUTSIDE_KVP")
                        .setManuell(false)
        );
        when(oppfolgingRepositoryMock.hentManuellHistorikk(AKTOR_ID)).thenReturn(manuellStatus);
    }

    private void gitt_kvp() {

        List<Kvp> kvp = singletonList(
                Kvp.builder()
                        .aktorId(AKTOR_ID)
                        .kvpId(1L)
                        .opprettetDato(from(KVP_START))
                        .opprettetBegrunnelse("IN_KVP")
                        .avsluttetDato(from(KVP_STOP))
                        .avsluttetBegrunnelse("IN_KVP")
                        .enhet(ENHET)
                        .build()

        );

        when(kvpRepositoryMock.hentKvpHistorikk(AKTOR_ID)).thenReturn(kvp);

    }

}