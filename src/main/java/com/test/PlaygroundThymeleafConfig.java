package com.test;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.StringTemplateResolver;

@Configuration
public class PlaygroundThymeleafConfig {

    /**
     * Renders arbitrary Thymeleaf templates provided as raw strings (not from /templates).
     * This is intended for local testing / playground usage.
     */
    @Bean
    public TemplateEngine playgroundTemplateEngine() {
        StringTemplateResolver resolver = new StringTemplateResolver();
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCacheable(false);

        // Use Spring's dialect (SpEL) instead of Thymeleaf Standard Dialect (OGNL),
        // so we don't need OGNL on the classpath and behavior matches Spring Boot templates.
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        return engine;
    }
}


