package no.nav.veilarboppfolging.client.veilarbaktivitet;

import lombok.Data;
import lombok.experimental.Accessors;
import no.nav.veilarboppfolging.domain.arena.AktivitetStatus;
import no.nav.veilarboppfolging.domain.arena.ArenaAktivitetTypeDTO;
import no.nav.veilarboppfolging.domain.arena.ArenaStatusDTO;
import no.nav.veilarboppfolging.domain.arena.MoteplanDTO;

import java.time.ZonedDateTime;
import java.util.List;

@Data
@Accessors(chain = true)
public class ArenaAktivitetDTO {
    //Felles
    String id;
    AktivitetStatus status;
    ArenaAktivitetTypeDTO type;
    String tittel;
    String beskrivelse;
    ZonedDateTime fraDato; // TODO: Usikker på om dette skal være LocalDate
    ZonedDateTime tilDato; // TODO: Usikker på om dette skal være LocalDate
    ZonedDateTime opprettetDato; // TODO: Usikker på om dette skal være LocalDate
    boolean avtalt;
    public ArenaStatusDTO etikett;

    // Tiltaksaktivitet
    Float deltakelseProsent;
    String tiltaksnavn;
    String tiltakLokaltNavn;
    String arrangoer;
    String bedriftsnummer;
    Float antallDagerPerUke;
    ZonedDateTime statusSistEndret; // TODO: Usikker på om dette skal være LocalDate

    // Gruppeaktivitet
    List<MoteplanDTO> moeteplanListe;
}
