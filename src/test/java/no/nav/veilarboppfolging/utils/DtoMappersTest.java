package no.nav.veilarboppfolging.utils;

import no.nav.veilarboppfolging.controller.response.OppfolgingPeriodeDTO;
import no.nav.veilarboppfolging.kafka.TestUtils;
import no.nav.veilarboppfolging.repository.entity.OppfolgingsperiodeEntity;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class DtoMappersTest {

    @Test
    public void oppfolgingsperiode_tilDTO_skal_handtere_manglende_kvp() {
        OppfolgingsperiodeEntity oppfolgingsperiode = TestUtils.INSTANCE.oppfølgingPeriodeEntity(null);
        OppfolgingPeriodeDTO periodeDTO = DtoMappers.tilOppfolgingPeriodeDTO(oppfolgingsperiode, false);
        assertTrue(periodeDTO.getKvpPerioder().isEmpty());
    }

}
