package no.nav.fo.veilarbsituasjon.mappers;

import no.nav.fo.veilarbsituasjon.domain.VilkarStatus;
import no.nav.fo.veilarbsituasjon.rest.domain.VilkarStatusApi;
import org.junit.Test;

public class VilkarMapperTest {

    @Test
    public void skalMappeVilkarStatusTilVilkarStatusApi() {
        assert(VilkarMapper.mapCommonVilkarStatusToVilkarStatusApi(VilkarStatus.AVSLATT).equals(VilkarStatusApi.AVSLATT));
        assert(VilkarMapper.mapCommonVilkarStatusToVilkarStatusApi(VilkarStatus.GODKJENT).equals(VilkarStatusApi.GODKJENT));
        assert(VilkarMapper.mapCommonVilkarStatusToVilkarStatusApi(VilkarStatus.IKKE_BESVART).equals(VilkarStatusApi.IKKE_BESVART));
    }
}
