package no.nav.veilarboppfolging.oppfolgingsbruker

import lombok.EqualsAndHashCode
import no.nav.common.types.identer.AktorId
import no.nav.pto_schema.enums.arena.Formidlingsgruppe
import no.nav.pto_schema.enums.arena.Kvalifiseringsgruppe
import no.nav.veilarboppfolging.domain.StartetAvType

@EqualsAndHashCode(callSuper = true)
data class ArenaSyncOppfolgingsBruker(
    override val aktorId: AktorId,
    val formidlingsgruppe: Formidlingsgruppe,
    val kvalifiseringsgruppe: Kvalifiseringsgruppe,
) : Oppfolgingsbruker(
    aktorId,
    if (formidlingsgruppe == Formidlingsgruppe.IARBS) OppfolgingStartBegrunnelse.ARENA_SYNC_IARBS else OppfolgingStartBegrunnelse.ARENA_SYNC_ARBS,
    StartetAvType.SYSTEM
)
