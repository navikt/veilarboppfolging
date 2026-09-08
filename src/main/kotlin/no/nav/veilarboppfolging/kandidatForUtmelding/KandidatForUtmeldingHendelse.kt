package no.nav.veilarboppfolging.kandidatForUtmelding

import java.time.Instant
import java.util.UUID
import kotlin.jvm.optionals.getOrElse
import no.nav.common.types.identer.Fnr
import no.nav.common.utils.EnvironmentUtils
import no.nav.veilarboppfolging.kandidatForUtmelding.dto.KandidatForUtmeldingTagDto
import no.nav.veilarboppfolging.kandidatForUtmelding.filterhendelse.FilterhendelseRecord
import org.postgresql.util.PGobject

sealed class KandidatForUtmeldingHendelse(
    val oppfolgingsperiodeUuid: UUID,
    val utfortAvType: KandidatForUtmeldingHendelseUtfortAvType,
    val utfortAv: String?,
    val kilde: String,
    val hendelseTidspunkt: Instant,
) {
    abstract val type: KandidatForUtmeldingHendelseType
    abstract val hendelseDataJson: PGobject?

    abstract fun tilFilterhendelseRecord(fnr: Fnr): FilterhendelseRecord

    private val erProd: Boolean = EnvironmentUtils.isProduction().getOrElse { false }

    fun baseUrlVeilarbpersonflate() =
        if (erProd) "https://veilarbpersonflate.intern.nav.no" else "https://veilarbpersonflate.ansatt.dev.nav.no"

    fun mapTilTag(): KandidatForUtmeldingTagDto? {
        return when (type) {
            ArbeidssokerperiodeAvsluttetHendelseType.ARBEIDSSOKERPERIODE_AVSLUTTET_IKKE_LEVERT_MELDEKORT -> KandidatForUtmeldingTagDto.ARBEIDSSOKERPERIODE_AVSLUTTET_IKKE_LEVERT_MELDEKORT
            ArbeidssokerperiodeAvsluttetHendelseType.ARBEIDSSOKERPERIODE_AVSLUTTET_SVARTE_NEI_I_BEKREFTELSE -> KandidatForUtmeldingTagDto.ARBEIDSSOKERPERIODE_AVSLUTTET_SVARTE_NEI_I_BEKREFTELSE
            ArbeidssokerperiodeAvsluttetHendelseType.ARBEIDSSOKERPERIODE_AVSLUTTET_ANNET -> KandidatForUtmeldingTagDto.ARBEIDSSOKERPERIODE_AVSLUTTET_ANNET
            ForlengelseHendelseType.FORLENGELSE_UTLOPT -> KandidatForUtmeldingTagDto.FORLENGELSE_UTLOPT
            ForlengelseHendelseType.FORLENGELSE_OPPRETTET, ForlengelseHendelseType.FORLENGELSE_ENDRET,
            OppfolgingAvsluttetHendelseType.OPPFOLGING_AVSLUTTET_AUTOMATISK, OppfolgingAvsluttetHendelseType.OPPFOLGING_AVSLUTTET_MANUELT-> null
        }
    }
}

sealed interface KandidatForUtmeldingHendelseType

enum class ArbeidssokerperiodeAvsluttetHendelseType : KandidatForUtmeldingHendelseType {
    ARBEIDSSOKERPERIODE_AVSLUTTET_IKKE_LEVERT_MELDEKORT,
    ARBEIDSSOKERPERIODE_AVSLUTTET_SVARTE_NEI_I_BEKREFTELSE,
    ARBEIDSSOKERPERIODE_AVSLUTTET_ANNET
}

enum class ForlengelseHendelseType : KandidatForUtmeldingHendelseType {
    FORLENGELSE_OPPRETTET,
    FORLENGELSE_ENDRET,
    FORLENGELSE_UTLOPT
}

enum class KandidatForUtmeldingHendelseUtfortAvType {
    VEILEDER,
    SYSTEM,
    BRUKER,
    UKJENT
}
