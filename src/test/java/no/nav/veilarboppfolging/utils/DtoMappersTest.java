package no.nav.veilarboppfolging.utils;

import no.nav.veilarboppfolging.controller.domain.OppfolgingPeriodeDTO;
import no.nav.veilarboppfolging.domain.Oppfolgingsperiode;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class DtoMappersTest {

    @Test
    public void oppfolgingsperiode_tilDTO_skal_handtere_manglende_kvp() {
        Oppfolgingsperiode oppfolgingsperiode = Oppfolgingsperiode.builder()
                .kvpPerioder(null)
                .build();

        OppfolgingPeriodeDTO periodeDTO = DtoMappers.tilOppfolgingPeriodeDTO(oppfolgingsperiode, false);
        assertTrue(periodeDTO.kvpPerioder.isEmpty());
    }

}
