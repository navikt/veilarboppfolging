package no.nav.veilarbsituasjon;


import javax.servlet.http.*;
import java.io.IOException;

public class AppServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.getWriter().println("Embedded Jetty");
    }

}
