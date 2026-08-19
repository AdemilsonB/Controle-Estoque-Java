package com.estoque.config;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

import javax.sql.DataSource;

@Configuration
public class FlywayConfig {

    @Value("${flyway.locations}")
    private String flywayLocations;

    // static: precisa rodar antes de qualquer @Value/${...} ser resolvido no restante do contexto.
    @Bean
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }

    @Bean
    @DependsOn("propertySourcesPlaceholderConfigurer")
    public Flyway flyway(DataSource dataSource) {
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations(flywayLocations)
                .load();
        flyway.migrate();
        return flyway;
    }
}
