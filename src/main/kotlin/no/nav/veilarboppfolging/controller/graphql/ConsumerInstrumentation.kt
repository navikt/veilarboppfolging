package no.nav.veilarboppfolging.controller.graphql

import graphql.execution.instrumentation.InstrumentationContext
import graphql.execution.instrumentation.InstrumentationState
import graphql.execution.instrumentation.SimplePerformantInstrumentation
import graphql.execution.instrumentation.parameters.InstrumentationFieldFetchParameters
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import no.nav.veilarboppfolging.service.AuthService
import org.springframework.stereotype.Component

@Component
class ConsumerInstrumentation(
    val authService: AuthService,
    val meterRegistry: MeterRegistry,
): SimplePerformantInstrumentation() {

    override fun beginFieldFetch(
        parameters: InstrumentationFieldFetchParameters,
        state: InstrumentationState
    ): InstrumentationContext<in Any>? {
        val fetchingAppName = authService.hentApplikasjonFraContext()
        val fieldName = parameters.environment.field.name

        Counter.builder("graphql_field_fetch")
            .tag("field", fieldName)
            .tag("consumer", fetchingAppName)
            .register(meterRegistry)
            .increment()

        return super.beginFieldFetch(parameters, state)
    }

}