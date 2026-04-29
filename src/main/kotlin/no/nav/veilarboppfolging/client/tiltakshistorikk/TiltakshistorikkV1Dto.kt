package no.nav.veilarboppfolging.client.tiltakshistorikk

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.time.LocalDate
import java.util.UUID

/**
 * Hentet fra https://github.com/navikt/mulighetsrommet/blob/main/common/tiltakshistorikk-client/src/main/kotlin/no/nav/tiltak/historikk/TiltakshistorikkV1Dto.kt
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = TiltakshistorikkV1Dto.ArenaDeltakelse::class, name = "ArenaDeltakelse"),
    JsonSubTypes.Type(value = TiltakshistorikkV1Dto.TeamKometDeltakelse::class, name = "TeamKometDeltakelse"),
    JsonSubTypes.Type(value = TiltakshistorikkV1Dto.TeamTiltakAvtale::class, name = "TeamTiltakAvtale"),
)
sealed class TiltakshistorikkV1Dto {
    /**
     * Id på deltakelse fra kildesystemet.
     *
     * MERK: Hvis kildesystemet er Arena så vil dette være en id som kun er kjent i `tiltakshistorikk`,
     * id fra Arena er tilgjengelig i feltet [TiltakshistorikkV1Dto.ArenaDeltakelse.arenaId].
     */
    abstract val id: UUID

    /**
     * Hvilket kildesystem deltakelsen kommer fra.
     */
    abstract val opphav: Opphav

    /**
     * Startdato i tiltaket.
     */
    abstract val startDato: LocalDate?

    /**
     * Sluttdato i tiltaket.
     */
    abstract val sluttDato: LocalDate?

    /**
     * Beskrivende tittel/leslig navn for tiltaksdeltakelsen.
     *
     * Dette vises bl.a. til veileder i Modia og til bruker i aktivitetsplanen (for noen tiltak), og vil typisk være på
     * formatet "<tiltakstype> hos <arrangør>", f.eks. "Oppfølging hos Arrangør AS".
     *
     * Selve innholdet/oppbygning av tittelen kan variere mellom de forskjellige tiltakstypene og det kan komme
     * endringer i logikken på hvordan dette utledes.
     */
    abstract val tittel: String

    abstract fun erAktiv(): Boolean

    enum class Opphav {
        ARENA,
        TEAM_KOMET,
        TEAM_TILTAK,
    }

    data class ArenaDeltakelse(
        override val startDato: LocalDate?,
        override val sluttDato: LocalDate?,
        override val id: UUID,
        override val tittel: String,
        val arenaId: Int,
        val status: ArenaDeltakerStatusDto,
    ) : TiltakshistorikkV1Dto() {
        override val opphav = Opphav.ARENA

        override fun erAktiv(): Boolean {
            return status.erAktiv()
        }
    }

    data class TeamKometDeltakelse(
        override val startDato: LocalDate?,
        override val sluttDato: LocalDate?,
        override val id: UUID,
        override val tittel: String,
        val status: KometDeltakerStatusDto,
    ) : TiltakshistorikkV1Dto() {
        override val opphav = Opphav.TEAM_KOMET

        override fun erAktiv(): Boolean {
            return status.erAktiv()
        }
    }

    data class TeamTiltakAvtale(
        override val startDato: LocalDate?,
        override val sluttDato: LocalDate?,
        override val id: UUID,
        override val tittel: String,
        val status: ArbeidsgiverAvtaleStatusDto,
    ) : TiltakshistorikkV1Dto() {
        override val opphav = Opphav.TEAM_TILTAK

        override fun erAktiv(): Boolean {
            return status.erAktiv()
        }
    }
}
