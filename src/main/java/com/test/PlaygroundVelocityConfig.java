package com.test;

import org.apache.velocity.app.VelocityEngine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PlaygroundVelocityConfig {

    /**
     * Renders arbitrary Velocity templates provided as raw strings (not from /templates).
     * This is intended for local testing / playground usage.
     */
    @Bean
    public VelocityEngine playgroundVelocityEngine() {
        VelocityEngine engine = new VelocityEngine();
        engine.init();
        return engine;
    }
}


