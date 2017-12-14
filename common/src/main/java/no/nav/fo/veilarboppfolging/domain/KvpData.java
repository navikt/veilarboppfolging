package no.nav.fo.veilarboppfolging.domain;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.Wither;

import java.util.Date;

@Value
@Builder
@Wither
public class KvpData {

    private long kvpId;
    private String aktorId;
    private String enhet;
    private String opprettetAv;
    private Date opprettetDato;
    private String opprettetBegrunnelse;
    private String avsluttetAv;
    private Date avsluttetDato;
    private String avsluttetBegrunnelse;

}
