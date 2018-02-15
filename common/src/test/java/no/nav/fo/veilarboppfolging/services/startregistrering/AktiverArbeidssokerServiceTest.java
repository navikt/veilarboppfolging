package no.nav.fo.veilarboppfolging.services.startregistrering;

import no.nav.fo.veilarboppfolging.domain.AktiverArbeidssokerData;
import no.nav.fo.veilarboppfolging.domain.Fnr;
import no.nav.tjeneste.virksomhet.behandlearbeidssoeker.v1.binding.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.ws.rs.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

public class AktiverArbeidssokerServiceTest {

    private static BehandleArbeidssoekerV1 behandleArbeidssoekerV1;
    private static AktiverArbeidssokerService aktiverArbeidssokerService;
    private static AktiverArbeidssokerData aktiverArbeidssokerData;

    @BeforeEach
    public void setup() {
        aktiverArbeidssokerData = new AktiverArbeidssokerData(new Fnr("fnr"), "kval");
        behandleArbeidssoekerV1 = mock(BehandleArbeidssoekerV1.class);
        aktiverArbeidssokerService = new AktiverArbeidssokerService(behandleArbeidssoekerV1);
    }

    @Test
    public void brukerSomIkkeFinnesIArenaSkalMappesTilNotFoundException() throws Exception {
        doThrow(mock(AktiverBrukerBrukerFinnesIkke.class)).when(behandleArbeidssoekerV1).aktiverBruker(any());
        assertThrows(NotFoundException.class, () -> aktiverArbeidssokerService.aktiverArbeidssoker(aktiverArbeidssokerData));
    }

    @Test
    public void brukerSomIkkeKanReaktiveresIArenaSkalGiServerErrorException() throws Exception {
        doThrow(mock(AktiverBrukerBrukerIkkeReaktivert.class)).when(behandleArbeidssoekerV1).aktiverBruker(any());
        assertThrows(ServerErrorException.class, () -> aktiverArbeidssokerService.aktiverArbeidssoker(aktiverArbeidssokerData));
    }

    @Test
    public void brukerSomIkkeKanAktiveresIArenaSkalGiServerErrorException() throws Exception {
        doThrow(mock(AktiverBrukerBrukerKanIkkeAktiveres.class)).when(behandleArbeidssoekerV1).aktiverBruker(any());
        assertThrows(ServerErrorException.class, () -> aktiverArbeidssokerService.aktiverArbeidssoker(aktiverArbeidssokerData));
    }

    @Test
    public void brukerSomManglerArbeidstillatelseSkalGiServerErrorException() throws Exception {
        doThrow(mock(AktiverBrukerBrukerManglerArbeidstillatelse.class)).when(behandleArbeidssoekerV1).aktiverBruker(any());
        assertThrows(ServerErrorException.class, () -> aktiverArbeidssokerService.aktiverArbeidssoker(aktiverArbeidssokerData));
    }

    @Test
    public void brukerSomIkkeHarTilgangSkalGiNotAuthorizedException() throws Exception {
        doThrow(mock(AktiverBrukerSikkerhetsbegrensning.class)).when(behandleArbeidssoekerV1).aktiverBruker(any());
        assertThrows(NotAuthorizedException.class, () -> aktiverArbeidssokerService.aktiverArbeidssoker(aktiverArbeidssokerData));
    }

    @Test
    public void ugyldigInputSkalGiBadRequestException() throws Exception {
        doThrow(mock(AktiverBrukerUgyldigInput.class)).when(behandleArbeidssoekerV1).aktiverBruker(any());
        assertThrows(BadRequestException.class, () -> aktiverArbeidssokerService.aktiverArbeidssoker(aktiverArbeidssokerData));
    }

}