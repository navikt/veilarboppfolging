package no.nav.fo.veilarboppfolging.db;

import no.nav.sbl.jdbc.Database;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.jdbc.core.JdbcTemplate;

import static no.nav.fo.veilarboppfolging.config.JndiLocalContextConfig.setupInMemoryDatabase;

public class VeilederHistorikkRepositoryTest {
    private static Database database;
    private static NyeBrukereFeedRepository nyeBrukereFeedRepository;

    @BeforeEach
    public void setup() {
        database = new Database(new JdbcTemplate(setupInMemoryDatabase()));
        nyeBrukereFeedRepository = new NyeBrukereFeedRepository(database);
    }

}
