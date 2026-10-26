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
sealed class OppfolgingsperiodeDetaljerDto {
    abstract val startTidspunkt: String
    abstract val sluttTidspunkt: String?
    abstract val aktorId: String
    abstract val ident: String?
    abstract val kontor: String?
    abstract val oppdatertTidspunkt: String
    abstract val startet: OppfolgingsperiodeStartetDto
    abstract val avsluttet: OppfolgingsperiodeAvsluttetDto?
}

data class ApenOppfolgingsperiodeDetaljerDto(
    override val startTidspunkt: String,
    override val aktorId: String,
    override val ident: String?,
    override val kontor: String?,
    override val oppdatertTidspunkt: String,
    override val startet: OppfolgingsperiodeStartetDto,
) : OppfolgingsperiodeDetaljerDto() {
    override val sluttTidspunkt: String? = null
    override val avsluttet: OppfolgingsperiodeAvsluttetDto? = null
}

data class LukketOppfolgingsperiodeDetaljerDto(
    override val startTidspunkt: String,
    override val sluttTidspunkt: String,
    override val aktorId: String,
    override val ident: String?,
    override val oppdatertTidspunkt: String,
    override val startet: OppfolgingsperiodeStartetDto,
    override val avsluttet: OppfolgingsperiodeAvsluttetDto,
) : OppfolgingsperiodeDetaljerDto() {
    override val kontor: String? = null
}

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
