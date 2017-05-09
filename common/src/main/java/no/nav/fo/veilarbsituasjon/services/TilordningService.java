package no.nav.fo.veilarbsituasjon.services;

import no.nav.fo.veilarbsituasjon.domain.OppfolgingBruker;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public interface TilordningService {
    List<OppfolgingBruker> hentTilordninger(LocalDateTime sinceId);
}
