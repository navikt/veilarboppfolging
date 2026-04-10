package no.nav.veilarboppfolging.service

import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.context.event.EventListener
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.env.MapPropertySource
import org.springframework.core.env.PropertySource
import org.springframework.stereotype.Component


@Component
class SpringContextEventListener {
    val LOGGER = org.slf4j.LoggerFactory.getLogger(SpringContextEventListener::class.java)
    @EventListener
    fun handleContextRefreshed(event: ContextRefreshedEvent?) {
        LOGGER.info("***** Context refreshed ******")

        val env = event!!.getApplicationContext().getEnvironment() as ConfigurableEnvironment?
        env!!.getPropertySources()
            .stream()
            .filter { ps: PropertySource<*>? -> ps is MapPropertySource }
            .map<MutableSet<String?>?> { ps: PropertySource<*>? -> (ps as MapPropertySource).getSource().keys }
            .flatMap<String?> { obj: MutableSet<String?>? -> obj!!.stream() }
            .distinct()
            .sorted()
            .filter { key: String? -> key!!.contains("graphql") }
            .forEach { key: String? -> LOGGER.info("{}={}", key, env.getProperty(key)) }
    }
}

