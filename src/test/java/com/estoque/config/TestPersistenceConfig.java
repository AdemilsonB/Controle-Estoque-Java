package com.estoque.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;

// Marcador: nenhuma classe nova de configuração é necessária além de reaproveitar
// PersistenceConfig/FlywayConfig/WebConfig/SecurityConfig já existentes, ativando o
// perfil "test" (que resolve app-test.properties, ver Task 2).
@Configuration
public class TestPersistenceConfig {
}
