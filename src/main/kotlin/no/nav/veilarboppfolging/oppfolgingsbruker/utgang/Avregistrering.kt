package no.nav.veilarboppfolging.oppfolgingsbruker.utgang

import no.nav.common.types.identer.AktorId
import no.nav.veilarboppfolging.oppfolgingsbruker.Registrant
import no.nav.veilarboppfolging.oppfolgingsbruker.SystemRegistrant
import no.nav.veilarboppfolging.oppfolgingsbruker.VeilederRegistrant

sealed class Avregistrering(
    open val aktorId: AktorId,
    open val avsluttetAv: Registrant,
    open val begrunnelse: String
)

data class UtmeldEtter28Dager(override val aktorId: AktorId) : Avregistrering(aktorId, SystemRegistrant, "Oppfølging avsluttet automatisk grunnet iserv i 28 dager")
data class ManuellAvregistrering(override val aktorId: AktorId, val veileder: VeilederRegistrant, override val begrunnelse: String) : Avregistrering(aktorId, veileder, begrunnelse)
data class ArenaIservKanIkkeReaktiveres(override val aktorId: AktorId): Avregistrering(aktorId, SystemRegistrant, BEGRUNNELSE) {
    companion object {
        const val BEGRUNNELSE = "Oppfølging avsluttet automatisk pga. inaktiv bruker som ikke kan reaktiveres"
    }
}