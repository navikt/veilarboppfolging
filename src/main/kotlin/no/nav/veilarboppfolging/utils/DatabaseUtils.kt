package no.nav.veilarboppfolging.utils

import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Timestamp
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*

object DatabaseUtils {

    @Throws(SQLException::class)
    fun hentZonedDateTime(rs: ResultSet, kolonneNavn: String?): ZonedDateTime {
        return Optional.ofNullable(rs.getTimestamp(kolonneNavn))
            .map { timestamp: Timestamp ->
                timestamp.toLocalDateTime().atZone(ZoneId.systemDefault())
            }
            .orElse(null)
    }
}