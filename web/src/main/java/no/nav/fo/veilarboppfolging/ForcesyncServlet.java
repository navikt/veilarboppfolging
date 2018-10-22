package no.nav.fo.veilarboppfolging;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ForcesyncServlet extends HttpServlet {

    private JdbcTemplate jdbc;

    @Override
    public void init() throws ServletException {
        jdbc = WebApplicationContextUtils.getWebApplicationContext(getServletContext())
                .getBean(JdbcTemplate.class);

        super.init();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String sql = Stream.of(
                "UPDATE veilarboppfolging.OPPFOLGINGSTATUS",
                "SET oppdatert=CURRENT_TIMESTAMP,",
                "FEED_ID = null",
                "WHERE aktor_id in (",
                " SELECT os.AKTOR_ID",
                " FROM veilarboppfolging.OPPFOLGINGSTATUS os",
                " LEFT JOIN veilarbportefolje.OPPFOLGING_DATA@VEILARBPORTEFOLJE bd ON (os.AKTOR_ID = bd.aktoerid)",
                " WHERE os.OPPDATERT < (sysdate - 1/24)",
                " AND (",
                " (os.VEILEDER is null AND bd.veilederident is not null) OR",
                " (os.VEILEDER is not null AND bd.veilederident is null) OR",
                " (os.VEILEDER != bd.veilederident)",
                " )",
                ")"
        ).collect(Collectors.joining(" "));

        int updated = jdbc.update(sql);

        resp.getWriter().write("Updated row: " + updated);
        resp.setStatus(200);
    }
}
