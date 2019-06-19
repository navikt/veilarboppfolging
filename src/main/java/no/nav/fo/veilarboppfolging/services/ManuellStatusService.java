package no.nav.fo.veilarboppfolging.services;

import no.nav.fo.veilarboppfolging.db.ManuellStatusRepository;
import no.nav.fo.veilarboppfolging.domain.OppfolgingTable;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.Optional;

@Component
public class ManuellStatusService {

    @Inject
    private ManuellStatusRepository manuellStatusRepository;

    public boolean erManuell (OppfolgingTable eksisterendeOppfolgingstatus) {
        return Optional.ofNullable(eksisterendeOppfolgingstatus)
                .map(oppfolgingstatus ->  {
                    long gjeldendeManuellStatusId = eksisterendeOppfolgingstatus.getGjeldendeManuellStatusId();
                    return gjeldendeManuellStatusId > 0 && manuellStatusRepository.fetch(gjeldendeManuellStatusId).isManuell();
                })
                .orElse(false);
    }
}
