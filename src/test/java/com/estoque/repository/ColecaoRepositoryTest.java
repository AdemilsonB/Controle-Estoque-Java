package com.estoque.repository;

import com.estoque.entity.Colecao;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class ColecaoRepositoryTest {

    @Autowired
    private ColecaoRepository colecaoRepository;

    @Test
    void devePersistirEBuscarColecao() {
        Colecao colecao = Colecao.builder().nome("Verão 2026").descricao("Coleção verão").build();

        Colecao salva = colecaoRepository.save(colecao);

        assertThat(salva.getId()).isNotNull();
        assertThat(colecaoRepository.existsByNomeIgnoreCase("verão 2026")).isTrue();
    }
}
