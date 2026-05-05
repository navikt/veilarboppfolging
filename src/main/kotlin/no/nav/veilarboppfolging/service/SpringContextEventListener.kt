package no.nav.veilarboppfolging.service

import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.context.event.EventListener
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.env.MapPropertySource
import org.springframework.stereotype.Component

@Component
class SpringContextEventListener {
    val LOGGER = org.slf4j.LoggerFactory.getLogger(SpringContextEventListener::class.java)
    @EventListener
    fun handleContextRefreshed(event: ContextRefreshedEvent) {
        LOGGER.info("***** Context refreshed ******")

        val env = event.applicationContext.environment as ConfigurableEnvironment
        env.propertySources
            .filterIsInstance<MapPropertySource>()
            .flatMap { it.source.keys }
            .distinct()
            .sorted()
            .filter { it.contains("graphql") }
            .forEach { key -> LOGGER.info("{}={}", key, env.getProperty(key)) }
    }
}

