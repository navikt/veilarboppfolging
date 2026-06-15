package no.nav.veilarboppfolging.controller.v2.response;


public class ManuellStatusV2Response {
    boolean erUnderManuellOppfolging;
    KrrStatus krrStatus;

    public static class KrrStatus {
        boolean kanVarsles;
        boolean erReservert;
    }
}
