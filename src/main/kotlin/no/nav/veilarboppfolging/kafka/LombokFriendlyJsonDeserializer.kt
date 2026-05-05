package no.nav.veilarboppfolging.kafka

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility
import org.apache.kafka.common.serialization.Deserializer
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinModule

/**
 * Kafka-deserializer som bruker en egen Jackson 3-mapper der `CreatorVisibility`
 * er `PUBLIC_ONLY`. Nødvendig fordi standardmapperen i `no.nav.common:json`
 * setter `CreatorVisibility.NONE`, og Jackson 3 har fjernet
 * `MapperFeature.ALLOW_FINAL_FIELDS_AS_MUTATORS`. Resultatet er at Lombok
 * `@Value`-klasser (f.eks. fra `pto-schema`) deserialiseres med bare null-felter
 * fordi Jackson hverken kan kalle `@AllArgsConstructor` eller skrive til
 * `private final` felter — den havner i `@NoArgsConstructor(force = true)` og
 * returnerer et objekt der alt er null.
 *
 * Med creator-synlighet på `PUBLIC_ONLY` plukker Jackson opp
 * `@AllArgsConstructor` og fyller objektene riktig.
 */
class LombokFriendlyJsonDeserializer<T : Any>(
    private val type: Class<T>,
) : Deserializer<T> {

    override fun deserialize(topic: String?, data: ByteArray?): T? {
        if (data == null) return null
        return mapper.readValue(data, type)
    }

    companion object {
        private val mapper: ObjectMapper = JsonMapper.builder()
            .addModule(KotlinModule.Builder().build())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true)
            .configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false)
            .changeDefaultVisibility { v ->
                v.withFieldVisibility(Visibility.ANY)
                    .withGetterVisibility(Visibility.NONE)
                    .withSetterVisibility(Visibility.NONE)
                    .withCreatorVisibility(Visibility.PUBLIC_ONLY)
            }
            .build()
    }
}
