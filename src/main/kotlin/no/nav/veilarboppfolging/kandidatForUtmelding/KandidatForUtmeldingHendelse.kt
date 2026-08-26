package no.nav.veilarboppfolging.kandidatForUtmelding

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
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
    // Persistert verdi ved lesing fra DB. Ved skriving beregnes verdien av repositoryet via [beregnAvsluttesAutomatiskDato].
    val avsluttesAutomatiskDato: LocalDateTime? = null,
) {
    abstract val type: KandidatForUtmeldingHendelseType
    abstract val hendelseDataJson: PGobject?

    /**
     * Datoen kandidaten skal avsluttes automatisk fra oppfølging, gitt denne hendelsen.
     * Kandidaten skal avsluttes automatisk [KARENSTID_DAGER] dager etter at de først ble kandidat for utmelding,
     * eller [KARENSTID_DAGER] dager etter at en eventuell forlengelse utløp.
     * Mens en forlengelse er aktiv (FORLENGELSE_OPPRETTET/FORLENGELSE_ENDRET) er datoen pauset (null).
     */
    fun beregnAvsluttesAutomatiskDato(): LocalDateTime? {
        val hendelseTid = LocalDateTime.ofInstant(hendelseTidspunkt, ZoneOffset.UTC)
        return when (val t = type) {
            is ArbeidssokerperiodeAvsluttetHendelseType -> hendelseTid.plusDays(KARENSTID_DAGER)
            is ForlengelseHendelseType -> when (t) {
                ForlengelseHendelseType.FORLENGELSE_OPPRETTET,
                ForlengelseHendelseType.FORLENGELSE_ENDRET -> null
                ForlengelseHendelseType.FORLENGELSE_UTLOPT -> hendelseTid.plusDays(KARENSTID_DAGER)
            }
        }
    }

    companion object {
        const val KARENSTID_DAGER = 28L
    }

    abstract fun tilFilterhendelseRecord(fnr: Fnr, operasjon: Operasjon): FilterhendelseRecord

    private val erProd: Boolean = EnvironmentUtils.isProduction().getOrElse { false }

    fun baseUrlVeilarbpersonflate() =
        if (erProd) "https://veilarbpersonflate.intern.nav.no" else "https://veilarbpersonflate.ansatt.dev.nav.no"

    fun mapTilTag(): KandidatForUtmeldingTag? {
        return when (type) {
            ArbeidssokerperiodeAvsluttetHendelseType.ARBEIDSSOKERPERIODE_AVSLUTTET_IKKE_LEVERT_MELDEKORT -> KandidatForUtmeldingTag.ARBEIDSSOKERPERIODE_AVSLUTTET_IKKE_LEVERT_MELDEKORT
            ArbeidssokerperiodeAvsluttetHendelseType.ARBEIDSSOKERPERIODE_AVSLUTTET_SVARTE_NEI_I_BEKREFTELSE -> KandidatForUtmeldingTag.ARBEIDSSOKERPERIODE_AVSLUTTET_SVARTE_NEI_I_BEKREFTELSE
            ArbeidssokerperiodeAvsluttetHendelseType.ARBEIDSSOKERPERIODE_AVSLUTTET_ANNET -> KandidatForUtmeldingTag.ARBEIDSSOKERPERIODE_AVSLUTTET_ANNET
            ForlengelseHendelseType.FORLENGELSE_UTLOPT -> KandidatForUtmeldingTag.FORLENGELSE_UTLOPT
            ForlengelseHendelseType.FORLENGELSE_OPPRETTET, ForlengelseHendelseType.FORLENGELSE_ENDRET -> null
        }
    }
}

sealed interface KandidatForUtmeldingHendelseType

enum class ArbeidssokerperiodeAvsluttetHendelseType : KandidatForUtmeldingHendelseType {
    ARBEIDSSOKERPERIODE_AVSLUTTET_IKKE_LEVERT_MELDEKORT,
    ARBEIDSSOKERPERIODE_AVSLUTTET_SVARTE_NEI_I_BEKREFTELSE,
    ARBEIDSSOKERPERIODE_AVSLUTTET_ANNET
}

enum class ForlengelseHendelseType : KandidatForUtmeldingHendelseType{
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
