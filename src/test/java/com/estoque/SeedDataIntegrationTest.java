package com.estoque;

import com.estoque.repository.CategoriaRepository;
import com.estoque.repository.ColecaoRepository;
import com.estoque.repository.FuncionarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import com.estoque.config.PersistenceConfig;
import com.estoque.config.FlywayConfig;
import com.estoque.config.SecurityConfig;

import static org.assertj.core.api.Assertions.assertThat;

@SpringJUnitConfig(classes = {PersistenceConfig.class, FlywayConfig.class, SecurityConfig.class})
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.profiles.active=test")
class SeedDataIntegrationTest {

    @Autowired private FuncionarioRepository funcionarioRepository;
    @Autowired private CategoriaRepository categoriaRepository;
    @Autowired private ColecaoRepository colecaoRepository;

    @Test
    void deveCarregarDadosDeSeedNaInicializacao() {
        assertThat(funcionarioRepository.findByEmail("admin@estoque.com")).isPresent();
        assertThat(categoriaRepository.count()).isGreaterThanOrEqualTo(3);
        assertThat(colecaoRepository.count()).isGreaterThanOrEqualTo(2);
    }
}
