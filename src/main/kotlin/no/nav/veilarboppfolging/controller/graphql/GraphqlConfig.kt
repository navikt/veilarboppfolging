package no.nav.veilarboppfolging.controller.graphql

import org.springframework.boot.autoconfigure.graphql.GraphQlSourceBuilderCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class GraphqlConfig {

    @Bean
    fun configure(instrumentation: GraphqlFieldTrackingInstrumentation): GraphQlSourceBuilderCustomizer {
        return GraphQlSourceBuilderCustomizer { builder ->
            builder.configureGraphQl { graphQLBuilder ->
                graphQLBuilder.instrumentation(instrumentation)
            }
        }
    }
}