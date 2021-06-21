package no.nav.veilarboppfolging.utils;

import no.nav.veilarboppfolging.controller.response.OppfolgingPeriodeDTO;
import no.nav.veilarboppfolging.repository.entity.OppfolgingsperiodeEntity;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class DtoMappersTest {

    @Test
    public void oppfolgingsperiode_tilDTO_skal_handtere_manglende_kvp() {
        OppfolgingsperiodeEntity oppfolgingsperiode = OppfolgingsperiodeEntity.builder()
                .kvpPerioder(null)
                .build();

        OppfolgingPeriodeDTO periodeDTO = DtoMappers.tilOppfolgingPeriodeDTO(oppfolgingsperiode, false);
        assertTrue(periodeDTO.kvpPerioder.isEmpty());
    }

}
