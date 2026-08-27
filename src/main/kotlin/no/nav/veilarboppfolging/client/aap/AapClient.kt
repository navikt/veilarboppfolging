package no.nav.veilarboppfolging.client.aap

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import java.time.LocalDate
import java.util.function.Supplier
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.slf4j.LoggerFactory
import tools.jackson.databind.cfg.DateTimeFeature
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinModule
import tools.jackson.module.kotlin.readValue

/**
 * Klient for å sjekke om en person har eller har en aktiv søknad om ytelsen AAP (arbeidsavklaringspenger).
 * Dokumentasjon: https://aap-api.intern.dev.nav.no/swagger-ui/index.html#/DAB/post_dab_sakerByFnr
 */
class AapClient(
    private val baseUrl: String,
    private val tokenProvider: Supplier<String>,
    private val httpClient: OkHttpClient,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    private val objectMapper: JsonMapper = JsonMapper.builder()
        .addModule(KotlinModule.Builder().build())
        .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
        .disable(tools.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .build()

    fun harAap(personident: String): Boolean {
        val requestBody = AapPerioderRequest(personidentifikator = personident)

        val request = Request.Builder()
            .url("$baseUrl/dab/sakerByFnr")
            .addHeader("Authorization", "Bearer ${tokenProvider.get()}")
            .post(
                objectMapper.writeValueAsString(requestBody)
                    .toRequestBody("application/json".toMediaType())
            )
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw RuntimeException("Klarte ikke å sjekke AAP-status, status=${response.code}")
            }

            val body = response.body?.string()
                ?: throw RuntimeException("Body mangler i respons fra aap-api")

            val response = objectMapper.readValue<DabSakerResponse>(body)
            val harAktivAap = response.mottarEllerHarSoktAAP()
            logger.info("Sjekket AAP-status, harAap=$harAktivAap, antall saker=${response.saker.size}")
            return harAktivAap
        }
    }
}

data class DabSakerResponse(
    val saker: List<DabSak>,
) {
    fun mottarEllerHarSoktAAP(): Boolean {
        return saker.any { it.mottarEllerHarSoktAAP() }
    }
}

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "kilde",
)
@JsonSubTypes(
    JsonSubTypes.Type(value = DabSak.Arena::class, name = "ARENA"),
    JsonSubTypes.Type(value = DabSak.Kelvin::class, name = "KELVIN"),
)
sealed interface DabSak {
    val sakId: String
    val kilde: Kilde

    fun mottarEllerHarSoktAAP(): Boolean

    @JsonTypeName("ARENA")
    data class Arena(
        override val sakId: String,
        val statusKode: ArenaStatus,
        val periode: AapPeriode,
    ) : DabSak {
        override val kilde: Kilde = Kilde.ARENA

        override fun mottarEllerHarSoktAAP(): Boolean {
            val idag = LocalDate.now()
            val harAktivAap = periode.tilOgMedDato == null || periode.tilOgMedDato.isAfter(idag)
            return harAktivAap
        }
    }

    @JsonTypeName("KELVIN")
    data class Kelvin(
        override val sakId: String,
        val statuskode: KelvinStatus,
        val perioder: List<AapPeriode>,
        val ytelsesstatus: YtelseStatus,
    ) : DabSak {
        override val kilde: Kilde = Kilde.KELVIN

        override fun mottarEllerHarSoktAAP(): Boolean {
            val idag = LocalDate.now()
            val harAktivAap = perioder.any { it.tilOgMedDato == null || it.tilOgMedDato.isAfter(idag) }
            return harAktivAap || statuskode == KelvinStatus.SOKNAD_UNDER_BEHANDLING
        }
    }

    enum class Kilde {
        ARENA,
        KELVIN
    }

    enum class ArenaStatus {
        AVSLU,
        FORDE,
        GODKJ,
        INNST,
        IVERK,
        KONT,
        MOTAT,
        OPPRE,
        REGIS,
        UKJENT,
    }

    enum class KelvinStatus {
        OPPRETTET,
        UTREDES,
        LØPENDE,
        AVSLUTTET,
        SOKNAD_UNDER_BEHANDLING,
        REVURDERING_UNDER_BEHANDLING,
        FERDIGBEHANDLET,
    }

    enum class YtelseStatus {
        FOR_VEDTAK, LOPENDE, AVSLUTTET
    }
}

data class AapPerioderRequest(
    val personidentifikator: String,
)

data class AapPeriode(
    val fraOgMedDato: LocalDate?,
    val tilOgMedDato: LocalDate?
)
