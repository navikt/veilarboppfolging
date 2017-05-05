package no.nav.fo.veilarbsituasjon.services.impl;

import no.nav.fo.veilarbsituasjon.domain.Tilordning;
import no.nav.fo.veilarbsituasjon.services.TilordningService;

import java.time.LocalDateTime;
import java.util.LinkedList;

public class TilordningServiceImpl implements TilordningService {
    @Override
    public LinkedList<Tilordning> hentTilordninger(LocalDateTime sinceId) {
        return new LinkedList<>();
    }
}
