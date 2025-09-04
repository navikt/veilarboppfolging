package no.nav.veilarboppfolging.oppfolgingsbruker.inngang

import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.EnhetId
import no.nav.common.types.identer.Fnr
import no.nav.pto_schema.enums.arena.Formidlingsgruppe
import no.nav.pto_schema.enums.arena.Kvalifiseringsgruppe
import no.nav.veilarboppfolging.oppfolgingsbruker.Registrant
import no.nav.veilarboppfolging.oppfolgingsbruker.SystemRegistrant
import no.nav.veilarboppfolging.oppfolgingsbruker.VeilederRegistrant

sealed class OppfolgingsRegistrering(
    open val fnr: Fnr,
    open val aktorId: AktorId,
    open val oppfolgingStartBegrunnelse: OppfolgingStartBegrunnelse,
    open val registrertAv: Registrant,
) {
    companion object {
        fun arbeidssokerRegistrering(fnr: Fnr, aktorId: AktorId, registrant: Registrant): ArbeidsokerRegistrering {
            return ArbeidsokerRegistrering(fnr, aktorId, registrant)
        }
        fun manuellRegistrering(fnr: Fnr, aktorId: AktorId, veileder: VeilederRegistrant, kontorSattAvVeileder: String? = null): ManuellRegistrering {
            return ManuellRegistrering(fnr, aktorId, veileder, kontorSattAvVeileder)
        }
        fun arenaSyncOppfolgingBrukerRegistrering(fnr: Fnr, aktorId: AktorId, formidlingsgruppe: Formidlingsgruppe, kvalifiseringsgruppe: Kvalifiseringsgruppe, enhet: EnhetId): ArenaSyncRegistrering {
            return ArenaSyncRegistrering(fnr, aktorId, formidlingsgruppe, kvalifiseringsgruppe, enhet)
        }
    }
}

data class ArbeidsokerRegistrering(
    override val fnr: Fnr,
    override val aktorId: AktorId,
    override val registrertAv: Registrant,
) : OppfolgingsRegistrering(fnr, aktorId, OppfolgingStartBegrunnelse.ARBEIDSSOKER_REGISTRERING, registrertAv)

data class ManuellRegistrering(
    override val fnr: Fnr,
    override val aktorId: AktorId,
    override val registrertAv: Registrant,
    val kontorSattAvVeileder: String?,
) : OppfolgingsRegistrering(fnr, aktorId, OppfolgingStartBegrunnelse.MANUELL_REGISTRERING_VEILEDER, registrertAv)

data class ArenaSyncRegistrering(
    override val fnr: Fnr,
    override val aktorId: AktorId,
    val formidlingsgruppe: Formidlingsgruppe,
    val kvalifiseringsgruppe: Kvalifiseringsgruppe,
    val enhet: EnhetId
): OppfolgingsRegistrering(
    fnr,
    aktorId,
    if (formidlingsgruppe == Formidlingsgruppe.IARBS) OppfolgingStartBegrunnelse.ARENA_SYNC_IARBS
    else OppfolgingStartBegrunnelse.ARENA_SYNC_ARBS,
    SystemRegistrant
)