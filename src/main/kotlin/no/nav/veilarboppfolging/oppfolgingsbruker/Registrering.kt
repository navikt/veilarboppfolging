package no.nav.veilarboppfolging.oppfolgingsbruker

import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.NavIdent
import no.nav.pto_schema.enums.arena.Formidlingsgruppe
import no.nav.pto_schema.enums.arena.Kvalifiseringsgruppe
import no.nav.veilarboppfolging.domain.StartetAvType

interface RegistrantFelter {
    fun getRegistrertAv(): String?
    fun getRegistrertAvType(): StartetAvType
}

sealed class Registrant: RegistrantFelter

object BrukerRegistrant : Registrant() {
    override fun getRegistrertAv(): String? = null
    override fun getRegistrertAvType() = StartetAvType.BRUKER
}

object SystemRegistrant : Registrant() {
    override fun getRegistrertAv(): String? = null
    override fun getRegistrertAvType() = StartetAvType.SYSTEM
}

class VeilederRegistrant(
    val navIdent: NavIdent,
) : Registrant() {
    override fun getRegistrertAv(): String? = navIdent.get()
    override fun getRegistrertAvType() = StartetAvType.VEILEDER
}

sealed class OppfolgingsRegistrering(
    open val aktorId: AktorId,
    open val oppfolgingStartBegrunnelse: OppfolgingStartBegrunnelse,
    open val registrant: Registrant,
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
    override val registrant: Registrant,
) : OppfolgingsRegistrering(aktorId, OppfolgingStartBegrunnelse.ARBEIDSSOKER_REGISTRERING, registrant)

data class ManuellRegistrering(
    override val aktorId: AktorId,
    override val registrant: Registrant,
) : OppfolgingsRegistrering(aktorId, OppfolgingStartBegrunnelse.MANUELL_REGISTRERING_VEILEDER, registrant)

data class ArenaSyncRegistrering(
    override val aktorId: AktorId,
    val formidlingsgruppe: Formidlingsgruppe,
    val kvalifiseringsgruppe: Kvalifiseringsgruppe,
): OppfolgingsRegistrering(
    aktorId,
    if (formidlingsgruppe == Formidlingsgruppe.IARBS) OppfolgingStartBegrunnelse.ARENA_SYNC_IARBS
    else OppfolgingStartBegrunnelse.ARENA_SYNC_ARBS,
    SystemRegistrant)