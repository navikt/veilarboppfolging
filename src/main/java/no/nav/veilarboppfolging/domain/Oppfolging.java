package no.nav.veilarboppfolging.domain;

import no.nav.veilarboppfolging.repository.entity.KvpPeriodeEntity;
import no.nav.veilarboppfolging.repository.entity.OppfolgingsperiodeEntity;
import no.nav.veilarboppfolging.repository.entity.*;

import java.util.List;

import static java.util.Collections.emptyList;

public class Oppfolging {
    private String aktorId;
    private String veilederId;
    private boolean underOppfolging;
    private ManuellStatusEntity gjeldendeManuellStatus;
    private MaalEntity gjeldendeMal;
    private List<OppfolgingsperiodeEntity> oppfolgingsperioder = emptyList();
    private KvpPeriodeEntity gjeldendeKvp;
}
