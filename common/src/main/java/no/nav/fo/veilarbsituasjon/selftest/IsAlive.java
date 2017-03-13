package no.nav.fo.veilarbsituasjon.selftest;


import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.ServletException;
import javax.servlet.http.*;
import java.io.IOException;

public class IsAlive extends HttpServlet {

    private WebApplicationContext ctx;

    @Override
    public void init() {
        ctx = WebApplicationContextUtils.getWebApplicationContext(getServletContext());
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String status = ctx.getStartupDate() > 0 ? "UP" : "DOWN";
        resp.setContentType("text/html");
        resp.getWriter().write("Application: " + status);
    }
}
