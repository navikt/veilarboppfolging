package no.nav.veilarboppfolging.repository.entity

import no.nav.veilarboppfolging.oppfolgingsbruker.RegistrantType
import no.nav.veilarboppfolging.oppfolgingsbruker.inngang.OppfolgingStartBegrunnelse
import no.nav.veilarboppfolging.oppfolgingsbruker.utgang.AvregistreringsType
import java.time.ZonedDateTime
import java.util.UUID

data class OppfolgingsperiodeEntity @JvmOverloads constructor(
    val uuid: UUID,
    val aktorId: String,
    /**
     * Veileder-ident hvis veileder avsluttet,
     * "System" hvis avsluttet automatisk,
     * "Bruker" hvis avsluttet av bruker selv
     */
    val avsluttetAv: String? = null,
    val startDato: ZonedDateTime,
    val sluttDato: ZonedDateTime? = null,
    val begrunnelse: String? = null,
    val kvpPerioder: List<KvpPeriodeEntity>,
    val startetBegrunnelse: OppfolgingStartBegrunnelse? = null,
    val startetAv: String? = null,
    val startetAvType: RegistrantType? = null,
    val avregistreringsType: AvregistreringsType? = null,
    val avsluttetAvType: RegistrantType? = null,
    val oppdatert: ZonedDateTime? = null,
) {
    fun oppdaterMedKvpPerioder(nyeKvpPerioder: List<KvpPeriodeEntity>) = this.copy(kvpPerioder = nyeKvpPerioder)
}
