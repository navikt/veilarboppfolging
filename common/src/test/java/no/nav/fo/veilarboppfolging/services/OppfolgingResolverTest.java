package no.nav.fo.veilarboppfolging.services;


import no.nav.apiapp.feil.UlovligHandling;
import no.nav.dialogarena.aktor.AktorService;
import no.nav.fo.veilarboppfolging.db.OppfolgingRepository;
import no.nav.fo.veilarboppfolging.domain.Oppfolging;
import no.nav.fo.veilarboppfolging.domain.Oppfolgingsperiode;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Date;

import static java.util.Optional.of;
import static org.mockito.Mockito.*;

public class OppfolgingResolverTest {

    private static final String AKTOR_ID = "aktorId";
    private static final String FNR = "fnr";

    private OppfolgingResolver.OppfolgingResolverDependencies oppfolgingResolverDependencies =
            mock(OppfolgingResolver.OppfolgingResolverDependencies.class, RETURNS_MOCKS);
    private OppfolgingRepository oppfolgingRepository = mock(OppfolgingRepository.class);
    private Oppfolging oppfolging = new Oppfolging();
    private OppfolgingResolver oppfolgingResolver;

    @Before
    public void setup() {
        AktorService aktorServiceMock = mock(AktorService.class);
        when(oppfolgingResolverDependencies.getAktorService()).thenReturn(aktorServiceMock);
        when(oppfolgingResolverDependencies.getOppfolgingRepository()).thenReturn(oppfolgingRepository);

        when(aktorServiceMock.getAktorId(FNR)).thenReturn(of(AKTOR_ID));
        when(oppfolgingRepository.hentOppfolging(AKTOR_ID)).thenReturn(of(oppfolging));

        oppfolgingResolver = new OppfolgingResolver(FNR, oppfolgingResolverDependencies);
    }

    @Test(expected = UlovligHandling.class)
    public void slettMal__under_oppfolging__ulovlig() {
        oppfolging.setUnderOppfolging(true);
        oppfolgingResolver.slettMal();
    }

    @Test
    public void slettMal__ikke_under_oppfolging_og_ingen_oppfolgingsperiode__slett_alle_mal_siden_1970() {
        oppfolgingResolver.slettMal();
        verify(oppfolgingRepository).slettMalForAktorEtter(AKTOR_ID, new Date(0));
    }

    @Test
    public void slettMal__ikke_under_oppfolging_og_oppfolgingsperioder__slett_alle_mal_etter_siste_avsluttede_periode() {
        Date date1 = new Date(1);
        Date date2 = new Date(2);
        Date date3 = new Date(3);
        oppfolging.setOppfolgingsperioder(Arrays.asList(
                periode(date1),
                periode(date3),
                periode(date2)
        ));

        oppfolgingResolver.slettMal();

        verify(oppfolgingRepository).slettMalForAktorEtter(eq(AKTOR_ID), eq(date3));
    }

    private Oppfolgingsperiode periode(Date date1) {
        return Oppfolgingsperiode.builder().sluttDato(date1).build();
    }

}