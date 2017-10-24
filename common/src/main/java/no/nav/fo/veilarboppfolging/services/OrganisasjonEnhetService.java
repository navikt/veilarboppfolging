package no.nav.fo.veilarboppfolging.services;

import no.nav.fo.veilarboppfolging.domain.Oppfolgingsenhet;
import no.nav.tjeneste.virksomhet.organisasjonenhet.v1.HentEnhetBolkUgyldigInput;
import no.nav.tjeneste.virksomhet.organisasjonenhet.v1.OrganisasjonEnhetV1;
import no.nav.tjeneste.virksomhet.organisasjonenhet.v1.informasjon.WSDetaljertEnhet;
import no.nav.tjeneste.virksomhet.organisasjonenhet.v1.meldinger.WSHentEnhetBolkRequest;
import no.nav.tjeneste.virksomhet.organisasjonenhet.v1.meldinger.WSHentEnhetBolkResponse;
import org.springframework.cache.annotation.Cacheable;

import java.util.List;
import java.util.NoSuchElementException;

import static no.nav.fo.veilarboppfolging.config.CacheConfig.HENT_ENHET;


public class OrganisasjonEnhetService {

    private final OrganisasjonEnhetV1 organisasjonenhetWs;

    public OrganisasjonEnhetService(OrganisasjonEnhetV1 organisasjonEnhetV1) {
        this.organisasjonenhetWs = organisasjonEnhetV1;
    }

    @Cacheable(HENT_ENHET)
    public Oppfolgingsenhet hentEnhet(String enhetId) {
        Oppfolgingsenhet oppfolgingsenhet = new Oppfolgingsenhet().withEnhetId(enhetId);
        try {
            WSHentEnhetBolkResponse response = organisasjonenhetWs.hentEnhetBolk(new WSHentEnhetBolkRequest().withEnhetIdListe(enhetId));
            return oppfolgingsenhet.withNavn(getNavn(response.getEnhetListe(), enhetId));
        } catch (HentEnhetBolkUgyldigInput hentEnhetBolkUgyldigInput) {
            throw new IllegalArgumentException(hentEnhetBolkUgyldigInput);
        }
    }

    private String getNavn(List<WSDetaljertEnhet> enhetListe, String enhetId) {
        if (enhetListe.size() == 0) {
            throw new NoSuchElementException("Norg returnerte en tom liste når vi spurte etter detaljer om denne enheten: " + enhetId);
        }
        return enhetListe.get(0).getNavn();
    }
}
