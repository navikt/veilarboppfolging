package no.nav.fo.veilarbsituasjon.config;

import no.nav.fo.veilarbsituasjon.HelloController;
import org.springframework.context.annotation.*;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

//@EnableAspectJAutoProxy
@Configuration
@EnableWebMvc
//@Import({
//})
@ComponentScan(basePackages = "no.nav.fo.veilarbsituasjon")
public class ApplicationConfig {

//    @Bean
//    public HelloController helloController() {
//        return new HelloController();
//    }

}