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
class GraphqlFieldTrackingInstrumentation(
    val authService: AuthService,
    val meterRegistry: MeterRegistry,
): SimplePerformantInstrumentation() {

    override fun beginFieldFetch(
        parameters: InstrumentationFieldFetchParameters,
        state: InstrumentationState?
    ): InstrumentationContext<in Any>? {
        val context = parameters.environment.graphQlContext
        val apiConsumer = context.get<String?>("apiConsumer")

        if (apiConsumer != null) {
            val fieldName = parameters.environment.field.name

            Counter.builder("graphql_field_fetch")
                .tag("field", fieldName)
                .tag("consumer", apiConsumer)
                .register(meterRegistry)
                .increment()
        }

        return super.beginFieldFetch(parameters, state)
    }

}
