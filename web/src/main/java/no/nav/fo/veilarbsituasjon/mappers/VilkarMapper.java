package no.nav.fo.veilarbsituasjon.mappers;

import no.nav.fo.veilarbsituasjon.domain.VilkarStatus;
import no.nav.fo.veilarbsituasjon.rest.domain.VilkarStatusApi;

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
