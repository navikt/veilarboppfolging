package no.nav.fo.veilarbsituasjon.services.impl;

import no.nav.fo.veilarbsituasjon.domain.Tilordning;
import no.nav.fo.veilarbsituasjon.services.TilordningService;

import java.util.LinkedList;

public class TilordningServiceImpl implements TilordningService {
    @Override
    public LinkedList<Tilordning> hentTilordninger(String sinceId) {
        return new LinkedList<>();
    }
}
