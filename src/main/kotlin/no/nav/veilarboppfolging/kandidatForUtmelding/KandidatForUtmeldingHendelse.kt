package no.nav.veilarboppfolging.kandidatForUtmelding

import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.veilarboppfolging.oppfolgingsbruker.utgang.AvregistreringsType
import java.time.ZonedDateTime

sealed class KandidatForUtmeldingHendelse (
    val aktorId : AktorId,
    val fnr: Fnr,
    val avsluttetAv: KandidatForUtmeldingHendelseAvsluttetAv,
    val kilde: String,
    val aarsak: String?
) {
    abstract val type: KandidatForUtmeldingHendelseType
}

enum class KandidatForUtmeldingHendelseType {
    ARBEIDSSOKERPERIODE_AVSLUTTET
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
    avsluttetAv: KandidatForUtmeldingHendelseAvsluttetAv,
    kilde: String,
    aarsak: String?
): KandidatForUtmeldingHendelse(aktorId, fnr, avsluttetAv, kilde, aarsak)  {
    override val type: KandidatForUtmeldingHendelseType = KandidatForUtmeldingHendelseType.ARBEIDSSOKERPERIODE_AVSLUTTET
}

data class KandidatForUtmelding(
    val aktorId : AktorId,
    val fnr: Fnr,
    val avsluttetAv: KandidatForUtmeldingHendelseAvsluttetAv,
    val kilde: String,
    val aarsak: String?,
    val avregistreringsType: AvregistreringsType?,
    val oppfolgingAvsluttetTidspunkt: ZonedDateTime?
)