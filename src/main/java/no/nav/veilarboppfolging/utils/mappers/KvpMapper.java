package no.nav.veilarboppfolging.utils.mappers;

import no.nav.veilarboppfolging.domain.Kvp;
import no.nav.veilarboppfolging.controller.domain.KvpDTO;

public class KvpMapper {

    /**
     * Given a Kvp object, return its DTO representation. All fields are included.
     */
    public static KvpDTO KvpToDTO(Kvp k) {
        return new KvpDTO()
                .setKvpId(k.getKvpId())
                .setSerial(k.getSerial())
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
