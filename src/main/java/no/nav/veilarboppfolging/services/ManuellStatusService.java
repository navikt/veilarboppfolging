package no.nav.veilarboppfolging.services;

import no.nav.veilarboppfolging.domain.OppfolgingTable;
import no.nav.veilarboppfolging.repository.ManuellStatusRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ManuellStatusService {

    private final ManuellStatusRepository manuellStatusRepository;

    @Autowired
    public ManuellStatusService(ManuellStatusRepository manuellStatusRepository) {
        this.manuellStatusRepository = manuellStatusRepository;
    }

    public boolean erManuell (OppfolgingTable eksisterendeOppfolgingstatus) {
        return Optional.ofNullable(eksisterendeOppfolgingstatus)
                .map(oppfolgingstatus ->  {
                    long gjeldendeManuellStatusId = eksisterendeOppfolgingstatus.getGjeldendeManuellStatusId();
                    return gjeldendeManuellStatusId > 0 && manuellStatusRepository.fetch(gjeldendeManuellStatusId).isManuell();
                })
                .orElse(false);
    }

}
