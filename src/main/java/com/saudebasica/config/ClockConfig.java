package com.saudebasica.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Fornece o relogio da aplicacao como bean. Quem precisa da hora atual recebe
 * este Clock por injecao, o que permite aos testes usar um relogio fixo.
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
