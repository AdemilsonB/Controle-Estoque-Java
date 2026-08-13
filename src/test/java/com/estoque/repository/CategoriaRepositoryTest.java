package com.estoque.repository;

import com.estoque.entity.Categoria;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class CategoriaRepositoryTest {

    @org.springframework.beans.factory.annotation.Autowired
    private CategoriaRepository categoriaRepository;

    @Test
    void devePersistirEBuscarCategoria() {
        Categoria categoria = Categoria.builder().nome("Calçados").descricao("Tênis e sapatos").build();

        Categoria salva = categoriaRepository.save(categoria);

        assertThat(salva.getId()).isNotNull();
        assertThat(categoriaRepository.existsByNomeIgnoreCase("calçados")).isTrue();
    }
}
