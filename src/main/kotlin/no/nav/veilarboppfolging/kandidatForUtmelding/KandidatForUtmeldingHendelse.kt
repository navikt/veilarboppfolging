package no.nav.veilarboppfolging.kandidatForUtmelding

import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.veilarboppfolging.kandidatForUtmelding.KandidatForUtmeldingTag.ARBEIDSSOKERPERIODE_AVSLUTTET_BRUKER
import no.nav.veilarboppfolging.kandidatForUtmelding.KandidatForUtmeldingTag.ARBEIDSSOKERPERIODE_AVSLUTTET_SYSTEM
import no.nav.veilarboppfolging.kandidatForUtmelding.KandidatForUtmeldingTag.ARBEIDSSOKERPERIODE_AVSLUTTET_UKJENT
import no.nav.veilarboppfolging.kandidatForUtmelding.KandidatForUtmeldingTag.ARBEIDSSOKERPERIODE_AVSLUTTET_VEILEDER
import java.util.UUID

sealed class KandidatForUtmeldingHendelse(
    val aktorId : AktorId,
    val fnr: Fnr,
    val oppfolgingsperiodeUuid: UUID,
    val avsluttetAv: KandidatForUtmeldingHendelseAvsluttetAv,
    val kilde: String,
    val detaljer: String?,
) {
    abstract val type: KandidatForUtmeldingHendelseType

    fun mapTilTag(): KandidatForUtmeldingTag {
        return when (type) {
            KandidatForUtmeldingHendelseType.ARBEIDSSOKERPERIODE_AVSLUTTET -> {
                when (avsluttetAv) {
                    KandidatForUtmeldingHendelseAvsluttetAv.BRUKER -> ARBEIDSSOKERPERIODE_AVSLUTTET_BRUKER
                    KandidatForUtmeldingHendelseAvsluttetAv.VEILEDER -> ARBEIDSSOKERPERIODE_AVSLUTTET_VEILEDER
                    KandidatForUtmeldingHendelseAvsluttetAv.SYSTEM -> ARBEIDSSOKERPERIODE_AVSLUTTET_SYSTEM
                    KandidatForUtmeldingHendelseAvsluttetAv.UKJENT -> ARBEIDSSOKERPERIODE_AVSLUTTET_UKJENT
                }
            }
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

sealed class ArbeidssøkerPeriodeAvsluttet(
    aktorId: AktorId,
    fnr: Fnr,
    oppfolgingsperiodeUuid: UUID,
    avsluttetAv: KandidatForUtmeldingHendelseAvsluttetAv,
    kilde: String,
    detaljer: String?
): KandidatForUtmeldingHendelse(aktorId, fnr, oppfolgingsperiodeUuid, avsluttetAv, kilde, detaljer)

class ArbeidssøkerPeriodeAvsluttetIkkeLevertMeldekort(
    aktorId: AktorId,
    fnr: Fnr,
    oppfolgingsperiodeUuid: UUID,
    avsluttetAv: KandidatForUtmeldingHendelseAvsluttetAv,
    kilde: String,
    detaljer: String?
): ArbeidssøkerPeriodeAvsluttet(aktorId, fnr, oppfolgingsperiodeUuid, avsluttetAv, kilde, detaljer) {
    override val type: KandidatForUtmeldingHendelseType = KandidatForUtmeldingHendelseType.ARBEIDSSOKERPERIODE_AVSLUTTET_IKKE_LEVERT_MELDEKORT
}