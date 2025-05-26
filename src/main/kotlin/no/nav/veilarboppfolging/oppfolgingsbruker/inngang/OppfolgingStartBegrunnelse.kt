package no.nav.veilarboppfolging.oppfolgingsbruker.inngang

import no.nav.veilarboppfolging.kafka.dto.StartetBegrunnelseDTO

enum class OppfolgingStartBegrunnelse {
    ARBEIDSSOKER_REGISTRERING,
    /* Oppfølging startet på grunn av melding fra arbeidssøkerregisteret.
       startet_av_type vil indikere om det var bruker, veileder eller system som utførte registreringen */
    ARENA_SYNC_ARBS,
    /* Oppfølging startet av at bruker fikk formidlingsgruppe ARBS i Arena
       (dette skal normalt ikke skje lenger,
       siden vi reagerer direkte på arbeidssøkerregistreringen)
    */
    ARENA_SYNC_IARBS,
    /* For brukere med fomidlingsgruppe IARBS i Arena,
       vil oppfølging startes enten når de får en innsatsgruppe
       (i praksis når 14a vedtak ble gjort i Arena - dette gjøres ikke lenger etter ny vedtaksløsning ble lansert)
       eller at de får servicegruppe VURDU (sykemeldt uten arbeidsgiver)
       eller servicegruppe OPPFI (helserelatert arbeidsrettet oppfølging i Nav)
       */
    //REAKTIVERT_OPPFØLGING,
    MANUELL_REGISTRERING_VEILEDER;
    /* Oppfølging startet av veileder via registreringsløsningen inngar */

    fun toStartetBegrunnelseDTO(): StartetBegrunnelseDTO {
        if (this == ARBEIDSSOKER_REGISTRERING || this == ARENA_SYNC_ARBS) {
            return StartetBegrunnelseDTO.ARBEIDSSOKER
        } else { // Reativer er sykmeldt fordi arbeidsøkere automatisk er under oppfølging
            return StartetBegrunnelseDTO.SYKEMELDT_MER_OPPFOLGING
        }
    }
}