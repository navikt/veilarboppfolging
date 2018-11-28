package no.nav.fo.veilarboppfolging.services;

import no.nav.fo.veilarboppfolging.domain.Oppfolgingsenhet;
import no.nav.tjeneste.virksomhet.organisasjonenhet.v2.binding.OrganisasjonEnhetV2;
import no.nav.tjeneste.virksomhet.organisasjonenhet.v2.informasjon.Organisasjonsenhet;
import no.nav.tjeneste.virksomhet.organisasjonenhet.v2.meldinger.HentEnhetBolkRequest;
import no.nav.tjeneste.virksomhet.organisasjonenhet.v2.meldinger.HentEnhetBolkResponse;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class OrganisasjonEnhetServiceTest {

    private static final String MOCK_ENHET_NAVN = "NAV AKERSHUS";
    private static final String MOCK_ENHET_ID = "1340";


    @InjectMocks
    private OrganisasjonEnhetService organisasjonEnhetService;

    @Mock
    private OrganisasjonEnhetV2 organisasjonEnhetWebService;

    @Test
    public void hentOrganisasjonsenhetReturnererEnRespons() {
        HentEnhetBolkResponse hentEnhetBolkResponse = new HentEnhetBolkResponse();
        Organisasjonsenhet organisasjonsenhet = new Organisasjonsenhet();
        organisasjonsenhet.setEnhetNavn(MOCK_ENHET_NAVN);
        hentEnhetBolkResponse.getEnhetListe().add(organisasjonsenhet);
        when(organisasjonEnhetWebService.hentEnhetBolk(any(HentEnhetBolkRequest.class)))
                .thenReturn(hentEnhetBolkResponse);

        Oppfolgingsenhet enhet = organisasjonEnhetService.hentEnhet(MOCK_ENHET_ID);

        assertThat(enhet.getNavn()).isEqualTo(MOCK_ENHET_NAVN);
        assertThat(enhet.getEnhetId()).isEqualTo(MOCK_ENHET_ID);
    }

    @Test(expected = NoSuchElementException.class)
    public void hentOrganisasjonsenhetKasterFeilVedTomRespons() {
        when(organisasjonEnhetWebService.hentEnhetBolk(any(HentEnhetBolkRequest.class)))
                .thenReturn(new HentEnhetBolkResponse());

        organisasjonEnhetService.hentEnhet(MOCK_ENHET_ID);
    }

}