package no.nav.fo.veilarbsituasjon.service;



import no.nav.fo.veilarbsituasjon.domain.AktoerIdToVeileder;
import no.nav.fo.veilarbsituasjon.repository.AktoerIdToVeilederDAO;
import no.nav.fo.veilarbsituasjon.services.AktoerIdService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;


@RunWith(MockitoJUnitRunner.class)
public class AktoerIdToVeilederTest {

    @Mock
    private AktoerIdToVeilederDAO aktoerIdToVeilederDAO;

    @InjectMocks
    private AktoerIdService aktoerIdService;


    @Test
    public void saveOrUpdateMustCallDAOWithFnrToAktoerId() {
        AktoerIdToVeileder aktoerIdToVeileder = mock(AktoerIdToVeileder.class);
        aktoerIdService.saveOrUpdateAktoerIdToVeileder(aktoerIdToVeileder);
        verify(aktoerIdToVeilederDAO, times(1)).opprettEllerOppdaterAktoerIdToVeileder(aktoerIdToVeileder);
    }
}