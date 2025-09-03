package no.nav.veilarboppfolging.oppfolgingsperioderHendelser

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import no.nav.veilarboppfolging.oppfolgingsperioderHendelser.hendelser.HendelseType
import no.nav.veilarboppfolging.oppfolgingsperioderHendelser.hendelser.OppfolgingStartetHendelseDto
import no.nav.veilarboppfolging.oppfolgingsperioderHendelser.hendelser.OppfolgingsAvsluttetHendelseDto

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "hendelseType")
@JsonSubTypes(
    JsonSubTypes.Type(value = OppfolgingStartetHendelseDto::class, name = "OPPFOLGING_STARTET"),
    JsonSubTypes.Type(value = OppfolgingsAvsluttetHendelseDto::class, name = "OPPFOLGING_AVSLUTTET")
)
abstract class OppfolgingsHendelseDto(
    val hendelseType: HendelseType,
)