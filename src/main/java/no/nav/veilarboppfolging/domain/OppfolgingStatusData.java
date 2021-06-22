package no.nav.veilarboppfolging.domain;

import lombok.Data;
import lombok.experimental.Accessors;
import no.nav.veilarboppfolging.repository.entity.EskaleringsvarselEntity;
import no.nav.veilarboppfolging.repository.entity.KvpEntity;
import no.nav.veilarboppfolging.repository.entity.OppfolgingsperiodeEntity;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static java.util.Comparator.naturalOrder;

@Data
@Accessors(chain = true)
public class OppfolgingStatusData {
    public String fnr;
    public String aktorId;
    public String veilederId;
    public boolean reservasjonKRR;
    public boolean manuell;
    public boolean underOppfolging;
    public boolean underKvp;
    public boolean kanStarteOppfolging;
    public boolean kanVarsles;
    private EskaleringsvarselEntity gjeldendeEskaleringsvarsel;
    public List<OppfolgingsperiodeEntity> oppfolgingsperioder = Collections.emptyList();
    public List<KvpEntity> kvpPerioder;
    public boolean harSkriveTilgang;
    public Boolean inaktivIArena;
    public Boolean kanReaktiveres;
    public LocalDate inaktiveringsdato;
    public Boolean erSykmeldtMedArbeidsgiver;
    public String servicegruppe;
    public String formidlingsgruppe;
    public String rettighetsgruppe;

    @Deprecated
    public Boolean erIkkeArbeidssokerUtenOppfolging;

    public ZonedDateTime getOppfolgingUtgang() {
        return oppfolgingsperioder.stream().map(OppfolgingsperiodeEntity::getSluttDato).filter(Objects::nonNull).max(naturalOrder()).orElse(null);
    }

}