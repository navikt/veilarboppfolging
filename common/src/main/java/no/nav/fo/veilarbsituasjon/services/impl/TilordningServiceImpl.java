package no.nav.fo.veilarbsituasjon.services.impl;

import no.nav.fo.veilarbsituasjon.db.BrukerRepository;
import no.nav.fo.veilarbsituasjon.domain.OppfolgingBruker;
import no.nav.fo.veilarbsituasjon.services.TilordningService;

import javax.inject.Inject;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

public class TilordningServiceImpl implements TilordningService {

    private BrukerRepository brukerRepository;

    @Inject
    public TilordningServiceImpl(BrukerRepository brukerRepository) {
        this.brukerRepository = brukerRepository;
    }

    @Override
    public List<OppfolgingBruker> hentTilordninger(LocalDateTime sinceId, int pageSize) {
        Timestamp timestamp = Timestamp.valueOf(sinceId);
        return brukerRepository.hentTilordningerEtterTimestamp(timestamp);
    }
}
