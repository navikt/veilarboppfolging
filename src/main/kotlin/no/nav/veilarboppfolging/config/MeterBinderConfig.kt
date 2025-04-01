package no.nav.veilarboppfolging.config

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.binder.MeterBinder
import io.micrometer.core.instrument.binder.jvm.JvmThreadDeadlockMetrics
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration


@Configuration
class MeterBinderConfig {
    @Bean
    fun jvmThreadDeadlockMetricsBinder(meterRegistry: MeterRegistry): MeterBinder {
        return JvmThreadDeadlockMetrics()
            .also { it.bindTo(meterRegistry) }
    }
}