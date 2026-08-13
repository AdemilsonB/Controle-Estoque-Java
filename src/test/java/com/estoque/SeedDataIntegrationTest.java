package com.estoque;

import com.estoque.repository.CategoriaRepository;
import com.estoque.repository.ColecaoRepository;
import com.estoque.repository.FuncionarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
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
