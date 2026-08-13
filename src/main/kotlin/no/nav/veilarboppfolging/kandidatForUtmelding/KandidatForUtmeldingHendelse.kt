package no.nav.veilarboppfolging.kandidatForUtmelding

import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import java.util.UUID
import no.nav.common.json.JsonUtils

sealed class KandidatForUtmeldingHendelse(
    val aktorId: AktorId,
    val fnr: Fnr,
    val oppfolgingsperiodeUuid: UUID,
    val utfortAvType: KandidatForUtmeldingHendelseUtfortAvType,
    val utfortAv: String?,
    val kilde: String,
) {
    abstract val type: KandidatForUtmeldingHendelseType
    abstract val hendelseDataJson: String?

    fun mapTilTag(): KandidatForUtmeldingTag {
        return when (type) {
            KandidatForUtmeldingHendelseType.ARBEIDSSOKERPERIODE_AVSLUTTET_IKKE_LEVERT_MELDEKORT -> KandidatForUtmeldingTag.ARBEIDSSOKERPERIODE_AVSLUTTET_IKKE_LEVERT_MELDEKORT
            KandidatForUtmeldingHendelseType.ARBEIDSSOKERPERIODE_AVSLUTTET_SVARTE_NEI_I_BEKREFTELSE -> KandidatForUtmeldingTag.ARBEIDSSOKERPERIODE_AVSLUTTET_SVARTE_NEI_I_BEKREFTELSE
            KandidatForUtmeldingHendelseType.ARBEIDSSOKERPERIODE_AVSLUTTET_ANNET -> KandidatForUtmeldingTag.ARBEIDSSOKERPERIODE_AVSLUTTET_ANNET
        }
    }
}

enum class KandidatForUtmeldingHendelseType {
    ARBEIDSSOKERPERIODE_AVSLUTTET_IKKE_LEVERT_MELDEKORT,
    ARBEIDSSOKERPERIODE_AVSLUTTET_SVARTE_NEI_I_BEKREFTELSE,
    ARBEIDSSOKERPERIODE_AVSLUTTET_ANNET,
}

enum class KandidatForUtmeldingHendelseUtfortAvType {
    VEILEDER,
    SYSTEM,
    BRUKER,
    UKJENT
}

class ArbeidssøkerPeriodeAvsluttet(
    aktorId: AktorId,
    fnr: Fnr,
    oppfolgingsperiodeUuid: UUID,
    utfortAvType: KandidatForUtmeldingHendelseUtfortAvType,
    utfortAv: String?,
    kilde: String,
    avslutningsarsak: String,
    kandidatForUtmeldingHendelseType: KandidatForUtmeldingHendelseType
) : KandidatForUtmeldingHendelse(
    aktorId,
    fnr,
    oppfolgingsperiodeUuid,
    utfortAvType,
    utfortAv,
    kilde,
) {
    override val type: KandidatForUtmeldingHendelseType = kandidatForUtmeldingHendelseType
    override val hendelseDataJson: String = JsonUtils.getMapper().writeValueAsString(Detaljer(avslutningsarsak))

    data class Detaljer(
        val avslutningsarsak: String
    )
}

