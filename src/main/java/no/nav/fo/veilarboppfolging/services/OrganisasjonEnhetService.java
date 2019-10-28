package no.nav.fo.veilarboppfolging.services;

import no.nav.fo.veilarboppfolging.domain.Oppfolgingsenhet;
import no.nav.tjeneste.virksomhet.organisasjonenhet.v2.HentEnhetBolk;
import no.nav.tjeneste.virksomhet.organisasjonenhet.v2.OrganisasjonEnhetV2;
import no.nav.tjeneste.virksomhet.organisasjonenhet.v2.informasjon.WSOrganisasjonsenhet;
import no.nav.tjeneste.virksomhet.organisasjonenhet.v2.meldinger.WSHentEnhetBolkRequest;
import no.nav.tjeneste.virksomhet.organisasjonenhet.v2.meldinger.WSHentEnhetBolkResponse;
import org.springframework.cache.annotation.Cacheable;

import java.util.List;
import java.util.NoSuchElementException;

import static no.nav.fo.veilarboppfolging.config.CacheConfig.HENT_ENHET;


public class OrganisasjonEnhetService {

    private final OrganisasjonEnhetV2 organisasjonenhetWs;

    public OrganisasjonEnhetService(OrganisasjonEnhetV2 organisasjonEnhetV2) {
        this.organisasjonenhetWs = organisasjonEnhetV2;
    }

    @Cacheable(HENT_ENHET)
    public Oppfolgingsenhet hentEnhet(String enhetId) {
        Oppfolgingsenhet oppfolgingsenhet = new Oppfolgingsenhet().withEnhetId(enhetId);
        WSHentEnhetBolkRequest hentEnhetBolkRequest = new HentEnhetBolk().getRequest();

        hentEnhetBolkRequest.getEnhetIdListe().add(enhetId);
        WSHentEnhetBolkResponse response = organisasjonenhetWs.hentEnhetBolk(hentEnhetBolkRequest);
        return oppfolgingsenhet.withNavn(getNavn(response.getEnhetListe(), enhetId));
    }

    private String getNavn(List<WSOrganisasjonsenhet> enhetListe, String enhetId) {
        if (enhetListe.size() == 0) {
            throw new NoSuchElementException("Norg returnerte en tom liste når vi spurte etter detaljer om denne enheten: " + enhetId);
        }
        return enhetListe.get(0).getEnhetNavn();
    }
}
