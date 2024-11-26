package no.nav.veilarboppfolging.oppfolgingsbruker

import lombok.AllArgsConstructor
import lombok.EqualsAndHashCode
import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.NavIdent
import no.nav.pto_schema.enums.arena.Formidlingsgruppe
import no.nav.pto_schema.enums.arena.Kvalifiseringsgruppe
import no.nav.veilarboppfolging.domain.StartetAvType

@AllArgsConstructor
@EqualsAndHashCode
open class Oppfolgingsbruker(
    open val aktorId: AktorId,
    open val oppfolgingStartBegrunnelse: OppfolgingStartBegrunnelse,
    open val startetAvType: StartetAvType,
) {

    companion object {
        @JvmStatic
        fun manuelRegistrertBruker(aktorId: AktorId, navIdent: NavIdent): Oppfolgingsbruker {
            return BrukerManueltRegistrertAvVeileder(navIdent, aktorId)
        }

        @JvmStatic
        fun arbeidssokerOppfolgingsBruker(
            aktorId: AktorId,
            startetAvType: StartetAvType
        ): Oppfolgingsbruker {
            return Arbeidssoker(
                aktorId,
                OppfolgingStartBegrunnelse.ARBEIDSSOKER_REGISTRERING,
                startetAvType
            )
        }

        @JvmStatic
        fun arenaSyncOppfolgingBruker(
            aktorId: AktorId,
            formidlingsgruppe: Formidlingsgruppe,
            kvalifiseringsgruppe: Kvalifiseringsgruppe
        ): Oppfolgingsbruker {
            check(formidlingsgruppe != Formidlingsgruppe.ISERV) { "ISERV skal ikke starte oppfølging" }
            return ArenaSyncOppfolgingsBruker(aktorId, formidlingsgruppe, kvalifiseringsgruppe)
        }
    }
}

data class BrukerManueltRegistrertAvVeileder(
    val registrertAv: NavIdent,
    override val aktorId: AktorId,
): Oppfolgingsbruker(aktorId, OppfolgingStartBegrunnelse.MANUELL_REGISTRERING_VEILEDER, StartetAvType.VEILEDER)

data class Arbeidssoker(
    override val aktorId: AktorId,
    override val oppfolgingStartBegrunnelse: OppfolgingStartBegrunnelse,
    override val startetAvType: StartetAvType
) : Oppfolgingsbruker(aktorId, oppfolgingStartBegrunnelse, startetAvType)

