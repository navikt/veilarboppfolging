package no.nav.veilarboppfolging.domain

import no.nav.veilarboppfolging.repository.entity.KvpPeriodeEntity
import no.nav.veilarboppfolging.repository.entity.OppfolgingsperiodeEntity
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.*

data class OppfolgingStatusData(
    val fnr: String,
    val aktorId: String,
    val veilederId: String? = null,
    val reservasjonKRR: Boolean,
    val registrertKRR: Boolean,
    val manuell: Boolean,
    val underOppfolging: Boolean,
    val underKvp: Boolean,
    val kanStarteOppfolging: Boolean?,
    val kanVarsles: Boolean,
    val oppfolgingsperioder: List<OppfolgingsperiodeEntity>,
    val kvpPerioder: List<KvpPeriodeEntity>,
    val harSkriveTilgang: Boolean,
    val inaktivIArena: Boolean?,
    val kanReaktiveres: Boolean?,
    val inaktiveringsdato: LocalDate?,
    val erSykmeldtMedArbeidsgiver: Boolean?,
    val servicegruppe: String?,
    val formidlingsgruppe: String?,
    val rettighetsgruppe: String?,
    @Deprecated("")
    val erIkkeArbeidssokerUtenOppfolging: Boolean? = null
) {
    val oppfolgingUtgang: ZonedDateTime?
        get() = oppfolgingsperioder.stream().map<ZonedDateTime?>(OppfolgingsperiodeEntity::sluttDato)
            .filter { obj: ZonedDateTime? -> Objects.nonNull(obj) }
            .max(Comparator.naturalOrder()).orElse(null)
}