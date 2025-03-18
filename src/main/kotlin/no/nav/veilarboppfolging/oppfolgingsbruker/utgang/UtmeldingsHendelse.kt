package no.nav.veilarboppfolging.oppfolgingsbruker.utgang

import no.nav.common.types.identer.AktorId
import java.time.ZonedDateTime

sealed class UtmeldingsHendelse(val aktorId: AktorId)

sealed class UpsertIUtmeling(aktorId: AktorId) : UtmeldingsHendelse(aktorId)
sealed class SlettFraUtmelding(aktorId: AktorId) : UtmeldingsHendelse(aktorId)

class InsertUtmeldingHendelse(aktorId: AktorId, val iservFraDato: ZonedDateTime) : UpsertIUtmeling(aktorId)
class OppdaterIservDatoHendelse(aktorId: AktorId, val iservFraDato: ZonedDateTime) : UpsertIUtmeling(aktorId)

class OppdateringFraArena_IkkeLengerIserv(aktorId: AktorId) : SlettFraUtmelding(aktorId)
class ScheduledJob_UtAvOppfolgingPga28DagerIserv(aktorId: AktorId) : SlettFraUtmelding(aktorId)
class ScheduledJob_AlleredeUteAvOppfolging(aktorId: AktorId) : SlettFraUtmelding(aktorId)
