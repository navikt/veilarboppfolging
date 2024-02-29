package no.nav.veilarboppfolging.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import lombok.Getter
import lombok.RequiredArgsConstructor
import lombok.Setter
import no.nav.veilarboppfolging.config.DatabaseConfig.DatasourceProperties
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import javax.sql.DataSource

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(DatasourceProperties::class)
open class DatabaseConfig {
    private val datasourceProperties: DatasourceProperties? = null

    @Bean
    open fun dataSource(): DataSource {
        val config = HikariConfig()
        config.jdbcUrl = datasourceProperties!!.url
        config.username = datasourceProperties.username
        config.password = datasourceProperties.password
        config.maximumPoolSize = 5

        return HikariDataSource(config)
    }

    @Bean
    open fun db(dataSource: DataSource?): JdbcTemplate {
        return JdbcTemplate(dataSource)
    }

    @Bean
    open fun namedParameterJdbcTemplate(jdbcTemplate: JdbcTemplate?): NamedParameterJdbcTemplate {
        return NamedParameterJdbcTemplate(jdbcTemplate)
    }

    @Getter
    @Setter
    @ConfigurationProperties(prefix = "app.datasource")
    class DatasourceProperties {
        var url: String? = null
        var username: String? = null
        var password: String? = null
    }
}