package no.nav.fo.veilarbsituasjon.config;

import org.springframework.context.annotation.*;
import org.springframework.web.servlet.config.annotation.*;

@EnableWebMvc
@ComponentScan(basePackages = "no.nav.fo.veilarbsituasjon")
@Import(SwaggerConfig.class)
@Configuration
public class SwaggerUIConfig extends WebMvcConfigurerAdapter {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("swagger-ui.html")
                .addResourceLocations("classpath:/META-INF/resources/");

        registry.addResourceHandler("/webjars/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/");
    }
}
