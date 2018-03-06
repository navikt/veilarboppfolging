package no.nav.fo.veilarboppfolging.utils;


import no.nav.fo.veilarboppfolging.domain.BrukerRegistrering;
import no.nav.fo.veilarboppfolging.domain.StartRegistreringStatus;

public class SelvgaaendeUtil {

    public static final String NUS_KODE_0 = "0";
    public static final String NUS_KODE_2 = "2";
    public static final boolean UTDANNING_BESTATT = false;
    public static final boolean UTDANNING_GODKJENT_I_NORGE = false;
    public static final boolean HAR_HELSEUTFORDRINGER = true;

    public static boolean erSelvgaaende(BrukerRegistrering bruker, StartRegistreringStatus startRegistreringStatus) {
        return erBesvarelseneValidertSomSelvgaaende(bruker) &&
                !startRegistreringStatus.isUnderOppfolging() &&
                startRegistreringStatus.isOppfyllerKravForAutomatiskRegistrering();
    }

    public static boolean erBesvarelseneValidertSomSelvgaaende(BrukerRegistrering bruker) {
        return !(bruker.getNusKode().equals(NUS_KODE_0)
                || bruker.getNusKode().equals(NUS_KODE_2)
                || bruker.isUtdanningBestatt() == UTDANNING_BESTATT
                || bruker.isUtdanningGodkjentNorge() == UTDANNING_GODKJENT_I_NORGE
                || bruker.isHarHelseutfordringer() == HAR_HELSEUTFORDRINGER);
    }
}

