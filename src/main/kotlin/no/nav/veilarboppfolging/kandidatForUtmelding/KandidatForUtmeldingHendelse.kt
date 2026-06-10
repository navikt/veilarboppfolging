package no.nav.veilarboppfolging.kandidatForUtmelding

import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.veilarboppfolging.oppfolgingsbruker.utgang.AvregistreringsType
import java.time.ZonedDateTime
import java.util.UUID

sealed class KandidatForUtmeldingHendelse (
    val aktorId : AktorId,
    val fnr: Fnr,
    val oppfolgingsperiodeUuid: UUID?, // TODO: Gjør not nullable
    val avsluttetAv: KandidatForUtmeldingHendelseAvsluttetAv,
    val kilde: String,
    val aarsak: String?,
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
    oppfolgingsperiodeUuid: UUID?,
    avsluttetAv: KandidatForUtmeldingHendelseAvsluttetAv,
    kilde: String,
    aarsak: String?
): KandidatForUtmeldingHendelse(aktorId, fnr, oppfolgingsperiodeUuid, avsluttetAv, kilde, aarsak)  {
    override val type: KandidatForUtmeldingHendelseType = KandidatForUtmeldingHendelseType.ARBEIDSSOKERPERIODE_AVSLUTTET
}
