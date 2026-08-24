package no.nav.veilarboppfolging.kandidatForUtmelding

import java.net.URI
import java.time.Instant
import java.time.ZoneId
import java.util.UUID
import kotlin.jvm.optionals.getOrElse
import no.nav.common.types.identer.Fnr
import no.nav.common.types.identer.NorskIdent
import no.nav.common.utils.EnvironmentUtils
import no.nav.veilarboppfolging.kandidatForUtmelding.filterhendelse.BeskrivelseEnum
import no.nav.veilarboppfolging.kandidatForUtmelding.filterhendelse.FilterhendelseRecord
import no.nav.veilarboppfolging.kandidatForUtmelding.filterhendelse.Kategori
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
    ARBEIDSSOKERPERIODE_AVSLUTTET_IKKE_LEVERT_MELDEKORT,
    ARBEIDSSOKERPERIODE_AVSLUTTET_SVARTE_NEI_I_BEKREFTELSE,
    ARBEIDSSOKERPERIODE_AVSLUTTET_ANNET,
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

class ForlengelseHendelse(
    oppfolgingsperiodeUuid: UUID,
    utfortAvType: KandidatForUtmeldingHendelseUtfortAvType,
    utfortAv: String?,
    kilde: String,
    val forlengelseHendelseType: KandidatForUtmeldingHendelseType,
    hendelseTidspunkt: Instant,
) : KandidatForUtmeldingHendelse(
    oppfolgingsperiodeUuid,
    utfortAvType,
    utfortAv,
    kilde,
    hendelseTidspunkt,
) {
    override val type: KandidatForUtmeldingHendelseType = forlengelseHendelseType
    override val hendelseDataJson: PGobject? = null

    override fun tilFilterhendelseRecord(fnr: Fnr, operasjon: Operasjon): FilterhendelseRecord {
        return FilterhendelseRecord(
            personID = NorskIdent(fnr.get()),
            kategori = Kategori.KANDIDAT_FOR_UTMELDING,
            operasjon = operasjon,
            hendelse = FilterhendelseRecord.HendelseInnhold(
                beskrivelse = when (type) {
                    KandidatForUtmeldingHendelseType.FORLENGELSE_UTLOPT -> "Forlengelse utløpt"
                    else -> throw IllegalArgumentException("Ugyldig forlengelseshendelsestype for filterhendelser")
                },
                beskrivelseEnum = when (type) {
                    KandidatForUtmeldingHendelseType.FORLENGELSE_UTLOPT -> BeskrivelseEnum.FORLENGELSE_UTLOPT
                    else -> throw IllegalArgumentException("Ugyldig forlengelseshendelsestype for filterhendelser")
                }.name,
                dato = hendelseTidspunkt.atZone(ZoneId.of("Europe/Oslo")),
                lenke = URI("${baseUrlVeilarbpersonflate()}/aktivitetsplan").toURL(),
                detaljer = null,
            )
        )
    }
}
)

