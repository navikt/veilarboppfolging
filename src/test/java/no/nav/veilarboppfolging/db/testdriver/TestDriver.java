package no.nav.veilarboppfolging.db.testdriver;

import no.nav.veilarboppfolging.utils.ProxyUtils;

import java.sql.*;
import java.util.Properties;
import java.util.logging.Logger;

import static java.sql.DriverManager.deregisterDriver;
import static java.sql.DriverManager.registerDriver;

public class TestDriver implements Driver {

    private static final String DB_URL = "jdbc:h2:mem:veilarboppfolging-%s;DB_CLOSE_DELAY=-1;MODE=Oracle";
    private static int databaseCounter;

    static {
        try {
            registerDriver(new TestDriver());
            // Deregistrerer opprinnelig h2 driver for å sikre at TestDriver benyttes for h2-databaser.
            // load() returnerer den registrerte instansen av h2-driveren.
            deregisterDriver(org.h2.Driver.load());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private Driver driver = new org.h2.Driver();

    public static String createInMemoryDatabaseUrl() {
        return String.format(DB_URL, databaseCounter++);
    }

    @Override
    public Connection connect(String url, Properties info) throws SQLException {
        return ProxyUtils.proxy(new ConnectionInvocationHandler(driver.connect(url, info)), Connection.class);
    }

    @Override
    public boolean acceptsURL(String url) throws SQLException {
        return driver.acceptsURL(url);
    }

    @Override
    public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
        return driver.getPropertyInfo(url, info);
    }

    @Override
    public int getMajorVersion() {
        return driver.getMajorVersion();
    }

    @Override
    public int getMinorVersion() {
        return driver.getMinorVersion();
    }

    @Override
    public boolean jdbcCompliant() {
        return driver.jdbcCompliant();
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return driver.getParentLogger();
    }

}
