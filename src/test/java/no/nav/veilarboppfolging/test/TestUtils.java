package no.nav.veilarboppfolging.test;



import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

public class TestUtils {

    private static Logger log = LoggerFactory.getLogger(TestUtils.class);
    
    public static String readTestResourceFile(String fileName) {
        URL fileUrl = TestUtils.class.getClassLoader().getResource(fileName);
        try {
            Path resPath = Paths.get(fileUrl.toURI());
            return Files.readString(resPath);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    
    public static void verifiserAsynkront(long timeout, TimeUnit unit, Runnable verifiser) {
        long timeoutMillis = unit.toMillis(timeout);
        boolean prosessert = false;
        boolean timedOut = false;
        long start = System.currentTimeMillis();
        while (!prosessert) {
            try {
                Thread.sleep(10);
                long current = System.currentTimeMillis();
                timedOut = current - start > timeoutMillis;
                verifiser.run();
                prosessert = true;
            } catch (Throwable a) {
                if (timedOut) {
                    log.error("verifiserAsynkront feilet", a);
                }
            }
        }
    }
}
