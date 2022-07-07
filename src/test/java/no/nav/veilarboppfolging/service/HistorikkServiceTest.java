package no.nav.veilarboppfolging.service;

import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.veilarboppfolging.controller.response.HistorikkHendelse;
import no.nav.veilarboppfolging.repository.*;
import no.nav.veilarboppfolging.repository.entity.KvpPeriodeEntity;
import no.nav.veilarboppfolging.repository.entity.ManuellStatusEntity;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.ZonedDateTime;
import java.util.List;

import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class HistorikkServiceTest {

    @Mock
    private AuthService authService;

    @Mock
    private KvpRepository kvpRepositoryMock;

    @Mock
    private TransactionTemplate transactor;

    @Mock
    private VeilederHistorikkRepository veilederHistorikkRepository;

    @Mock
    private OppfolgingsenhetHistorikkRepository oppfolgingsenhetHistorikkRepository;

    @Mock
    private ManuellStatusService manuellStatusService;


    @Mock
    private OppfolgingsPeriodeRepository oppfolgingsPeriodeRepository;

    @InjectMocks
    private HistorikkService historikkService;

    private static final Fnr FNR = Fnr.of("fnr");
    private static final AktorId AKTOR_ID = AktorId.of("aktorId");
    private static final String ENHET = "enhet";

    private static final ZonedDateTime BEFORE_KVP = ZonedDateTime.now();
    private static final ZonedDateTime ALSO_BEFORE_KVP = BEFORE_KVP.plus(1, HOURS);
    private static final ZonedDateTime KVP_START = BEFORE_KVP.plus(1, DAYS);
    private static final ZonedDateTime IN_KVP = BEFORE_KVP.plus(2, DAYS);
    private static final ZonedDateTime KVP_STOP = BEFORE_KVP.plus(3, DAYS);
    private static final ZonedDateTime AFTER_KVP = BEFORE_KVP.plus(4, DAYS);


    @Before
    public void setup() {
        when(authService.getAktorIdOrThrow(FNR)).thenReturn(AKTOR_ID);
        gitt_kvp();
        gitt_manuell_hitsorikk();
    }

    @Test
    public void saksbehandler_har_ikke_tilgang_til_enhet() {
        when(authService.harTilgangTilEnhet(ENHET)).thenReturn(false);

        List<HistorikkHendelse> historikk = historikkService.hentInstillingsHistorikk(FNR);
        List<String> begrunnelser = historikk.stream()
                .map(HistorikkHendelse::getBegrunnelse).collect(toList());

        assertThat(begrunnelser).doesNotContain("IN_KVP");
        assertThat(begrunnelser).contains("OUTSIDE_KVP");
    }

    @Test
    public void saksbehandler_har_tilgang_til_enhet() {
        when(authService.harTilgangTilEnhet(ENHET)).thenReturn(true);

        List<HistorikkHendelse> historikk = historikkService.hentInstillingsHistorikk(FNR);
        List<String> begrunnelser = historikk.stream()
                .map(HistorikkHendelse::getBegrunnelse).collect(toList());

        assertThat(begrunnelser).contains("IN_KVP", "OUTSIDE_KVP");
    }



    private void gitt_manuell_hitsorikk() {
        List<ManuellStatusEntity> manuellStatus = asList(
                new ManuellStatusEntity()
                        .setAktorId(AKTOR_ID.get())
                        .setDato(BEFORE_KVP)
                        .setBegrunnelse("OUTSIDE_KVP")
                        .setManuell(true),
                new ManuellStatusEntity()
                        .setAktorId(AKTOR_ID.get())
                        .setDato(BEFORE_KVP)
                        .setBegrunnelse("OUTSIDE_KVP")
                        .setManuell(false),
                new ManuellStatusEntity()
                        .setAktorId(AKTOR_ID.get())
                        .setDato(IN_KVP)
                        .setBegrunnelse("IN_KVP")
                        .setManuell(true),
                new ManuellStatusEntity()
                        .setAktorId(AKTOR_ID.get())
                        .setDato(IN_KVP)
                        .setBegrunnelse("IN_KVP")
                        .setManuell(false),
                new ManuellStatusEntity()
                        .setAktorId(AKTOR_ID.get())
                        .setDato(AFTER_KVP)
                        .setBegrunnelse("OUTSIDE_KVP")
                        .setManuell(true),
                new ManuellStatusEntity()
                        .setAktorId(AKTOR_ID.get())
                        .setDato(AFTER_KVP)
                        .setBegrunnelse("OUTSIDE_KVP")
                        .setManuell(false)
        );

        when(manuellStatusService.hentManuellStatusHistorikk(AKTOR_ID)).thenReturn(manuellStatus);
    }

    private void gitt_kvp() {
        List<KvpPeriodeEntity> kvp = singletonList(
                KvpPeriodeEntity.builder()
                        .aktorId(AKTOR_ID.get())
                        .kvpId(1L)
                        .opprettetDato(KVP_START)
                        .opprettetBegrunnelse("IN_KVP")
                        .avsluttetDato(KVP_STOP)
                        .avsluttetBegrunnelse("IN_KVP")
                        .enhet(ENHET)
                        .build()

        );

        when(kvpRepositoryMock.hentKvpHistorikk(AKTOR_ID)).thenReturn(kvp);
    }

}
