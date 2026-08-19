package com.estoque.repository;

import com.estoque.entity.Categoria;
import org.junit.jupiter.api.Test;
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
class CategoriaRepositoryTest {

    @org.springframework.beans.factory.annotation.Autowired
    private CategoriaRepository categoriaRepository;

    @Test
    void devePersistirEBuscarCategoria() {
        Categoria categoria = Categoria.builder().nome("Eletrônicos").descricao("Fones e acessórios").build();

        Categoria salva = categoriaRepository.save(categoria);

        assertThat(salva.getId()).isNotNull();
        assertThat(categoriaRepository.existsByNomeIgnoreCase("eletrônicos")).isTrue();
    }
}
