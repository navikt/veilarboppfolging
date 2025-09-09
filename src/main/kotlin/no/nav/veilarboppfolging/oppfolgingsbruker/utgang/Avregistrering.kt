package no.nav.veilarboppfolging.oppfolgingsbruker.utgang

import no.nav.common.types.identer.AktorId
import no.nav.veilarboppfolging.oppfolgingsbruker.Registrant
import no.nav.veilarboppfolging.oppfolgingsbruker.SystemRegistrant
import no.nav.veilarboppfolging.oppfolgingsbruker.VeilederRegistrant
import java.util.UUID

enum class AvregistreringsType {
    UtmeldtEtter28Dager,
    ManuellAvregistrering,
    ArenaIservKanIkkeReaktiveres,
    AdminAvregistrering,
}

sealed class Avregistrering(
    open val aktorId: AktorId,
    open val avsluttetAv: Registrant,
    open val begrunnelse: String,
) {
    abstract fun getAvregistreringsType(): AvregistreringsType
}

data class UtmeldtEtter28Dager(override val aktorId: AktorId) : Avregistrering(aktorId, SystemRegistrant, BEGRUNNELSE) {
    override fun getAvregistreringsType() = AvregistreringsType.UtmeldtEtter28Dager

    companion object {
        const val BEGRUNNELSE = "Oppfølging avsluttet automatisk grunnet iserv i 28 dager"
    }
}

data class ManuellAvregistrering(
    override val aktorId: AktorId,
    val veileder: VeilederRegistrant,
    override val begrunnelse: String
) : Avregistrering(aktorId, veileder, begrunnelse) {
    override fun getAvregistreringsType() = AvregistreringsType.ManuellAvregistrering

    companion object {
    }
}

data class ArenaIservKanIkkeReaktiveres(override val aktorId: AktorId) :
    Avregistrering(aktorId, SystemRegistrant, BEGRUNNELSE) {
    override fun getAvregistreringsType() = AvregistreringsType.ArenaIservKanIkkeReaktiveres

    companion object {
        const val BEGRUNNELSE = "Oppfølging avsluttet automatisk pga. inaktiv bruker som ikke kan reaktiveres"
    }
}

data class AdminAvregistrering(
    override val aktorId: AktorId,
    val veileder: VeilederRegistrant,
    override val begrunnelse: String,
    val oppfolgingsperiodeUUID: UUID
) : Avregistrering(aktorId, veileder, begrunnelse) {
    override fun getAvregistreringsType() = AvregistreringsType.AdminAvregistrering
}