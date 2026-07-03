package no.nav.veilarboppfolging.kandidatForUtmelding

import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import java.util.UUID

sealed class KandidatForUtmeldingHendelse(
    val aktorId: AktorId,
    val fnr: Fnr,
    val oppfolgingsperiodeUuid: UUID,
    val avsluttetAv: KandidatForUtmeldingHendelseAvsluttetAv,
    val kilde: String,
    val detaljer: String?,
) {
    abstract val type: KandidatForUtmeldingHendelseType

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

enum class KandidatForUtmeldingHendelseAvsluttetAv {
    VEILEDER,
    SYSTEM,
    BRUKER,
    UKJENT
}

class ArbeidssøkerPeriodeAvsluttet(
    aktorId: AktorId,
    fnr: Fnr,
    oppfolgingsperiodeUuid: UUID,
    avsluttetAv: KandidatForUtmeldingHendelseAvsluttetAv,
    kilde: String,
    detaljer: String,
    kandidatForUtmeldingHendelseType: KandidatForUtmeldingHendelseType
) : KandidatForUtmeldingHendelse(
    aktorId,
    fnr,
    oppfolgingsperiodeUuid,
    avsluttetAv,
    kilde,
    detaljer
) {
    override val type: KandidatForUtmeldingHendelseType = kandidatForUtmeldingHendelseType
}

