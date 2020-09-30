package no.nav.veilarboppfolging.client.veilarbaktivitet;

import no.nav.common.health.HealthCheck;

import java.util.List;

/**
 * TODO: Denne klienten brukes kun for å sette 1 felt i AvslutningStatusData (harTiltak).
 *  Som kun brukes av 1 frontend (veilarbvisittkortfs). Denne klienten bør fjernes fra veilarboppfolging og
 *  veilarbvisittkortfs burde kalle veilarbaktivitet direkte istedenfor.
 */
public interface VeilarbaktivitetClient extends HealthCheck {

    List<ArenaAktivitetDTO> hentArenaAktiviteter(String fnr);

}
