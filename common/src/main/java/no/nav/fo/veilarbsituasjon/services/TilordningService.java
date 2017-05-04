package no.nav.fo.veilarbsituasjon.services;

import no.nav.fo.veilarbsituasjon.domain.Tilordning;
import org.springframework.stereotype.Service;

import java.util.LinkedList;

@Service
public interface TilordningService {
    public LinkedList<Tilordning> hentTilordninger(String sinceId);
}
