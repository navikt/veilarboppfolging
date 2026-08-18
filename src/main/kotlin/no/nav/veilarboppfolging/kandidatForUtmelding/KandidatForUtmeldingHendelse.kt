package no.nav.veilarboppfolging.kandidatForUtmelding

import java.net.URI
import java.time.ZonedDateTime
import java.util.UUID
import kotlin.jvm.optionals.getOrElse
import no.nav.common.json.JsonUtils
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
) {
    abstract val type: KandidatForUtmeldingHendelseType
    abstract val hendelseDataJson: PGobject?

    abstract fun tilFilterhendelseRecord(fnr: Fnr, operasjon: Operasjon): FilterhendelseRecord

    private val erProd: Boolean = EnvironmentUtils.isProduction().getOrElse { false }

    fun baseUrlVeilarbpersonflate() =
        if (erProd) "https://veilarbpersonflate.intern.nav.no" else "https://veilarbpersonflate.ansatt.dev.nav.no"

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

enum class KandidatForUtmeldingHendelseUtfortAvType {
    VEILEDER,
    SYSTEM,
    BRUKER,
    UKJENT
}

class ArbeidssøkerPeriodeAvsluttet(
    oppfolgingsperiodeUuid: UUID,
    utfortAvType: KandidatForUtmeldingHendelseUtfortAvType,
    utfortAv: String?,
    kilde: String,
    kandidatForUtmeldingHendelseType: KandidatForUtmeldingHendelseType,
    val avslutningsarsak: String?
) : KandidatForUtmeldingHendelse(
    oppfolgingsperiodeUuid,
    utfortAvType,
    utfortAv,
    kilde,
) {
    override val type: KandidatForUtmeldingHendelseType = kandidatForUtmeldingHendelseType
    override val hendelseDataJson: PGobject? = avslutningsarsak?.let {
        PGobject().apply {
            type = "jsonb"
            value = JsonUtils.getMapper().writeValueAsString(Detaljer(it))
        }
    }

    data class Detaljer(
        val avslutningsarsak: String?
    )

    override fun tilFilterhendelseRecord(fnr: Fnr, operasjon: Operasjon): FilterhendelseRecord {
        return FilterhendelseRecord(
            personID = NorskIdent(fnr.get()),
            kategori = Kategori.KANDIDAT_FOR_UTMELDING,
            operasjon = operasjon,
            hendelse = FilterhendelseRecord.HendelseInnhold(
                beskrivelse = when (type) {
                    KandidatForUtmeldingHendelseType.ARBEIDSSOKERPERIODE_AVSLUTTET_IKKE_LEVERT_MELDEKORT -> "Arbeidssøkerperiode avsluttet: Ikke levert meldekort"
                    KandidatForUtmeldingHendelseType.ARBEIDSSOKERPERIODE_AVSLUTTET_SVARTE_NEI_I_BEKREFTELSE -> "Arbeidssøkerperiode avsluttet: Svarte nei i bekreftelse"
                    KandidatForUtmeldingHendelseType.ARBEIDSSOKERPERIODE_AVSLUTTET_ANNET -> "Arbeidssøkerperiode avsluttet"
                },
                beskrivelseEnum = when (type) {
                    KandidatForUtmeldingHendelseType.ARBEIDSSOKERPERIODE_AVSLUTTET_IKKE_LEVERT_MELDEKORT -> BeskrivelseEnum.ARBEIDSSOKERPERIODE_AVSLUTTET_IKKE_LEVERT_MELDEKORT
                    KandidatForUtmeldingHendelseType.ARBEIDSSOKERPERIODE_AVSLUTTET_SVARTE_NEI_I_BEKREFTELSE -> BeskrivelseEnum.ARBEIDSSOKERPERIODE_AVSLUTTET_SVARTE_NEI_I_BEKREFTELSE
                    KandidatForUtmeldingHendelseType.ARBEIDSSOKERPERIODE_AVSLUTTET_ANNET -> BeskrivelseEnum.ARBEIDSSOKERPERIODE_AVSLUTTET_ANNET
                }.name,
                dato = ZonedDateTime.now(),
                lenke = URI("${baseUrlVeilarbpersonflate()}/aktivitetsplan").toURL(),
                detaljer = null,
            )
        )
    }
}

