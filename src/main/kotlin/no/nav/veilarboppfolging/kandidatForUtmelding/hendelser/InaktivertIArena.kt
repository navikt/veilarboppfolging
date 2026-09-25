package no.nav.veilarboppfolging.kandidatForUtmelding.hendelser

import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import no.nav.common.json.JsonUtils
import no.nav.common.types.identer.Fnr
import no.nav.veilarboppfolging.kandidatForUtmelding.filterhendelse.FilterhendelseRecord
import org.postgresql.util.PGobject

class InaktivertIArena(
    oppfolgingsperiodeUuid: UUID,
    val iservFraDato: LocalDate?,
    hendelseTidspunkt: Instant,
) : KandidatForUtmeldingHendelse(
    oppfolgingsperiodeUuid,
    KandidatForUtmeldingHendelseUtfortAvType.SYSTEM,
    "Arena",
    "Arena",
    hendelseTidspunkt,
) {
    override val type: InaktivertIArenaHendelseType = InaktivertIArenaHendelseType.INAKTIVERT_I_ARENA
    override val hendelseDataJson: PGobject? = iservFraDato?.let {
        PGobject().apply {
            type = "jsonb"
            value = JsonUtils.getMapper().writeValueAsString(Detaljer(it))
        }
    }

    data class Detaljer(
        val iservFraDato: LocalDate?
    )

    override fun tilFilterhendelseRecord(fnr: Fnr): FilterhendelseRecord? {
        return null
    }
}