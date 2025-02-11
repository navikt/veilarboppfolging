package no.nav.veilarboppfolging;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;

@EnableAutoConfiguration
public class TestApplication {

    //TODO Må fikse AuthService og AuthContextHolder mock
    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(TestApplication.class);
        application.setAdditionalProfiles("local");
        application.run(args);
    }
}
