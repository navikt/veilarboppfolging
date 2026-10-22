package no.nav.veilarboppfolging.controller.graphql.oppfolging

import no.nav.veilarboppfolging.oppfolgingsbruker.inngang.KanStarteOppfolgingDto
import no.nav.veilarboppfolging.oppfolgingsbruker.RegistrantType
import no.nav.veilarboppfolging.oppfolgingsbruker.inngang.OppfolgingStartBegrunnelse
import no.nav.veilarboppfolging.oppfolgingsbruker.utgang.AvregistreringsType

data class OppfolgingDto(
    val erUnderOppfolging: Boolean?,
    val kanStarteOppfolging: KanStarteOppfolgingDto?,
)

data class OppfolgingsperiodeDto(
    val startTidspunkt: String,
    val sluttTidspunkt: String?,
    val id: String,
    val startetBegrunnelse: String?,
    val avsluttetAv: String?,
    val kvpPerioder: List<KvpPeriodeDto>
)

data class KvpPeriodeDto(
    val startTidspunkt: String,
    val sluttTidspunkt: String?
)

/**
 * Detaljert visning av én oppfølgingsperiode oppslått på periode-id.
 */
data class OppfolgingsperiodeDetaljerDto(
    val startTidspunkt: String,
    val sluttTidspunkt: String?,
    val aktorId: String,
    val ident: String?,
    val kontor: String?,
    val oppdatert: String,
    val startet: OppfolgingsperiodeStartetDto,
    val avsluttet: OppfolgingsperiodeAvsluttetDto?
)

data class OppfolgingsperiodeStartetDto(
    val startetAv: String?,
    val startetAvType: RegistrantType?,
    val registreringsType: OppfolgingStartBegrunnelse?
)

data class OppfolgingsperiodeAvsluttetDto(
    val avsluttetAv: String?,
    val avsluttetAvType: RegistrantType?,
    val avregistreringsType: AvregistreringsType?
)
