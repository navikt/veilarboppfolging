package no.nav.veilarboppfolging.controller.response;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.ZonedDateTime;

@Data
@Accessors(chain = true)
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