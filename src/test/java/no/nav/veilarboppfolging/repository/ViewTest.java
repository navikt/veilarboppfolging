package no.nav.veilarboppfolging.repository;

import no.nav.common.json.JsonUtils;
import no.nav.veilarboppfolging.LocalDatabaseSingleton;
import org.json.JSONArray;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.springframework.jdbc.core.JdbcTemplate;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(value = Parameterized.class)
public class ViewTest {

    @Parameters(name = "{0}")
    public static Object[] views() {
            return new Object[]{
                "DVH_GJELDENDE_OPPFOLGINGSTATUS",
                "DVH_MANUELL_HISTORIKK",
                "DVH_OPPFOLGINGSHISTORIKK"
        };
    }

    @Parameter(value = 0)
    public String viewName;

    private static final int antallViews = views().length;

    private static final JdbcTemplate jdbcTemplate = LocalDatabaseSingleton.INSTANCE.getJdbcTemplate();

    @Test
    public void database_skal_ha_riktig_antall_views() {
        long count = (long) jdbcTemplate.queryForList("" +
                "SELECT " +
                "COUNT(*) AS VIEW_COUNT " +
                "FROM INFORMATION_SCHEMA.VIEWS " +
                "WHERE TABLE_SCHEMA='PUBLIC';"
        ).get(0).get("VIEW_COUNT");

        assertThat(count).isEqualTo(antallViews);
    }

    @Test
    public void view_eksisterer() {
        List<Map<String, Object>> viewData = jdbcTemplate.queryForList("SELECT * FROM " + viewName + ";");

        assertThat(viewData).isNotNull();
    }

    @Test
    public void view_skal_reflektere_kolonner_i_tabell() {
        String kolonneData = jsonFormatter(JsonUtils.toJson(hentKolonneDataForView(viewName)));
        String kolonneDataFasit = jsonFormatter(lesInnholdFraFil("view-meta-data/" + viewName.toLowerCase() + ".json"));

        assertThat(kolonneData).isEqualTo(kolonneDataFasit);
    }

    private static String jsonFormatter(String jsonArray) {
        return new JSONArray(jsonArray).toString();
    }

    private List<Map<String, Object>> hentKolonneDataForView(String view) {
        return jdbcTemplate.queryForList(
                "SELECT " +
                        "COLUMN_NAME, " +
                        "DATA_TYPE, " +
                        "CHARACTER_MAXIMUM_LENGTH " +
                        "FROM INFORMATION_SCHEMA.COLUMNS " +
                        "WHERE TABLE_NAME = '" + view + "';"
        );
    }

    private static String lesInnholdFraFil(String filNavn) {
        return new Scanner(ViewTest.class.getClassLoader().getResourceAsStream(filNavn), StandardCharsets.UTF_8).useDelimiter("\\A").next();
    }
}
