package no.nav.fo.veilarboppfolging.mappers;

import no.nav.fo.veilarboppfolging.domain.Kvp;
import no.nav.fo.veilarboppfolging.rest.domain.KvpDTO;

public class KvpMapper {

    /**
     * Given a Kvp object, return its DTO representation. All fields are included.
     */
    public static KvpDTO KvpToDTO(Kvp k) {
        return new KvpDTO()
                .setKvpId(k.getKvpId())
                .setAktorId(k.getAktorId())
                .setAvsluttetAv(k.getAvsluttetAv())
                .setAvsluttetBegrunnelse(k.getAvsluttetBegrunnelse())
                .setAvsluttetDato(k.getAvsluttetDato())
                .setEnhet(k.getEnhet())
                .setOpprettetAv(k.getOpprettetAv())
                .setOpprettetBegrunnelse(k.getOpprettetBegrunnelse())
                .setOpprettetDato(k.getOpprettetDato());
    }
}
