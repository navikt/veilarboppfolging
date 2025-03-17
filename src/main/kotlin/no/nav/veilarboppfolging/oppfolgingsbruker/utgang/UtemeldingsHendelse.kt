package no.nav.veilarboppfolging.oppfolgingsbruker.utgang

import no.nav.common.types.identer.AktorId

sealed class UtemeldingsHendelse(val aktorId: AktorId)

sealed class StartGracePeriode(aktorId: AktorId) : UtemeldingsHendelse(aktorId)
sealed class SlettFraUtmelding(aktorId: AktorId) : UtemeldingsHendelse(aktorId)

class SkalUtmeldes(aktorId: AktorId) : StartGracePeriode(aktorId)
class ManueltAvsluttetAvVeielder(aktorId: AktorId) : SlettFraUtmelding(aktorId)
class IkkeLengerIservIArena(aktorId: AktorId) : SlettFraUtmelding(aktorId)
class UtAvOppfolgingPga28DagerIserv(aktorId: AktorId) : SlettFraUtmelding(aktorId)
class AlleredeUteAvOppfolging(aktorId: AktorId) : SlettFraUtmelding(aktorId)