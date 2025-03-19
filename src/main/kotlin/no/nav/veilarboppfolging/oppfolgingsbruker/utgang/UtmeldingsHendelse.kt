package no.nav.veilarboppfolging.oppfolgingsbruker.utgang

import no.nav.common.types.identer.AktorId
import java.time.ZonedDateTime

/*
* Utmeldingshendelser skjer selvom brukeren tas ut av oppfølging av OppfolginsEndringService
* */
sealed class UtmeldingsHendelse(val aktorId: AktorId)

sealed class UpsertIUtmelding(aktorId: AktorId, val iservFraDato: ZonedDateTime) : UtmeldingsHendelse(aktorId)

sealed class InsertIUtmelding(aktorId: AktorId, iservFraDato: ZonedDateTime) : UpsertIUtmelding(aktorId, iservFraDato)
sealed class UpdateIservDatoUtmelding(aktorId: AktorId, iservFraDato: ZonedDateTime) : UpsertIUtmelding(aktorId, iservFraDato)
sealed class SlettFraUtmelding(aktorId: AktorId) : UtmeldingsHendelse(aktorId)
sealed class NoOp(aktorId: AktorId) : UtmeldingsHendelse(aktorId)

// NoOp - Iserv, ikke under oppfølging og ikke i utmeldingstablell
class OppdateringFraArena_NoOp(aktorId: AktorId) : NoOp(aktorId)
class ArbeidsøkerRegSync_NoOp(aktorId: AktorId) : NoOp(aktorId)

// Inserts
class OppdateringFraArena_BleIserv(aktorId: AktorId, iservFraDato: ZonedDateTime) : InsertIUtmelding(aktorId, iservFraDato)
class ArbeidsøkerRegSync_BleIserv(aktorId: AktorId, iservFraDato: ZonedDateTime) : InsertIUtmelding(aktorId, iservFraDato)

// Update iserv dato
class OppdateringFraArena_OppdaterIservDato(aktorId: AktorId, iservFraDato: ZonedDateTime) : UpdateIservDatoUtmelding(aktorId, iservFraDato)
class ArbeidsøkerRegSync_OppdaterIservDato(aktorId: AktorId, iservFraDato: ZonedDateTime) : UpdateIservDatoUtmelding(aktorId, iservFraDato)

// Deletes
class OppdateringFraArena_IkkeLengerIserv(aktorId: AktorId) : SlettFraUtmelding(aktorId)
class ArbeidsøkerRegSync_IkkeLengerIserv(aktorId: AktorId) : SlettFraUtmelding(aktorId)
class ArbeidsøkerRegSync_AlleredeUteAvOppfolging(aktorId: AktorId) : SlettFraUtmelding(aktorId)
class OppdateringFraArena_AlleredeUteAvOppfolging(aktorId: AktorId) : SlettFraUtmelding(aktorId)
class ScheduledJob_UtAvOppfolgingPga28DagerIserv(aktorId: AktorId) : SlettFraUtmelding(aktorId)
class ScheduledJob_AlleredeUteAvOppfolging(aktorId: AktorId) : SlettFraUtmelding(aktorId)
