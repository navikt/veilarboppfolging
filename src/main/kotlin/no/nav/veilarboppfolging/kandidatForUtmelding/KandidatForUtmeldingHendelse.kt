package no.nav.veilarboppfolging.kandidatForUtmelding

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.UUID
import kotlin.jvm.optionals.getOrElse
import no.nav.common.types.identer.Fnr
import no.nav.common.utils.EnvironmentUtils
import no.nav.veilarboppfolging.kandidatForUtmelding.dto.KandidatForUtmeldingTagDto
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

    /**
     * Datoen kandidaten skal avsluttes automatisk fra oppfølging, gitt denne hendelsen.
     * Kandidaten skal avsluttes automatisk [KARENSTID_DAGER] dager etter at de først ble kandidat for utmelding,
     * eller [KARENSTID_DAGER] dager etter at en eventuell forlengelse utløp.
     * Mens en forlengelse er aktiv (FORLENGELSE_OPPRETTET/FORLENGELSE_ENDRET) er datoen pauset (null).
     */
    fun beregnAvsluttesAutomatiskDato(): LocalDateTime? {
        val hendelseTid = LocalDateTime.ofInstant(hendelseTidspunkt, ZoneOffset.UTC)
        return when (this) {
            is ArbeidssøkerPeriodeAvsluttet -> hendelseTid.plusDays(KARENSTID_DAGER)
            is ForlengelseHendelse -> when (type) {
                ForlengelseHendelseType.FORLENGELSE_OPPRETTET,
                ForlengelseHendelseType.FORLENGELSE_ENDRET -> null
                ForlengelseHendelseType.FORLENGELSE_UTLOPT -> hendelseTid.plusDays(KARENSTID_DAGER)
            }
            is OppfolgingAvsluttetHendelse -> when (type) {
                OppfolgingAvsluttetHendelseType.OPPFOLGING_AVSLUTTET_AUTOMATISK,
                OppfolgingAvsluttetHendelseType.OPPFOLGING_AVSLUTTET_MANUELT -> null
            }
        }
    }

    /**
     * Samme dato som [beregnAvsluttesAutomatiskDato], men som [ZonedDateTime] i norsk tidssone.
     * Brukes som datoFrist i filterhendelser sendt til OBO.
     */
    fun beregnAvsluttesAutomatiskDatoZonedDateTime(): ZonedDateTime? =
        beregnAvsluttesAutomatiskDato()?.atZone(ZoneOffset.UTC)?.withZoneSameInstant(ZoneId.of("Europe/Oslo"))

    companion object {
        const val KARENSTID_DAGER = 28L
    }

    abstract fun tilFilterhendelseRecord(fnr: Fnr, operasjon: Operasjon): FilterhendelseRecord

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
