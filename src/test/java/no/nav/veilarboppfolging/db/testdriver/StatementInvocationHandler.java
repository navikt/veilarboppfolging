package no.nav.veilarboppfolging.db.testdriver;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.sql.Statement;

import static no.nav.veilarboppfolging.db.testdriver.DatabaseSyntaxMapper.h2Syntax;
import static no.nav.veilarboppfolging.test.ReflectionUtils.getMethod;

public class StatementInvocationHandler implements InvocationHandler {

    private static final Method EXECUTE_METHOD = getMethod(Statement.class, "execute", String.class);

    private final Statement statement;

    StatementInvocationHandler(Statement statement) {
        this.statement = statement;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (EXECUTE_METHOD.equals(method)) {
            args[0] = h2Syntax((String) args[0]);
            System.out.println(args[0]);
        }
        return method.invoke(statement, args);
    }

}
