package no.nav.veilarboppfolging.test;

import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.EnhetId;
import no.nav.common.types.identer.Fnr;
import no.nav.common.types.identer.NavIdent;

public class TestData {

    public final static Fnr TEST_FNR = Fnr.of("12345678900");
    public final static Fnr TEST_FNR_2 = Fnr.of("12345678901");
    public final static AktorId TEST_AKTOR_ID = AktorId.of("11122233334445");
    public final static AktorId TEST_AKTOR_ID_2 = AktorId.of("5556667778889");
    public final static AktorId TEST_AKTOR_ID_3 = AktorId.of("3409823");
    public final static NavIdent TEST_NAV_IDENT = NavIdent.of("Z112233");
    public final static EnhetId TEST_ENHET_ID = EnhetId.of("0123");
    public final static NavIdent TEST_NAV_IDENT_2 = NavIdent.of("Z123456");
    public final static String TEST_TOKENDINGS_ISSUER = "https://tokendings";
    public final static String TEST_AZURE_ISSUER = "microsoftonline.com";
}
