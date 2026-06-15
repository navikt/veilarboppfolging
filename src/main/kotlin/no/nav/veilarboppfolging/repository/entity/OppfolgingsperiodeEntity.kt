package no.nav.veilarboppfolging.repository.entity

import no.nav.veilarboppfolging.oppfolgingsbruker.StartetAvType
import no.nav.veilarboppfolging.oppfolgingsbruker.inngang.OppfolgingStartBegrunnelse
import no.nav.veilarboppfolging.oppfolgingsbruker.utgang.AvregistreringsType
import java.time.ZonedDateTime
import java.util.UUID

data class OppfolgingsperiodeEntity(
    val uuid: UUID,
    val aktorId: String,
    /**
     * Enten veileder-ident eller stringen "System" hvis avsluttet automatisk
     */
    val avsluttetAv: String? = null,
    val startDato: ZonedDateTime,
    val sluttDato: ZonedDateTime? = null,
    val begrunnelse: String? = null,
    val kvpPerioder: List<KvpPeriodeEntity>,
    val startetBegrunnelse: OppfolgingStartBegrunnelse? = null,
    val startetAv: String? = null,
    val startetAvType: StartetAvType? = null,
    val avregistreringsType: AvregistreringsType? = null,
)