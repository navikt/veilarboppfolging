package no.nav.veilarboppfolging.kandidatForUtmelding

import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.paw.arbeidssokerregisteret.api.v1.Aarsaksinformasjon
import no.nav.paw.arbeidssokerregisteret.api.v1.AvsluttetAarsakType
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
            KandidatForUtmeldingHendelseType.ARBEIDSSOKERPERIODE_AVSLUTTET -> KandidatForUtmeldingTag.ARBEIDSSOKERPERIODE_AVSLUTTET_ANNET
        }
    }
}

enum class KandidatForUtmeldingHendelseType {
    @Deprecated("Ikke bruk")
    ARBEIDSSOKERPERIODE_AVSLUTTET,
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
    avsluttetAarsakType: AvsluttetAarsakType,
) : KandidatForUtmeldingHendelse(
    aktorId,
    fnr,
    oppfolgingsperiodeUuid,
    avsluttetAv,
    kilde,
    detaljer = avsluttetAarsakType.toString()
) {
    override val type: KandidatForUtmeldingHendelseType = when (avsluttetAarsakType) {
        AvsluttetAarsakType.SVARTE_NEI_I_BEKREFTELSE -> KandidatForUtmeldingHendelseType.ARBEIDSSOKERPERIODE_AVSLUTTET_SVARTE_NEI_I_BEKREFTELSE
        AvsluttetAarsakType.BEKREFTELSE_IKKE_LEVERT_INNEN_FRIST -> KandidatForUtmeldingHendelseType.ARBEIDSSOKERPERIODE_AVSLUTTET_IKKE_LEVERT_MELDEKORT
        AvsluttetAarsakType.UDEFINERT, AvsluttetAarsakType.UKJENT_VERDI -> KandidatForUtmeldingHendelseType.ARBEIDSSOKERPERIODE_AVSLUTTET_ANNET
    }
}

