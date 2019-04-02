package no.nav.fo.veilarboppfolging.domain.arena;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;
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
    Date fraDato;
    Date tilDato;
    Date opprettetDato;
    boolean avtalt;
    public ArenaStatusDTO etikett;

    // Tiltaksaktivitet
    Float deltakelseProsent;
    String tiltaksnavn;
    String tiltakLokaltNavn;
    String arrangoer;
    String bedriftsnummer;
    Float antallDagerPerUke;
    Date statusSistEndret;

    // Gruppeaktivitet
    List<MoteplanDTO> moeteplanListe;
}
