package no.nav.veilarboppfolging.controller.v2.response;

import lombok.Value;

@Value
public class ManuellStatusV2Response {
    boolean erUnderManuellOppfolging;
    KrrStatus krrStatus;

    @Value
    public static class KrrStatus {
        boolean kanVarsles;
        boolean erReservert;
    }
}
