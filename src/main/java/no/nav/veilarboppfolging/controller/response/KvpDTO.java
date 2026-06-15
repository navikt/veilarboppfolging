package no.nav.veilarboppfolging.controller.response;




import java.time.ZonedDateTime;


public class KvpDTO implements Comparable<KvpDTO> {
    private long kvpId;
    private long serial;
    private String aktorId;
    private String enhet;
    private String opprettetAv;
    private ZonedDateTime opprettetDato;
    private String opprettetBegrunnelse;
    private String avsluttetAv;
    private ZonedDateTime avsluttetDato;
    private String avsluttetBegrunnelse;

    @Override
    public int compareTo(KvpDTO k) {
        return Long.compare(serial, k.serial);
    }
}