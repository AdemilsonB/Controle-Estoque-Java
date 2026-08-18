package com.estoque.repository;

import com.estoque.entity.Colecao;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Transactional;
import com.estoque.config.PersistenceConfig;
import com.estoque.config.FlywayConfig;
import com.estoque.config.SecurityConfig;

import static org.assertj.core.api.Assertions.assertThat;

@SpringJUnitConfig(classes = {PersistenceConfig.class, FlywayConfig.class, SecurityConfig.class})
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.profiles.active=test")
@Transactional
class ColecaoRepositoryTest {

    @Autowired
    private ColecaoRepository colecaoRepository;

    @Test
    void devePersistirEBuscarColecao() {
        Colecao colecao = Colecao.builder().nome("Primavera 2026").descricao("Coleção primavera").build();

        Colecao salva = colecaoRepository.save(colecao);

        assertThat(salva.getId()).isNotNull();
        assertThat(colecaoRepository.existsByNomeIgnoreCase("primavera 2026")).isTrue();
    }
}
