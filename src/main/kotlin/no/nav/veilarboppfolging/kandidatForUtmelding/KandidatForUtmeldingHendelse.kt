package no.nav.veilarboppfolging.kandidatForUtmelding

import java.time.Instant
import java.util.UUID
import kotlin.jvm.optionals.getOrElse
import no.nav.common.types.identer.Fnr
import no.nav.common.utils.EnvironmentUtils
import no.nav.veilarboppfolging.kandidatForUtmelding.filterhendelse.FilterhendelseRecord
import no.nav.veilarboppfolging.kandidatForUtmelding.filterhendelse.Operasjon
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

    abstract fun tilFilterhendelseRecord(fnr: Fnr, operasjon: Operasjon): FilterhendelseRecord

    private val erProd: Boolean = EnvironmentUtils.isProduction().getOrElse { false }

    fun baseUrlVeilarbpersonflate() =
        if (erProd) "https://veilarbpersonflate.intern.nav.no" else "https://veilarbpersonflate.ansatt.dev.nav.no"

    fun mapTilTag(): KandidatForUtmeldingTag? {
        return when (type) {
            KandidatForUtmeldingHendelseType.ARBEIDSSOKERPERIODE_AVSLUTTET_IKKE_LEVERT_MELDEKORT -> KandidatForUtmeldingTag.ARBEIDSSOKERPERIODE_AVSLUTTET_IKKE_LEVERT_MELDEKORT
            KandidatForUtmeldingHendelseType.ARBEIDSSOKERPERIODE_AVSLUTTET_SVARTE_NEI_I_BEKREFTELSE -> KandidatForUtmeldingTag.ARBEIDSSOKERPERIODE_AVSLUTTET_SVARTE_NEI_I_BEKREFTELSE
            KandidatForUtmeldingHendelseType.ARBEIDSSOKERPERIODE_AVSLUTTET_ANNET -> KandidatForUtmeldingTag.ARBEIDSSOKERPERIODE_AVSLUTTET_ANNET
            KandidatForUtmeldingHendelseType.FORLENGELSE_UTLOPT -> KandidatForUtmeldingTag.FORLENGELSE_UTLOPT
            KandidatForUtmeldingHendelseType.FORLENGELSE_OPPRETTET, KandidatForUtmeldingHendelseType.FORLENGELSE_ENDRET -> null
        }
    }
}

enum class KandidatForUtmeldingHendelseType {
    ;

    enum class ArbeidssokerperiodeAvsluttetHendelseType {
        ARBEIDSSOKERPERIODE_AVSLUTTET_IKKE_LEVERT_MELDEKORT,
        ARBEIDSSOKERPERIODE_AVSLUTTET_SVARTE_NEI_I_BEKREFTELSE,
        ARBEIDSSOKERPERIODE_AVSLUTTET_ANNET
    }
    enum class ForlengelseHendelseType {
        FORLENGELSE_OPPRETTET,
        FORLENGELSE_ENDRET,
        FORLENGELSE_UTLOPT
    }
}

enum class KandidatForUtmeldingHendelseUtfortAvType {
    VEILEDER,
    SYSTEM,
    BRUKER,
    UKJENT
}

)

