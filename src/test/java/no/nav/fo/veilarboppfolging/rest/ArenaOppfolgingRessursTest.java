package no.nav.fo.veilarboppfolging.rest;

import no.nav.apiapp.security.PepClient;
import no.nav.dialogarena.aktor.AktorService;
import no.nav.fo.veilarboppfolging.domain.OppfolgingskontraktData;
import no.nav.fo.veilarboppfolging.domain.OppfolgingskontraktResponse;
import no.nav.fo.veilarboppfolging.mappers.OppfolgingMapper;
import no.nav.fo.veilarboppfolging.services.ArenaOppfolgingService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ArenaOppfolgingRessursTest {

    @InjectMocks
    private ArenaOppfolgingRessurs oppfoelgingRessurs;

    @Mock
    private OppfolgingMapper oppfolgingMapper;

    @Mock
    @SuppressWarnings("unused")
    private ArenaOppfolgingService arenaOppfolgingService;

    @Mock
    private PepClient pepClient;

    @Mock
    private AktorService aktorService;

    @Test
    public void getOppfoelgingSkalReturnereEnRespons() throws Exception {

        when(oppfolgingMapper.tilOppfolgingskontrakt(any())).thenReturn(
                new OppfolgingskontraktResponse(Collections.singletonList(new OppfolgingskontraktData()))
        );

        when(aktorService.getAktorId("fnr")).thenReturn(Optional.of("aktorId"));

        final OppfolgingskontraktResponse oppfoelging = oppfoelgingRessurs.getOppfoelging("fnr");

        assertThat(oppfoelging.getOppfoelgingskontrakter().isEmpty(), is(false));
    }
}
