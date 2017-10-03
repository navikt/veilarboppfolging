package no.nav.fo.veilarboppfolging.services;

import no.nav.fo.veilarboppfolging.domain.Oppfolgingsenhet;
import no.nav.tjeneste.virksomhet.organisasjonenhet.v1.HentEnhetBolkUgyldigInput;
import no.nav.tjeneste.virksomhet.organisasjonenhet.v1.OrganisasjonEnhetV1;
import no.nav.tjeneste.virksomhet.organisasjonenhet.v1.informasjon.WSDetaljertEnhet;
import no.nav.tjeneste.virksomhet.organisasjonenhet.v1.meldinger.WSHentEnhetBolkRequest;
import no.nav.tjeneste.virksomhet.organisasjonenhet.v1.meldinger.WSHentEnhetBolkResponse;
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
    private OrganisasjonEnhetV1 organisasjonEnhetWebService;

    @Test
    public void hentOrganisasjonsenhetReturnererEnRespons() throws HentEnhetBolkUgyldigInput {
        when(organisasjonEnhetWebService.hentEnhetBolk(any(WSHentEnhetBolkRequest.class)))
                .thenReturn(new WSHentEnhetBolkResponse().withEnhetListe(new WSDetaljertEnhet().withNavn(MOCK_ENHET_NAVN)));

        Oppfolgingsenhet enhet = organisasjonEnhetService.hentEnhet(MOCK_ENHET_ID);

        assertThat(enhet.getNavn()).isEqualTo(MOCK_ENHET_NAVN);
        assertThat(enhet.getEnhetId()).isEqualTo(MOCK_ENHET_ID);
    }

    @Test(expected = NoSuchElementException.class)
    public void hentOrganisasjonsenhetKasterFeilVedTomRespons() throws HentEnhetBolkUgyldigInput {
        when(organisasjonEnhetWebService.hentEnhetBolk(any(WSHentEnhetBolkRequest.class)))
                .thenReturn(new WSHentEnhetBolkResponse().withEnhetListe());

        organisasjonEnhetService.hentEnhet(MOCK_ENHET_ID);
    }

}