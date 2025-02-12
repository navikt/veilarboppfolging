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
    open val registrertAv: NavIdent? = null
) {

    companion object {
        @JvmStatic
        fun manueltRegistrertBruker(aktorId: AktorId, navIdent: NavIdent): Oppfolgingsbruker {
            return BrukerManueltRegistrertAvVeileder(aktorId, navIdent)
        }

        @JvmStatic
        fun arbeidssokerStartetAvVeileder(
            aktorId: AktorId,
            startetAvType: StartetAvType,
            navIdent: NavIdent? = null
        ): Oppfolgingsbruker {
            return Arbeidssoker(
                aktorId,
                OppfolgingStartBegrunnelse.ARBEIDSSOKER_REGISTRERING,
                startetAvType,
                navIdent
            )
        }

        @JvmStatic
        fun arbeidssokerStartetAvBruker(
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
    override val aktorId: AktorId,
    override val registrertAv: NavIdent
): Oppfolgingsbruker(aktorId, OppfolgingStartBegrunnelse.MANUELL_REGISTRERING_VEILEDER, StartetAvType.VEILEDER, registrertAv)

data class Arbeidssoker(
    override val aktorId: AktorId,
    override val oppfolgingStartBegrunnelse: OppfolgingStartBegrunnelse,
    override val startetAvType: StartetAvType,
    override val registrertAv: NavIdent? = null
) : Oppfolgingsbruker(
    aktorId,
    oppfolgingStartBegrunnelse,
    startetAvType,
    if (startetAvType == StartetAvType.VEILEDER) registrertAv else null
)

