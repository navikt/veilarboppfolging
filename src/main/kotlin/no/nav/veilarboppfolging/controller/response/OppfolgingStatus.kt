package no.nav.veilarboppfolging.controller.response

import java.time.LocalDate
import java.time.ZonedDateTime


class OppfolgingStatus(
    val fnr: String,
    val aktorId: String,
    val veilederId: String?,
    val reservasjonKRR: Boolean,
    val registrertKRR: Boolean,
    val kanVarsles: Boolean,
    val manuell: Boolean,
    val underOppfolging: Boolean,
    val underKvp: Boolean,
    val oppfolgingUtgang: ZonedDateTime?,
    val kanStarteOppfolging: Boolean?,
    @Deprecated("")
    val avslutningStatus: AvslutningsStatusDto? = null,
    val oppfolgingsPerioder: List<OppfolgingPeriodeDTO>,
    val harSkriveTilgang: Boolean?,
    val inaktivIArena: Boolean?,
    val kanReaktiveres: Boolean,
    var inaktiveringsdato: LocalDate? = null,
    /*
    * Får treff i "mulighetsrommet", "arbopp", "arbopp-new"
    * */
    val erSykmeldtMedArbeidsgiver: Boolean,
    val servicegruppe: String,
    val formidlingsgruppe: String,
    val rettighetsgruppe: String,
)