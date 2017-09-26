package no.nav.fo.veilarboppfolging.mappers;

import no.nav.fo.veilarboppfolging.domain.VilkarStatus;
import no.nav.fo.veilarboppfolging.rest.domain.VilkarStatusApi;
import org.junit.Test;

public class VilkarMapperTest {

    @Test
    public void skalMappeVilkarStatusTilVilkarStatusApi() {
        assert(VilkarMapper.mapCommonVilkarStatusToVilkarStatusApi(VilkarStatus.AVSLATT).equals(VilkarStatusApi.AVSLATT));
        assert(VilkarMapper.mapCommonVilkarStatusToVilkarStatusApi(VilkarStatus.GODKJENT).equals(VilkarStatusApi.GODKJENT));
        assert(VilkarMapper.mapCommonVilkarStatusToVilkarStatusApi(VilkarStatus.IKKE_BESVART).equals(VilkarStatusApi.IKKE_BESVART));
    }
}
