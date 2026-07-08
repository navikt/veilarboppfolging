package no.nav.veilarboppfolging.kafka.inngang

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.time.LocalDateTime
import no.nav.veilarboppfolging.oppfolgingsbruker.StartetAvType
import no.nav.veilarboppfolging.oppfolgingsbruker.inngang.OppfolgingStartBegrunnelseFraSystem

data class StartOppfolgingMelding(
    val personident: String,
    val aarsak: Aarsak,
    val kilde: Kilde,
    val arbeidsoppfolgingskontor: String?,
    val registrant: Registrant,
    val sendtTidspunkt: LocalDateTime,
) {
    enum class Aarsak {
        SYKMELDT_UTEN_ARBEIDSGIVER_4_UKER;

        fun toOppfolgingStartBegrunnelseFraSystem(): OppfolgingStartBegrunnelseFraSystem {
            return when (this) {
                SYKMELDT_UTEN_ARBEIDSGIVER_4_UKER -> OppfolgingStartBegrunnelseFraSystem.SYKMELDT_UTEN_ARBEIDSGIVER_4_UKER
            }
        }
    }

    enum class Kilde {
        ISYFO,
    }

    @JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "type",
        visible = true,
    )
    @JsonSubTypes(
        JsonSubTypes.Type(value = SystemRegistrant::class, name = "SYSTEM"),
        JsonSubTypes.Type(value = VeilederRegistrant::class, name = "VEILEDER"),
    )
    sealed class Registrant {
        abstract val type: StartetAvType
        abstract val opprettetAv: String

        fun toOppfolgingsRegistrant(): no.nav.veilarboppfolging.oppfolgingsbruker.Registrant {
            return when (this) {
                is SystemRegistrant -> {
                    no.nav.veilarboppfolging.oppfolgingsbruker.SystemRegistrant(opprettetAv)
                }
                is VeilederRegistrant -> {
                    no.nav.veilarboppfolging.oppfolgingsbruker.VeilederRegistrant(
                        no.nav.common.types.identer.NavIdent.of(opprettetAv)
                    )
                }
            }
        }
    }

    data class SystemRegistrant(
        override val opprettetAv: String,
    ) : Registrant() {
        override val type: StartetAvType = StartetAvType.SYSTEM
    }

    data class VeilederRegistrant(
        override val opprettetAv: String,
    ) : Registrant() {
        override val type: StartetAvType = StartetAvType.VEILEDER
    }
}
