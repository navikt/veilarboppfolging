package no.nav.fo.veilarboppfolging.rest.domain;

import java.util.Date;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class KvpDTO implements Comparable<KvpDTO> {
    private long kvpId;
    private String aktorId;
    private String enhet;
    private String opprettetAv;
    private Date opprettetDato;
    private String opprettetBegrunnelse;
    private String avsluttetAv;
    private Date avsluttetDato;
    private String avsluttetBegrunnelse;

    @Override
    public int compareTo(KvpDTO k) {
        return Long.compare(kvpId, k.kvpId);
    }
}