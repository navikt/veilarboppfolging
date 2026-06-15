package no.nav.veilarboppfolging.controller.response

import no.nav.veilarboppfolging.repository.enums.KodeverkBruker
import java.time.ZonedDateTime

data class HistorikkHendelse(
    val type: HistorikkHendelseType,
    val dato: ZonedDateTime,
    val begrunnelse: String?,
    val opprettetAv: KodeverkBruker?,
    val opprettetAvBrukerId: String?,
    val dialogId: Long?,
    val enhet: String?,
    val tildeltVeilederId: String?,
)

enum class HistorikkHendelseType {
    SATT_TIL_MANUELL,
    SATT_TIL_DIGITAL,
    STARTET_OPPFOLGINGSPERIODE,
    AVSLUTTET_OPPFOLGINGSPERIODE,
    REAKTIVERT_OPPFOLGINGSPERIODE,
    KVP_STARTET,
    KVP_STOPPET,
    VEILEDER_TILORDNET,
    OPPFOLGINGSENHET_ENDRET
}
