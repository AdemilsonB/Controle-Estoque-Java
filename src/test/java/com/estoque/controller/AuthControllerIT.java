package com.estoque.controller;

import com.estoque.entity.Endereco;
import com.estoque.entity.Funcionario;
import com.estoque.enums.Role;
import com.estoque.repository.FuncionarioRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import com.estoque.config.PersistenceConfig;
import com.estoque.config.FlywayConfig;
import com.estoque.config.WebConfig;
import com.estoque.config.SecurityConfig;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringJUnitConfig(classes = {PersistenceConfig.class, FlywayConfig.class, WebConfig.class, SecurityConfig.class})
@WebAppConfiguration
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.profiles.active=test")
class AuthControllerIT {

    @Autowired private WebApplicationContext webApplicationContext;
    private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private FuncionarioRepository funcionarioRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    void deveAutenticarComCredenciaisValidasERetornarToken() throws Exception {
        String corpo = objectMapper.writeValueAsString(
                new com.estoque.dto.request.LoginRequest("admin@estoque.com", "admin123"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content(corpo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tipo").value("Bearer"));
    }

    @Test
    void deveRejeitarCredenciaisInvalidasCom401() throws Exception {
        String corpo = objectMapper.writeValueAsString(
                new com.estoque.dto.request.LoginRequest("admin@estoque.com", "senha-errada"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content(corpo))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deveRejeitarRequisicaoSemTokenCom401() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/produtos"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andReturn();

        // Verify UTF-8 character encoding to prevent mojibake in Portuguese accented characters
        assertThat(result.getResponse().getCharacterEncoding()).isEqualTo("UTF-8");
    }

    @Test
    void deveRejeitarRequisicaoComTokenMalformadoCom401() throws Exception {
        mockMvc.perform(get("/api/v1/produtos")
                        .header("Authorization", "Bearer garbage-not-a-real-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deveRejeitarLoginDeFuncionarioInativoCom401() throws Exception {
        Endereco endereco = new Endereco("Rua C", "20", "Centro", "Curitiba", "PR", "80000-000");
        Funcionario inativo = Funcionario.builder()
                .nome("Carlos")
                .sobrenome("Souza")
                .cpf("99988877766")
                .email("carlos.inativo@estoque.com")
                .senha(passwordEncoder.encode("senha123"))
                .matricula("F999")
                .dataAdmissao(LocalDate.now())
                .setor("Almoxarifado")
                .role(Role.OPERADOR)
                .endereco(endereco)
                .build();
        inativo.desativar();
        funcionarioRepository.save(inativo);

        String corpo = objectMapper.writeValueAsString(
                new com.estoque.dto.request.LoginRequest("carlos.inativo@estoque.com", "senha123"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content(corpo))
                .andExpect(status().isUnauthorized());
    }
}
