package no.nav.veilarboppfolging.test;

import lombok.SneakyThrows;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

public class TestUtils {

    @SneakyThrows
    public static String readTestResourceFile(String fileName) {
        URL fileUrl = TestUtils.class.getClassLoader().getResource(fileName);
        Path resPath = Paths.get(fileUrl.toURI());
        return Files.readString(resPath);
    }

    @SneakyThrows
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
                    throw a;
                }
            }
        }
    }
}
