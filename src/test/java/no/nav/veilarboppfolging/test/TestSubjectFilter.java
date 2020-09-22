package no.nav.veilarboppfolging.test;

import no.nav.common.auth.context.AuthContext;
import no.nav.common.auth.context.AuthContextHolder;
import no.nav.common.auth.context.UserRole;
import no.nav.common.test.auth.AuthTestUtils;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

public class TestSubjectFilter implements Filter {

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) {
        AuthContextHolder.withContext(AuthTestUtils.createAuthContext(UserRole.INTERN, "uid"), () -> filterChain.doFilter(servletRequest, servletResponse));
    }

}
