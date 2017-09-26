package no.nav.fo.veilarboppfolging.services;


import no.nav.apiapp.feil.UlovligHandling;
import no.nav.fo.veilarboppfolging.db.SituasjonRepository;
import no.nav.fo.veilarboppfolging.domain.Oppfolgingsperiode;
import no.nav.fo.veilarboppfolging.domain.Situasjon;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Date;

import static java.util.Optional.of;
import static org.mockito.Mockito.*;

public class SituasjonResolverTest {

    private static final String AKTOR_ID = "aktorId";
    private static final String FNR = "fnr";

    private SituasjonResolver.SituasjonResolverDependencies situasjonResolverDependencies = mock(SituasjonResolver.SituasjonResolverDependencies.class, RETURNS_MOCKS);
    private SituasjonRepository situasjonRepository = mock(SituasjonRepository.class);
    private Situasjon situasjon = new Situasjon();
    private SituasjonResolver situasjonResolver;

    @Before
    public void setup() {
        AktoerIdService aktoerIdService = mock(AktoerIdService.class);
        when(situasjonResolverDependencies.getAktoerIdService()).thenReturn(aktoerIdService);
        when(situasjonResolverDependencies.getSituasjonRepository()).thenReturn(situasjonRepository);

        when(aktoerIdService.findAktoerId(FNR)).thenReturn(AKTOR_ID);
        when(situasjonRepository.hentSituasjon(AKTOR_ID)).thenReturn(of(situasjon));

        situasjonResolver = new SituasjonResolver(FNR, situasjonResolverDependencies);
    }

    @Test(expected = UlovligHandling.class)
    public void slettMal__under_oppfolging__ulovlig() {
        situasjon.setOppfolging(true);
        situasjonResolver.slettMal();
    }

    @Test
    public void slettMal__ikke_under_oppfolging_og_ingen_oppfolgingsperiode__slett_alle_mal_siden_1970() {
        situasjonResolver.slettMal();
        verify(situasjonRepository).slettMalForAktorEtter(AKTOR_ID, new Date(0));
    }

    @Test
    public void slettMal__ikke_under_oppfolging_og_oppfolgingsperioder__slett_alle_mal_etter_siste_avsluttede_periode() {
        Date date1 = new Date(1);
        Date date2 = new Date(2);
        Date date3 = new Date(3);
        situasjon.setOppfolgingsperioder(Arrays.asList(
                periode(date1),
                periode(date3),
                periode(date2)
        ));

        situasjonResolver.slettMal();

        verify(situasjonRepository).slettMalForAktorEtter(eq(AKTOR_ID), eq(date3));
    }

    private Oppfolgingsperiode periode(Date date1) {
        return Oppfolgingsperiode.builder().sluttDato(date1).build();
    }

}