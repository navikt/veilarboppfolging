package no.nav.veilarboppfolging.utils;

import org.slf4j.LoggerFactory;
import org.slf4j.MarkerFactory

object SecureLog {
    @JvmField
    val secureLog = LoggerFactory.getLogger("team-logs-logger") ?: throw IllegalStateException("Klarte ikke å instansiere Team Logs logger.")
}
