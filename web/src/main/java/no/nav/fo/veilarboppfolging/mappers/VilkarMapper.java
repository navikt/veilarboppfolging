package no.nav.fo.veilarboppfolging.mappers;

import no.nav.fo.veilarboppfolging.domain.VilkarStatus;
import no.nav.fo.veilarboppfolging.rest.domain.VilkarStatusApi;

public class VilkarMapper {

    public static VilkarStatusApi mapCommonVilkarStatusToVilkarStatusApi(VilkarStatus vilkarStatus) {
        switch (vilkarStatus) {
            case GODKJENT:
                return VilkarStatusApi.GODKJENT;
            case AVSLATT:
                return VilkarStatusApi.AVSLATT;
            default:
                return VilkarStatusApi.IKKE_BESVART;
        }
    }

}
