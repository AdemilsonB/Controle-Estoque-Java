package com.estoque.controller;

import com.estoque.dto.request.EnderecoRequest;
import com.estoque.dto.request.FuncionarioRequest;
import com.estoque.entity.Endereco;
import com.estoque.entity.Funcionario;
import com.estoque.enums.Role;
import com.estoque.repository.FuncionarioRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import com.estoque.config.PersistenceConfig;
import com.estoque.config.FlywayConfig;
import com.estoque.config.WebConfig;
import com.estoque.config.SecurityConfig;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringJUnitConfig(classes = {PersistenceConfig.class, FlywayConfig.class, WebConfig.class, SecurityConfig.class})
@WebAppConfiguration
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.profiles.active=test")
class FuncionarioControllerIT {

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
    @WithUserDetails(value = "admin@estoque.com", userDetailsServiceBeanName = "userDetailsServiceImpl")
    void deveRetornarPerfilDoUsuarioAutenticado() throws Exception {
        mockMvc.perform(get("/api/v1/funcionarios/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("admin@estoque.com"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deveCriarFuncionarioComSucesso() throws Exception {
        FuncionarioRequest request = new FuncionarioRequest("Ana", "Silva", "11122233344",
                "ana@estoque.com", "senha123", "F001", LocalDate.now(), "Almoxarifado", Role.OPERADOR,
                new EnderecoRequest("Rua B", "10", "Centro", "Curitiba", "PR", "80000-000"));
        String corpo = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/api/v1/funcionarios").contentType("application/json").content(corpo))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.nome").value("Ana"))
                .andExpect(jsonPath("$.senha").doesNotExist())
                .andExpect(jsonPath("$.ativo").value(true));

        Funcionario salvo = funcionarioRepository.findByEmail("ana@estoque.com").orElseThrow();
        assertThat(salvo.getSenha()).startsWith("$2");
        assertThat(passwordEncoder.matches("senha123", salvo.getSenha())).isTrue();
    }

    @Test
    @WithMockUser(roles = "OPERADOR")
    void deveRejeitarCriacaoParaOperadorCom403() throws Exception {
        FuncionarioRequest request = new FuncionarioRequest("Bruno", "Costa", "22233344455",
                "bruno@estoque.com", "senha123", "F002", LocalDate.now(), "Vendas", Role.OPERADOR,
                new EnderecoRequest("Rua C", "20", "Centro", "Curitiba", "PR", "80000-000"));
        String corpo = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/api/v1/funcionarios").contentType("application/json").content(corpo))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deveAtualizarFuncionarioComSucesso() throws Exception {
        Funcionario funcionario = Funcionario.builder()
                .nome("Carla").sobrenome("Dias").cpf("33344455566").email("carla@estoque.com")
                .senha(passwordEncoder.encode("senhaOriginal")).matricula("F003").dataAdmissao(LocalDate.now())
                .setor("Estoque").role(Role.OPERADOR)
                .endereco(new Endereco("Rua D", "30", "Centro", "Curitiba", "PR", "80000-000"))
                .build();
        Funcionario salvo = funcionarioRepository.save(funcionario);

        FuncionarioRequest request = new FuncionarioRequest("Carla", "Dias Atualizada", "33344455566",
                "carla.nova@estoque.com", "senhaNova123", "F003", LocalDate.now(), "Logística", Role.ADMIN,
                new EnderecoRequest("Rua E", "40", "Bairro Novo", "Rio de Janeiro", "RJ", "20000-000"));
        String corpo = objectMapper.writeValueAsString(request);

        mockMvc.perform(put("/api/v1/funcionarios/" + salvo.getId()).contentType("application/json").content(corpo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sobrenome").value("Dias Atualizada"))
                .andExpect(jsonPath("$.email").value("carla.nova@estoque.com"))
                .andExpect(jsonPath("$.setor").value("Logística"))
                .andExpect(jsonPath("$.endereco.cidade").value("Rio de Janeiro"))
                .andExpect(jsonPath("$.senha").doesNotExist());

        Funcionario atualizado = funcionarioRepository.findById(salvo.getId()).orElseThrow();
        assertThat(atualizado.getSenha()).startsWith("$2");
        assertThat(passwordEncoder.matches("senhaNova123", atualizado.getSenha())).isTrue();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deveExcluirFuncionarioComSucesso() throws Exception {
        Funcionario funcionario = Funcionario.builder()
                .nome("Diego").sobrenome("Melo").cpf("44455566677").email("diego@estoque.com")
                .senha(passwordEncoder.encode("senha123")).matricula("F004").dataAdmissao(LocalDate.now())
                .setor("Vendas").role(Role.OPERADOR)
                .endereco(new Endereco("Rua F", "50", "Centro", "Curitiba", "PR", "80000-000"))
                .build();
        Funcionario salvo = funcionarioRepository.save(funcionario);

        mockMvc.perform(delete("/api/v1/funcionarios/" + salvo.getId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/funcionarios/" + salvo.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deveRetornar404AoBuscarFuncionarioInexistente() throws Exception {
        mockMvc.perform(get("/api/v1/funcionarios/999999"))
                .andExpect(status().isNotFound());
    }
}
