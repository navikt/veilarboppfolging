package no.nav.fo.veilarboppfolging.rest.domain;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;

@Data
@Accessors(chain = true)
public class AvsluttetOppfolgingFeedDTO implements Comparable<AvsluttetOppfolgingFeedDTO> {

    public String aktoerid;
    public Date sluttdato;
    public Date oppdatert;

    @Override
    public int compareTo(AvsluttetOppfolgingFeedDTO avsluttetOppfolgingFeedDTO) {
        return oppdatert.compareTo(avsluttetOppfolgingFeedDTO.oppdatert);
    }
}
