package no.nav.veilarboppfolging.client.veilarbarena;


import java.time.ZonedDateTime;

/**
 * Har IKKE feltet kanReaktiveres som oppfølgingsStatus {@link no.nav.veilarboppfolging.client.veilarbarena.VeilarbArenaOppfolgingsStatus} har, men har hovedmaalkode
 * @see no.nav.veilarboppfolging.client.veilarbarena.VeilarbArenaOppfolgingsStatus
 */
public record VeilarbArenaOppfolgingsBruker(
    String fodselsnr,
    String formidlingsgruppekode,
    ZonedDateTime iservFraDato,
    String navKontor,
    String kvalifiseringsgruppekode,
    String rettighetsgruppekode,
    String hovedmaalkode,
    String sikkerhetstiltakTypeKode,
    String frKode,
    Boolean harOppfolgingssak,
    Boolean sperretAnsatt,
    Boolean erDoed,
    ZonedDateTime doedFraDato,
    ZonedDateTime sistEndretDato
) {}
