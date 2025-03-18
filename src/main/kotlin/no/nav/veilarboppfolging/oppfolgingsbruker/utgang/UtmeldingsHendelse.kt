package no.nav.veilarboppfolging.oppfolgingsbruker.utgang

import no.nav.common.types.identer.AktorId
import java.time.ZonedDateTime

sealed class UtmeldingsHendelse(val aktorId: AktorId)

sealed class UpsertIUtmelding(aktorId: AktorId, val iservFraDato: ZonedDateTime) : UtmeldingsHendelse(aktorId)

sealed class InsertIUtmelding(aktorId: AktorId, iservFraDato: ZonedDateTime) : UpsertIUtmelding(aktorId, iservFraDato)
sealed class UpdateIservDatoUtmelding(aktorId: AktorId, iservFraDato: ZonedDateTime) : UpsertIUtmelding(aktorId, iservFraDato)

sealed class SlettFraUtmelding(aktorId: AktorId) : UtmeldingsHendelse(aktorId)

class OppdateringFraArena_BleIserv(aktorId: AktorId, iservFraDato: ZonedDateTime) : InsertIUtmelding(aktorId, iservFraDato)
class ArbeidsøkerRegSync_BleIserv(aktorId: AktorId, iservFraDato: ZonedDateTime) : InsertIUtmelding(aktorId, iservFraDato)
class OppdateringFraArena_OppdaterIservDato(aktorId: AktorId, iservFraDato: ZonedDateTime) : UpdateIservDatoUtmelding(aktorId, iservFraDato)
class ArbeidsøkerRegSync_OppdaterIservDato(aktorId: AktorId, iservFraDato: ZonedDateTime) : UpdateIservDatoUtmelding(aktorId, iservFraDato)

class OppdateringFraArena_IkkeLengerIserv(aktorId: AktorId) : SlettFraUtmelding(aktorId)
class ScheduledJob_UtAvOppfolgingPga28DagerIserv(aktorId: AktorId) : SlettFraUtmelding(aktorId)
class ScheduledJob_AlleredeUteAvOppfolging(aktorId: AktorId) : SlettFraUtmelding(aktorId)
