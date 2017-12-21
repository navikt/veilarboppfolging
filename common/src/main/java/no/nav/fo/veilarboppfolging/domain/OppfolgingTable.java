package no.nav.fo.veilarboppfolging.domain;

import lombok.Data;
import lombok.experimental.Accessors;

// The OppfolgingTable class is used as a transient data carrier from the
// database layer (OppfolgingsStatusRepository) to the service layer.
// Please avoid downstream use of this class. Instead, try to use
// the "Oppfolging" class and related methods.
@Data
@Accessors(chain = true)
public class OppfolgingTable {
    private String aktorId;
    private String veilederId;
    private boolean underOppfolging;
    private Long gjeldendeManuellStatusId;
    private Long gjeldendeBrukervilkarId;
    private Long gjeldendeEskaleringsvarselId;
    private Long gjeldendeMaalId;
}
