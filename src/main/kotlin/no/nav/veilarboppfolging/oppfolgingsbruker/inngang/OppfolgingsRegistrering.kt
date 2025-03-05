package no.nav.veilarboppfolging.oppfolgingsbruker.inngang

import no.nav.common.types.identer.AktorId
import no.nav.pto_schema.enums.arena.Formidlingsgruppe
import no.nav.pto_schema.enums.arena.Kvalifiseringsgruppe
import no.nav.veilarboppfolging.oppfolgingsbruker.Registrant
import no.nav.veilarboppfolging.oppfolgingsbruker.SystemRegistrant
import no.nav.veilarboppfolging.oppfolgingsbruker.VeilederRegistrant

sealed class OppfolgingsRegistrering(
    open val aktorId: AktorId,
    open val oppfolgingStartBegrunnelse: OppfolgingStartBegrunnelse,
    open val registrertAv: Registrant,
) {
    companion object {
        fun arbeidssokerRegistrering(aktorId: AktorId, registrant: Registrant): ArbeidsokerRegistrering {
            return ArbeidsokerRegistrering(aktorId, registrant)
        }
        fun manueltRegistrertBruker(aktorId: AktorId, veileder: VeilederRegistrant): ManuellRegistrering {
            return ManuellRegistrering(aktorId, veileder)
        }
        fun arenaSyncOppfolgingBruker(aktorId: AktorId, formidlingsgruppe: Formidlingsgruppe, kvalifiseringsgruppe: Kvalifiseringsgruppe): ArenaSyncRegistrering {
            return ArenaSyncRegistrering(aktorId, formidlingsgruppe, kvalifiseringsgruppe)
        }
    }
}

data class ArbeidsokerRegistrering(
    override val aktorId: AktorId,
    override val registrertAv: Registrant,
) : OppfolgingsRegistrering(aktorId, OppfolgingStartBegrunnelse.ARBEIDSSOKER_REGISTRERING, registrertAv)

data class ManuellRegistrering(
    override val aktorId: AktorId,
    override val registrertAv: Registrant,
) : OppfolgingsRegistrering(aktorId, OppfolgingStartBegrunnelse.MANUELL_REGISTRERING_VEILEDER, registrertAv)

data class ArenaSyncRegistrering(
    override val aktorId: AktorId,
    val formidlingsgruppe: Formidlingsgruppe,
    val kvalifiseringsgruppe: Kvalifiseringsgruppe,
): OppfolgingsRegistrering(
    aktorId,
    if (formidlingsgruppe == Formidlingsgruppe.IARBS) OppfolgingStartBegrunnelse.ARENA_SYNC_IARBS
    else OppfolgingStartBegrunnelse.ARENA_SYNC_ARBS,
    SystemRegistrant
)