# Controle de Estoque — Migração para Spring Boot — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Reescrever o Controle de Estoque (hoje um app Java de console em memória) como uma API REST Spring Boot 3 completa, com persistência real, segurança JWT, tratamento de exceções padronizado, controle de concorrência no estoque, documentação OpenAPI e testes.

**Architecture:** Camadas `controller → service → repository`, DTOs (records) na borda da API, entidades JPA isoladas do transporte HTTP, exceptions de negócio mapeadas via `@RestControllerAdvice` + `ProblemDetail`, segurança stateless com JWT, lock otimista (`@Version`) em `Produto` para concorrência em movimentações de estoque.

**Tech Stack:** Java 17, Spring Boot 3.3.4, Spring Web, Spring Data JPA, Spring Security 6, Flyway, H2 (dev/test) + PostgreSQL (prod), springdoc-openapi 2.6, MapStruct 1.6, Lombok, JJWT 0.12, JUnit 5, Mockito, MockMvc, Maven.

## Global Constraints

- Java 17, Spring Boot 3.3.4, Maven (groupId `com.estoque`, artifactId `controle-estoque`).
- Injeção de dependência exclusivamente via construtor (`private final` + `@RequiredArgsConstructor` ou construtor explícito). Nunca `@Autowired` em campo.
- Toda entidade JPA: construtor no-args `protected` (Hibernate) via `@NoArgsConstructor(access = AccessLevel.PROTECTED)`, e construção válida via `@Builder` ou construtor público explícito — nunca monta objeto incompleto via setters soltos.
- Associações `@ManyToOne` sempre `fetch = FetchType.LAZY`.
- Enums persistidos sempre com `@Enumerated(EnumType.STRING)`.
- Toda escrita em service: `@Transactional`. Toda leitura: `@Transactional(readOnly = true)`.
- Toda resposta de erro: `ProblemDetail` (RFC 7807) via `GlobalExceptionHandler`, nunca stacktrace exposta.
- Toda rota da API sob prefixo `/api/v1`.
- Controllers nunca retornam entidade JPA diretamente — sempre DTO de resposta (record).
- Sem Testcontainers — testes rodam com H2, perfil `test`.
- Sem menção de coautoria de IA em nenhum commit, comentário ou arquivo.

---

## Task 1: Scaffolding do projeto Maven

**Files:**
- Create: `pom.xml`
- Create: `src/main/java/com/estoque/EstoqueApplication.java`
- Create: `src/main/resources/application.yml`
- Create: `src/main/resources/application-test.yml`
- Create: `src/test/java/com/estoque/EstoqueApplicationTests.java`
- Create: `.gitignore`
- Delete: `src/models/`, `src/views/`, `src/controllers/`, `bin/`, `.classpath`, `.project`, `.metadata/`

**Interfaces:**
- Produces: projeto Maven compilável, `EstoqueApplication` como classe main, perfis `default` (H2) e `test` (H2) configurados, `mvn spring-boot:run` sobe a aplicação vazia na porta 8080.

- [ ] **Step 1: Remover o código legado**

```bash
git rm -r src/models src/views src/controllers bin .classpath .project
git rm -r .metadata
```

- [ ] **Step 2: Criar `.gitignore`**

```gitignore
target/
*.class
.idea/
*.iml
.vscode/
.settings/
.classpath
.project
.metadata/
bin/
*.log
```

- [ ] **Step 3: Criar `pom.xml`**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.3.4</version>
        <relativePath/>
    </parent>

    <groupId>com.estoque</groupId>
    <artifactId>controle-estoque</artifactId>
    <version>1.0.0</version>
    <name>controle-estoque</name>
    <description>API REST de controle de estoque</description>

    <properties>
        <java.version>17</java.version>
        <mapstruct.version>1.6.2</mapstruct.version>
        <jjwt.version>0.12.6</jjwt.version>
        <springdoc.version>2.6.0</springdoc.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-core</artifactId>
        </dependency>
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-database-postgresql</artifactId>
        </dependency>
        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>org.springdoc</groupId>
            <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
            <version>${springdoc.version}</version>
        </dependency>
        <dependency>
            <groupId>org.mapstruct</groupId>
            <artifactId>mapstruct</artifactId>
            <version>${mapstruct.version}</version>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-api</artifactId>
            <version>${jjwt.version}</version>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-impl</artifactId>
            <version>${jjwt.version}</version>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-jackson</artifactId>
            <version>${jjwt.version}</version>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.security</groupId>
            <artifactId>spring-security-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <configuration>
                    <annotationProcessorPaths>
                        <path>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                            <version>${lombok.version}</version>
                        </path>
                        <path>
                            <groupId>org.mapstruct</groupId>
                            <artifactId>mapstruct-processor</artifactId>
                            <version>${mapstruct.version}</version>
                        </path>
                        <path>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok-mapstruct-binding</artifactId>
                            <version>0.2.0</version>
                        </path>
                    </annotationProcessorPaths>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 4: Criar a classe principal**

```java
package com.estoque;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class EstoqueApplication {
    public static void main(String[] args) {
        SpringApplication.run(EstoqueApplication.class, args);
    }
}
```

- [ ] **Step 5: Criar `application.yml` (perfil padrão = dev, H2)**

```yaml
spring:
  application:
    name: controle-estoque
  datasource:
    url: jdbc:h2:mem:estoque;DB_CLOSE_DELAY=-1
    username: sa
    password: ""
    driver-class-name: org.h2.Driver
  jpa:
    hibernate:
      ddl-auto: validate
    open-in-view: false
    properties:
      hibernate:
        format_sql: true
  flyway:
    enabled: true
    locations: classpath:db/migration
  h2:
    console:
      enabled: true
      path: /h2-console

server:
  port: 8080

jwt:
  secret: ${JWT_SECRET:MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDEyMzQ1Njc4OTAxMjM0NTY3ODk=}
  expiration-ms: ${JWT_EXPIRATION_MS:3600000}

springdoc:
  swagger-ui:
    path: /swagger-ui.html

logging:
  level:
    com.estoque: INFO
```

- [ ] **Step 6: Criar `application-test.yml`**

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:estoque-test;DB_CLOSE_DELAY=-1
  flyway:
    clean-disabled: false

jwt:
  secret: MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDEyMzQ1Njc4OTAxMjM0NTY3ODk=
  expiration-ms: 3600000
```

- [ ] **Step 7: Criar teste de contexto**

```java
package com.estoque;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class EstoqueApplicationTests {
    @Test
    void contextLoads() {
    }
}
```

Nesta task ainda não existe nenhuma entidade `@Entity` nem migration alguma — a pasta `src/main/resources/db/migration` fica vazia por enquanto. Com `ddl-auto: validate` e zero entidades, o Hibernate não tem nada para validar e o Flyway apenas cria a tabela de controle `flyway_schema_history` vazia; o boot funciona normalmente. `ddl-auto` permanece `validate` a partir daqui — a Task 3 introduz o primeiro schema real (`V1__schema_categoria_colecao.sql`) e as entidades correspondentes, sem exigir nenhum ajuste retroativo neste arquivo.

- [ ] **Step 8: Rodar o build e o teste de contexto**

Run: `mvn -q clean test`
Expected: BUILD SUCCESS, `contextLoads` passa.

- [ ] **Step 9: Commit**

```bash
git add -A
git commit -m "chore: substituir app Java legado por scaffolding Spring Boot"
```

---

## Task 2: Enums e Endereco (value object)

**Files:**
- Create: `src/main/java/com/estoque/enums/Role.java`
- Create: `src/main/java/com/estoque/enums/MotivoSaida.java`
- Create: `src/main/java/com/estoque/entity/Endereco.java`
- Test: `src/test/java/com/estoque/entity/EnderecoTest.java`

**Interfaces:**
- Produces: `Role` (`ADMIN`, `OPERADOR`), `MotivoSaida` (`VENDA`, `PERDA`, `AJUSTE`, `DEVOLUCAO`), `Endereco` (`@Embeddable`) com campos `logradouro, numero, bairro, cidade, estado, cep`, construtor `Endereco(String logradouro, String numero, String bairro, String cidade, String estado, String cep)` e no-args protegido.

- [ ] **Step 1: Criar os enums**

```java
package com.estoque.enums;

public enum Role {
    ADMIN,
    OPERADOR
}
```

```java
package com.estoque.enums;

public enum MotivoSaida {
    VENDA,
    PERDA,
    AJUSTE,
    DEVOLUCAO
}
```

- [ ] **Step 2: Escrever o teste de `Endereco` (falha: classe não existe)**

```java
package com.estoque.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EnderecoTest {
    @Test
    void deveConstruirEnderecoValido() {
        Endereco endereco = new Endereco("Rua A", "100", "Centro", "Curitiba", "PR", "80000-000");

        assertThat(endereco.getLogradouro()).isEqualTo("Rua A");
        assertThat(endereco.getEstado()).isEqualTo("PR");
        assertThat(endereco.getCep()).isEqualTo("80000-000");
    }
}
```

- [ ] **Step 3: Rodar o teste e confirmar falha de compilação**

Run: `mvn -q -Dtest=EnderecoTest test`
Expected: FAIL — `Endereco` não existe.

- [ ] **Step 4: Implementar `Endereco`**

```java
package com.estoque.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Endereco {

    @Column(name = "logradouro", nullable = false, length = 150)
    private String logradouro;

    @Column(name = "numero", nullable = false, length = 20)
    private String numero;

    @Column(name = "bairro", nullable = false, length = 100)
    private String bairro;

    @Column(name = "cidade", nullable = false, length = 100)
    private String cidade;

    @Column(name = "estado", nullable = false, length = 2)
    private String estado;

    @Column(name = "cep", nullable = false, length = 9)
    private String cep;
}
```

- [ ] **Step 5: Rodar o teste e confirmar sucesso**

Run: `mvn -q -Dtest=EnderecoTest test`
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/estoque/enums src/main/java/com/estoque/entity/Endereco.java src/test/java/com/estoque/entity/EnderecoTest.java
git commit -m "feat: adicionar enums de dominio e value object Endereco"
```

---

## Task 3: Categoria e Colecao (entidades + repositórios)

**Files:**
- Create: `src/main/java/com/estoque/entity/Categoria.java`
- Create: `src/main/java/com/estoque/entity/Colecao.java`
- Create: `src/main/java/com/estoque/repository/CategoriaRepository.java`
- Create: `src/main/java/com/estoque/repository/ColecaoRepository.java`
- Create: `src/main/resources/db/migration/V1__schema_categoria_colecao.sql`
- Test: `src/test/java/com/estoque/repository/CategoriaRepositoryTest.java`
- Test: `src/test/java/com/estoque/repository/ColecaoRepositoryTest.java`

**Interfaces:**
- Consumes: nenhum (primeira migration real).
- Produces: `Categoria{id, nome, descricao}`, `Colecao{id, nome, descricao}`, `CategoriaRepository extends JpaRepository<Categoria, Long>` com `boolean existsByNomeIgnoreCase(String nome)`, `ColecaoRepository extends JpaRepository<Colecao, Long>` com `boolean existsByNomeIgnoreCase(String nome)`.

- [ ] **Step 1: Escrever o teste de persistência de `Categoria` (falha: classe não existe)**

```java
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
```

- [ ] **Step 2: Rodar e confirmar falha**

Run: `mvn -q -Dtest=CategoriaRepositoryTest test`
Expected: FAIL — `Categoria`/`CategoriaRepository` não existem.

- [ ] **Step 3: Implementar `Categoria`, `Colecao` e repositórios**

```java
package com.estoque.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "categoria", uniqueConstraints = @jakarta.persistence.UniqueConstraint(columnNames = "nome"))
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Categoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nome", nullable = false, unique = true, length = 80)
    private String nome;

    @Column(name = "descricao", length = 255)
    private String descricao;

    @Builder
    public Categoria(String nome, String descricao) {
        this.nome = nome;
        this.descricao = descricao;
    }
}
```

```java
package com.estoque.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "colecao", uniqueConstraints = @UniqueConstraint(columnNames = "nome"))
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Colecao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nome", nullable = false, unique = true, length = 80)
    private String nome;

    @Column(name = "descricao", length = 255)
    private String descricao;

    @Builder
    public Colecao(String nome, String descricao) {
        this.nome = nome;
        this.descricao = descricao;
    }
}
```

```java
package com.estoque.repository;

import com.estoque.entity.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {
    boolean existsByNomeIgnoreCase(String nome);
}
```

```java
package com.estoque.repository;

import com.estoque.entity.Colecao;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ColecaoRepository extends JpaRepository<Colecao, Long> {
    boolean existsByNomeIgnoreCase(String nome);
}
```

- [ ] **Step 4: Criar a migration `V1__schema_categoria_colecao.sql`**

```sql
CREATE TABLE categoria (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    nome VARCHAR(80) NOT NULL UNIQUE,
    descricao VARCHAR(255)
);

CREATE TABLE colecao (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    nome VARCHAR(80) NOT NULL UNIQUE,
    descricao VARCHAR(255)
);
```

- [ ] **Step 5: Escrever o teste de `Colecao` (mesmo padrão do Step 1, para `Colecao`)**

```java
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
```

- [ ] **Step 6: Rodar os testes e confirmar sucesso**

Run: `mvn -q -Dtest=CategoriaRepositoryTest,ColecaoRepositoryTest test`
Expected: PASS

- [ ] **Step 7: Commit**

```bash
git add src/main/java/com/estoque/entity/Categoria.java src/main/java/com/estoque/entity/Colecao.java src/main/java/com/estoque/repository/CategoriaRepository.java src/main/java/com/estoque/repository/ColecaoRepository.java src/main/resources/db/migration/V1__schema_categoria_colecao.sql src/test/java/com/estoque/repository/CategoriaRepositoryTest.java src/test/java/com/estoque/repository/ColecaoRepositoryTest.java src/main/resources/application.yml
git commit -m "feat: adicionar entidades Categoria e Colecao com migration e repositorios"
```

---

## Task 4: Fornecedor (entidade + repositório)

**Files:**
- Create: `src/main/java/com/estoque/entity/Fornecedor.java`
- Create: `src/main/java/com/estoque/repository/FornecedorRepository.java`
- Create: `src/main/resources/db/migration/V2__schema_fornecedor.sql`
- Test: `src/test/java/com/estoque/repository/FornecedorRepositoryTest.java`

**Interfaces:**
- Consumes: `Endereco` (Task 2).
- Produces: `Fornecedor{id, cnpj, razaoSocial, telefone, email, endereco, ativo, criadoEm, atualizadoEm}`, `FornecedorRepository` com `boolean existsByCnpj(String cnpj)`, `Optional<Fornecedor> findByIdAndAtivoTrue(Long id)`, `Page<Fornecedor> findByAtivoTrue(Pageable pageable)`.

- [ ] **Step 1: Escrever o teste (falha: classe não existe)**

```java
package com.estoque.repository;

import com.estoque.entity.Endereco;
import com.estoque.entity.Fornecedor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class FornecedorRepositoryTest {

    @Autowired
    private FornecedorRepository fornecedorRepository;

    @Test
    void devePersistirFornecedorComEnderecoEmbutido() {
        Endereco endereco = new Endereco("Av. Brasil", "500", "Centro", "São Paulo", "SP", "01000-000");
        Fornecedor fornecedor = Fornecedor.builder()
                .cnpj("12345678000199")
                .razaoSocial("Fornecedor Exemplo LTDA")
                .telefone("11999999999")
                .email("contato@fornecedor.com")
                .endereco(endereco)
                .build();

        Fornecedor salvo = fornecedorRepository.save(fornecedor);

        assertThat(salvo.getId()).isNotNull();
        assertThat(salvo.isAtivo()).isTrue();
        assertThat(fornecedorRepository.existsByCnpj("12345678000199")).isTrue();
    }
}
```

- [ ] **Step 2: Rodar e confirmar falha**

Run: `mvn -q -Dtest=FornecedorRepositoryTest test`
Expected: FAIL — `Fornecedor` não existe.

- [ ] **Step 3: Implementar `Fornecedor`**

```java
package com.estoque.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "fornecedor", uniqueConstraints = @UniqueConstraint(columnNames = "cnpj"))
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Fornecedor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cnpj", nullable = false, unique = true, length = 14)
    private String cnpj;

    @Column(name = "razao_social", nullable = false, length = 150)
    private String razaoSocial;

    @Column(name = "telefone", nullable = false, length = 20)
    private String telefone;

    @Column(name = "email", nullable = false, length = 150)
    private String email;

    @Embedded
    private Endereco endereco;

    @Column(name = "ativo", nullable = false)
    private boolean ativo;

    @CreatedDate
    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @LastModifiedDate
    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;

    @Builder
    public Fornecedor(String cnpj, String razaoSocial, String telefone, String email, Endereco endereco) {
        this.cnpj = cnpj;
        this.razaoSocial = razaoSocial;
        this.telefone = telefone;
        this.email = email;
        this.endereco = endereco;
        this.ativo = true;
    }

    public void desativar() {
        this.ativo = false;
    }
}
```

```java
package com.estoque.repository;

import com.estoque.entity.Fornecedor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FornecedorRepository extends JpaRepository<Fornecedor, Long> {
    boolean existsByCnpj(String cnpj);
    Optional<Fornecedor> findByIdAndAtivoTrue(Long id);
    Page<Fornecedor> findByAtivoTrue(Pageable pageable);
}
```

- [ ] **Step 4: Criar a migration `V2__schema_fornecedor.sql`**

```sql
CREATE TABLE fornecedor (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    cnpj VARCHAR(14) NOT NULL UNIQUE,
    razao_social VARCHAR(150) NOT NULL,
    telefone VARCHAR(20) NOT NULL,
    email VARCHAR(150) NOT NULL,
    logradouro VARCHAR(150) NOT NULL,
    numero VARCHAR(20) NOT NULL,
    bairro VARCHAR(100) NOT NULL,
    cidade VARCHAR(100) NOT NULL,
    estado VARCHAR(2) NOT NULL,
    cep VARCHAR(9) NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em TIMESTAMP NOT NULL,
    atualizado_em TIMESTAMP NOT NULL
);
```

- [ ] **Step 5: Rodar o teste e confirmar sucesso**

Run: `mvn -q -Dtest=FornecedorRepositoryTest test`
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/estoque/entity/Fornecedor.java src/main/java/com/estoque/repository/FornecedorRepository.java src/main/resources/db/migration/V2__schema_fornecedor.sql src/test/java/com/estoque/repository/FornecedorRepositoryTest.java
git commit -m "feat: adicionar entidade Fornecedor com auditoria e soft delete"
```

---

## Task 5: Funcionario (entidade + repositório)

**Files:**
- Create: `src/main/java/com/estoque/entity/Funcionario.java`
- Create: `src/main/java/com/estoque/repository/FuncionarioRepository.java`
- Create: `src/main/resources/db/migration/V3__schema_funcionario.sql`
- Test: `src/test/java/com/estoque/repository/FuncionarioRepositoryTest.java`

**Interfaces:**
- Consumes: `Endereco` (Task 2), `Role` (Task 2).
- Produces: `Funcionario{id, nome, sobrenome, cpf, email, senha, matricula, dataAdmissao, setor, role, endereco, ativo, criadoEm, atualizadoEm}`, `FuncionarioRepository` com `Optional<Funcionario> findByEmail(String email)`, `boolean existsByCpf(String cpf)`, `boolean existsByEmail(String email)`, `Page<Funcionario> findByAtivoTrue(Pageable pageable)`.

- [ ] **Step 1: Escrever o teste (falha: classe não existe)**

```java
package com.estoque.repository;

import com.estoque.entity.Endereco;
import com.estoque.entity.Funcionario;
import com.estoque.enums.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class FuncionarioRepositoryTest {

    @Autowired
    private FuncionarioRepository funcionarioRepository;

    @Test
    void devePersistirEBuscarFuncionarioPorEmail() {
        Endereco endereco = new Endereco("Rua B", "10", "Centro", "Curitiba", "PR", "80000-000");
        Funcionario funcionario = Funcionario.builder()
                .nome("Ana")
                .sobrenome("Silva")
                .cpf("11122233344")
                .email("ana.silva@estoque.com")
                .senha("hash-fake")
                .matricula("F001")
                .dataAdmissao(LocalDate.now())
                .setor("Almoxarifado")
                .role(Role.OPERADOR)
                .endereco(endereco)
                .build();

        funcionarioRepository.save(funcionario);

        assertThat(funcionarioRepository.findByEmail("ana.silva@estoque.com")).isPresent();
        assertThat(funcionarioRepository.existsByCpf("11122233344")).isTrue();
    }
}
```

- [ ] **Step 2: Rodar e confirmar falha**

Run: `mvn -q -Dtest=FuncionarioRepositoryTest test`
Expected: FAIL — `Funcionario` não existe.

- [ ] **Step 3: Implementar `Funcionario`**

```java
package com.estoque.entity;

import com.estoque.enums.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "funcionario", uniqueConstraints = {
        @UniqueConstraint(columnNames = "cpf"),
        @UniqueConstraint(columnNames = "email")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Funcionario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nome", nullable = false, length = 100)
    private String nome;

    @Column(name = "sobrenome", nullable = false, length = 100)
    private String sobrenome;

    @Column(name = "cpf", nullable = false, unique = true, length = 11)
    private String cpf;

    @Column(name = "email", nullable = false, unique = true, length = 150)
    private String email;

    @Column(name = "senha", nullable = false)
    private String senha;

    @Column(name = "matricula", nullable = false, length = 20)
    private String matricula;

    @Column(name = "data_admissao", nullable = false)
    private LocalDate dataAdmissao;

    @Column(name = "setor", nullable = false, length = 80)
    private String setor;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private Role role;

    @Embedded
    private Endereco endereco;

    @Column(name = "ativo", nullable = false)
    private boolean ativo;

    @CreatedDate
    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @LastModifiedDate
    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;

    @Builder
    public Funcionario(String nome, String sobrenome, String cpf, String email, String senha, String matricula,
                        LocalDate dataAdmissao, String setor, Role role, Endereco endereco) {
        this.nome = nome;
        this.sobrenome = sobrenome;
        this.cpf = cpf;
        this.email = email;
        this.senha = senha;
        this.matricula = matricula;
        this.dataAdmissao = dataAdmissao;
        this.setor = setor;
        this.role = role;
        this.endereco = endereco;
        this.ativo = true;
    }

    public void desativar() {
        this.ativo = false;
    }

    public void trocarSenha(String novaSenhaHash) {
        this.senha = novaSenhaHash;
    }
}
```

```java
package com.estoque.repository;

import com.estoque.entity.Funcionario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FuncionarioRepository extends JpaRepository<Funcionario, Long> {
    Optional<Funcionario> findByEmail(String email);
    boolean existsByCpf(String cpf);
    boolean existsByEmail(String email);
    Page<Funcionario> findByAtivoTrue(Pageable pageable);
}
```

- [ ] **Step 4: Criar a migration `V3__schema_funcionario.sql`**

```sql
CREATE TABLE funcionario (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    sobrenome VARCHAR(100) NOT NULL,
    cpf VARCHAR(11) NOT NULL UNIQUE,
    email VARCHAR(150) NOT NULL UNIQUE,
    senha VARCHAR(255) NOT NULL,
    matricula VARCHAR(20) NOT NULL,
    data_admissao DATE NOT NULL,
    setor VARCHAR(80) NOT NULL,
    role VARCHAR(20) NOT NULL,
    logradouro VARCHAR(150) NOT NULL,
    numero VARCHAR(20) NOT NULL,
    bairro VARCHAR(100) NOT NULL,
    cidade VARCHAR(100) NOT NULL,
    estado VARCHAR(2) NOT NULL,
    cep VARCHAR(9) NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em TIMESTAMP NOT NULL,
    atualizado_em TIMESTAMP NOT NULL
);
```

- [ ] **Step 5: Rodar o teste e confirmar sucesso**

Run: `mvn -q -Dtest=FuncionarioRepositoryTest test`
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/estoque/entity/Funcionario.java src/main/java/com/estoque/repository/FuncionarioRepository.java src/main/resources/db/migration/V3__schema_funcionario.sql src/test/java/com/estoque/repository/FuncionarioRepositoryTest.java
git commit -m "feat: adicionar entidade Funcionario com role e credenciais"
```

---

## Task 6: Produto (entidade com lock otimista + repositório)

**Files:**
- Create: `src/main/java/com/estoque/entity/Produto.java`
- Create: `src/main/java/com/estoque/repository/ProdutoRepository.java`
- Create: `src/main/resources/db/migration/V4__schema_produto.sql`
- Test: `src/test/java/com/estoque/repository/ProdutoRepositoryTest.java`

**Interfaces:**
- Consumes: `Categoria` (Task 3), `Colecao` (Task 3), `Fornecedor` (Task 4).
- Produces: `Produto{id, codigo, nome, descricao, categoria, colecao, fornecedor, precoVenda, custoMedio, quantidadeEstoque, estoqueMinimo, version, ativo, criadoEm, atualizadoEm}` com métodos de domínio `registrarEntrada(int quantidade, BigDecimal custoUnitario)` e `registrarSaida(int quantidade)` (lança `EstoqueInsuficienteException` se saldo insuficiente — a exceção é criada na Task 9; este teste cobre apenas persistência, a lógica de saldo é testada na Task 17/18). `ProdutoRepository` com `boolean existsByCodigo(String codigo)`, `Optional<Produto> findByIdAndAtivoTrue(Long id)`, `Page<Produto> findByAtivoTrue(Pageable pageable)`, `@Query` para estoque baixo.

- [ ] **Step 1: Escrever o teste (falha: classe não existe)**

```java
package com.estoque.repository;

import com.estoque.entity.Categoria;
import com.estoque.entity.Colecao;
import com.estoque.entity.Produto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class ProdutoRepositoryTest {

    @Autowired
    private ProdutoRepository produtoRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private ColecaoRepository colecaoRepository;

    @Test
    void devePersistirEEncontrarProdutosComEstoqueBaixo() {
        Categoria categoria = categoriaRepository.save(Categoria.builder().nome("Camisetas").build());
        Colecao colecao = colecaoRepository.save(Colecao.builder().nome("Inverno").build());

        Produto produtoBaixo = Produto.builder()
                .codigo("SKU-1")
                .nome("Camiseta P")
                .categoria(categoria)
                .colecao(colecao)
                .precoVenda(new BigDecimal("49.90"))
                .estoqueMinimo(10)
                .build();
        produtoBaixo.registrarEntrada(3, new BigDecimal("20.00"));

        Produto produtoOk = Produto.builder()
                .codigo("SKU-2")
                .nome("Camiseta M")
                .categoria(categoria)
                .colecao(colecao)
                .precoVenda(new BigDecimal("49.90"))
                .estoqueMinimo(10)
                .build();
        produtoOk.registrarEntrada(50, new BigDecimal("20.00"));

        produtoRepository.save(produtoBaixo);
        produtoRepository.save(produtoOk);

        List<Produto> abaixoDoMinimo = produtoRepository.buscarComEstoqueAbaixoDoMinimo();

        assertThat(abaixoDoMinimo).extracting(Produto::getCodigo).containsExactly("SKU-1");
    }
}
```

- [ ] **Step 2: Rodar e confirmar falha**

Run: `mvn -q -Dtest=ProdutoRepositoryTest test`
Expected: FAIL — `Produto` não existe.

- [ ] **Step 3: Implementar `Produto`**

```java
package com.estoque.entity;

import com.estoque.exception.EstoqueInsuficienteException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "produto", uniqueConstraints = @UniqueConstraint(columnNames = "codigo"))
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Produto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo", nullable = false, unique = true, length = 40)
    private String codigo;

    @Column(name = "nome", nullable = false, length = 150)
    private String nome;

    @Column(name = "descricao", length = 500)
    private String descricao;

    @ManyToOne(fetch = FetchType.LAZY)
    @jakarta.persistence.JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;

    @ManyToOne(fetch = FetchType.LAZY)
    @jakarta.persistence.JoinColumn(name = "colecao_id")
    private Colecao colecao;

    @ManyToOne(fetch = FetchType.LAZY)
    @jakarta.persistence.JoinColumn(name = "fornecedor_id")
    private Fornecedor fornecedor;

    @Column(name = "preco_venda", nullable = false, precision = 12, scale = 2)
    private BigDecimal precoVenda;

    @Column(name = "custo_medio", nullable = false, precision = 12, scale = 2)
    private BigDecimal custoMedio;

    @Column(name = "quantidade_estoque", nullable = false)
    private int quantidadeEstoque;

    @Column(name = "estoque_minimo", nullable = false)
    private int estoqueMinimo;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @Column(name = "ativo", nullable = false)
    private boolean ativo;

    @CreatedDate
    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @LastModifiedDate
    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;

    @Builder
    public Produto(String codigo, String nome, String descricao, Categoria categoria, Colecao colecao,
                    Fornecedor fornecedor, BigDecimal precoVenda, int estoqueMinimo) {
        this.codigo = codigo;
        this.nome = nome;
        this.descricao = descricao;
        this.categoria = categoria;
        this.colecao = colecao;
        this.fornecedor = fornecedor;
        this.precoVenda = precoVenda;
        this.estoqueMinimo = estoqueMinimo;
        this.custoMedio = BigDecimal.ZERO;
        this.quantidadeEstoque = 0;
        this.ativo = true;
    }

    /** Soma a quantidade recebida e recalcula o custo médio ponderado. */
    public void registrarEntrada(int quantidade, BigDecimal custoUnitario) {
        BigDecimal valorAtual = custoMedio.multiply(BigDecimal.valueOf(quantidadeEstoque));
        BigDecimal valorEntrada = custoUnitario.multiply(BigDecimal.valueOf(quantidade));
        int novaQuantidade = quantidadeEstoque + quantidade;

        this.custoMedio = valorAtual.add(valorEntrada)
                .divide(BigDecimal.valueOf(novaQuantidade), 2, java.math.RoundingMode.HALF_UP);
        this.quantidadeEstoque = novaQuantidade;
    }

    /** Debita a quantidade do estoque; lança se não houver saldo suficiente. */
    public void registrarSaida(int quantidade) {
        if (quantidade > quantidadeEstoque) {
            throw new EstoqueInsuficienteException(codigo, quantidadeEstoque, quantidade);
        }
        this.quantidadeEstoque -= quantidade;
    }

    public boolean estoqueAbaixoDoMinimo() {
        return quantidadeEstoque < estoqueMinimo;
    }

    public void desativar() {
        this.ativo = false;
    }
}
```

```java
package com.estoque.repository;

import com.estoque.entity.Produto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ProdutoRepository extends JpaRepository<Produto, Long> {
    boolean existsByCodigo(String codigo);
    Optional<Produto> findByIdAndAtivoTrue(Long id);
    Page<Produto> findByAtivoTrue(Pageable pageable);

    @Query("SELECT p FROM Produto p WHERE p.ativo = true AND p.quantidadeEstoque < p.estoqueMinimo ORDER BY p.codigo")
    List<Produto> buscarComEstoqueAbaixoDoMinimo();
}
```

Nota: `Produto` referencia `EstoqueInsuficienteException`, criada nesta mesma task (Step 4) como versão mínima; a Task 9 apenas a reencaixa na hierarquia completa de exceptions, mantendo a mesma assinatura de construtor — nenhuma quebra de compatibilidade.

- [ ] **Step 4: Criar `EstoqueInsuficienteException` mínima (será enriquecida na Task 9)**

```java
package com.estoque.exception;

public class EstoqueInsuficienteException extends RuntimeException {
    public EstoqueInsuficienteException(String codigoProduto, int disponivel, int solicitado) {
        super("Estoque insuficiente para o produto %s: disponível %d, solicitado %d"
                .formatted(codigoProduto, disponivel, solicitado));
    }
}
```

- [ ] **Step 5: Criar a migration `V4__schema_produto.sql`**

```sql
CREATE TABLE produto (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    codigo VARCHAR(40) NOT NULL UNIQUE,
    nome VARCHAR(150) NOT NULL,
    descricao VARCHAR(500),
    categoria_id BIGINT NOT NULL REFERENCES categoria(id),
    colecao_id BIGINT REFERENCES colecao(id),
    fornecedor_id BIGINT REFERENCES fornecedor(id),
    preco_venda DECIMAL(12,2) NOT NULL,
    custo_medio DECIMAL(12,2) NOT NULL DEFAULT 0,
    quantidade_estoque INT NOT NULL DEFAULT 0,
    estoque_minimo INT NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em TIMESTAMP NOT NULL,
    atualizado_em TIMESTAMP NOT NULL
);
```

- [ ] **Step 6: Rodar o teste e confirmar sucesso**

Run: `mvn -q -Dtest=ProdutoRepositoryTest test`
Expected: PASS

- [ ] **Step 7: Commit**

```bash
git add src/main/java/com/estoque/entity/Produto.java src/main/java/com/estoque/repository/ProdutoRepository.java src/main/java/com/estoque/exception/EstoqueInsuficienteException.java src/main/resources/db/migration/V4__schema_produto.sql src/test/java/com/estoque/repository/ProdutoRepositoryTest.java
git commit -m "feat: adicionar entidade Produto com lock otimista e regra de estoque"
```

---

## Task 7: MovimentacaoEstoque, Entrada e Saida (herança JOINED + repositórios)

**Files:**
- Create: `src/main/java/com/estoque/entity/MovimentacaoEstoque.java`
- Create: `src/main/java/com/estoque/entity/Entrada.java`
- Create: `src/main/java/com/estoque/entity/Saida.java`
- Create: `src/main/java/com/estoque/repository/EntradaRepository.java`
- Create: `src/main/java/com/estoque/repository/SaidaRepository.java`
- Create: `src/main/java/com/estoque/repository/MovimentacaoEstoqueRepository.java`
- Create: `src/main/resources/db/migration/V5__schema_movimentacao.sql`
- Test: `src/test/java/com/estoque/repository/MovimentacaoEstoqueRepositoryTest.java`

**Interfaces:**
- Consumes: `Produto` (Task 6), `Funcionario` (Task 5), `Fornecedor` (Task 4), `MotivoSaida` (Task 2).
- Produces: `MovimentacaoEstoque{id, produto, funcionario, quantidade, dataMovimentacao, observacao}` (abstrata, `@Inheritance(JOINED)`), `Entrada extends MovimentacaoEstoque {custoUnitario, fornecedor}`, `Saida extends MovimentacaoEstoque {motivo}`. `MovimentacaoEstoqueRepository extends JpaRepository<MovimentacaoEstoque, Long>` com `Page<MovimentacaoEstoque> findByProdutoIdOrderByDataMovimentacaoDesc(Long produtoId, Pageable pageable)`. `EntradaRepository`/`SaidaRepository extends JpaRepository<Entrada/Saida, Long>` com `Page<...> findByProdutoId(Long produtoId, Pageable pageable)`.

- [ ] **Step 1: Escrever o teste (falha: classes não existem)**

```java
package com.estoque.repository;

import com.estoque.entity.Categoria;
import com.estoque.entity.Endereco;
import com.estoque.entity.Entrada;
import com.estoque.entity.Funcionario;
import com.estoque.entity.MovimentacaoEstoque;
import com.estoque.entity.Produto;
import com.estoque.entity.Saida;
import com.estoque.enums.MotivoSaida;
import com.estoque.enums.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class MovimentacaoEstoqueRepositoryTest {

    @Autowired private MovimentacaoEstoqueRepository movimentacaoRepository;
    @Autowired private EntradaRepository entradaRepository;
    @Autowired private SaidaRepository saidaRepository;
    @Autowired private ProdutoRepository produtoRepository;
    @Autowired private CategoriaRepository categoriaRepository;
    @Autowired private FuncionarioRepository funcionarioRepository;

    @Test
    void deveListarMovimentacoesPolimorficasPorProduto() {
        Categoria categoria = categoriaRepository.save(Categoria.builder().nome("Bolsas").build());
        Produto produto = produtoRepository.save(Produto.builder()
                .codigo("SKU-9").nome("Bolsa").categoria(categoria)
                .precoVenda(new BigDecimal("100.00")).estoqueMinimo(5).build());
        Funcionario funcionario = funcionarioRepository.save(Funcionario.builder()
                .nome("Bia").sobrenome("Souza").cpf("99988877766").email("bia@estoque.com")
                .senha("hash").matricula("F002").dataAdmissao(LocalDate.now())
                .setor("Loja").role(Role.OPERADOR)
                .endereco(new Endereco("Rua C", "20", "Centro", "Curitiba", "PR", "80000-001"))
                .build());

        Entrada entrada = Entrada.builder().produto(produto).funcionario(funcionario)
                .quantidade(10).custoUnitario(new BigDecimal("40.00")).build();
        Saida saida = Saida.builder().produto(produto).funcionario(funcionario)
                .quantidade(2).motivo(MotivoSaida.VENDA).build();

        entradaRepository.save(entrada);
        saidaRepository.save(saida);

        Page<MovimentacaoEstoque> pagina = movimentacaoRepository
                .findByProdutoIdOrderByDataMovimentacaoDesc(produto.getId(), PageRequest.of(0, 10));

        assertThat(pagina.getTotalElements()).isEqualTo(2);
    }
}
```

- [ ] **Step 2: Rodar e confirmar falha**

Run: `mvn -q -Dtest=MovimentacaoEstoqueRepositoryTest test`
Expected: FAIL — classes não existem.

- [ ] **Step 3: Implementar `MovimentacaoEstoque`, `Entrada`, `Saida`**

```java
package com.estoque.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "movimentacao_estoque")
@Inheritance(strategy = InheritanceType.JOINED)
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class MovimentacaoEstoque {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "produto_id", nullable = false)
    private Produto produto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "funcionario_id", nullable = false)
    private Funcionario funcionario;

    @Column(name = "quantidade", nullable = false)
    private int quantidade;

    @Column(name = "observacao", length = 255)
    private String observacao;

    @CreatedDate
    @Column(name = "data_movimentacao", nullable = false, updatable = false)
    private LocalDateTime dataMovimentacao;

    protected MovimentacaoEstoque(Produto produto, Funcionario funcionario, int quantidade, String observacao) {
        this.produto = produto;
        this.funcionario = funcionario;
        this.quantidade = quantidade;
        this.observacao = observacao;
    }
}
```

```java
package com.estoque.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "entrada")
@PrimaryKeyJoinColumn(name = "id")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Entrada extends MovimentacaoEstoque {

    @Column(name = "custo_unitario", nullable = false, precision = 12, scale = 2)
    private BigDecimal custoUnitario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fornecedor_id")
    private Fornecedor fornecedor;

    @Builder
    public Entrada(Produto produto, Funcionario funcionario, int quantidade, String observacao,
                    BigDecimal custoUnitario, Fornecedor fornecedor) {
        super(produto, funcionario, quantidade, observacao);
        this.custoUnitario = custoUnitario;
        this.fornecedor = fornecedor;
    }
}
```

```java
package com.estoque.entity;

import com.estoque.enums.MotivoSaida;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "saida")
@PrimaryKeyJoinColumn(name = "id")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Saida extends MovimentacaoEstoque {

    @Enumerated(EnumType.STRING)
    @Column(name = "motivo", nullable = false, length = 20)
    private MotivoSaida motivo;

    @Builder
    public Saida(Produto produto, Funcionario funcionario, int quantidade, String observacao, MotivoSaida motivo) {
        super(produto, funcionario, quantidade, observacao);
        this.motivo = motivo;
    }
}
```

```java
package com.estoque.repository;

import com.estoque.entity.MovimentacaoEstoque;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovimentacaoEstoqueRepository extends JpaRepository<MovimentacaoEstoque, Long> {
    Page<MovimentacaoEstoque> findByProdutoIdOrderByDataMovimentacaoDesc(Long produtoId, Pageable pageable);
}
```

```java
package com.estoque.repository;

import com.estoque.entity.Entrada;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EntradaRepository extends JpaRepository<Entrada, Long> {
    Page<Entrada> findByProdutoId(Long produtoId, Pageable pageable);
}
```

```java
package com.estoque.repository;

import com.estoque.entity.Saida;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SaidaRepository extends JpaRepository<Saida, Long> {
    Page<Saida> findByProdutoId(Long produtoId, Pageable pageable);
}
```

- [ ] **Step 4: Criar a migration `V5__schema_movimentacao.sql`**

```sql
CREATE TABLE movimentacao_estoque (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    produto_id BIGINT NOT NULL REFERENCES produto(id),
    funcionario_id BIGINT NOT NULL REFERENCES funcionario(id),
    quantidade INT NOT NULL,
    observacao VARCHAR(255),
    data_movimentacao TIMESTAMP NOT NULL
);

CREATE TABLE entrada (
    id BIGINT PRIMARY KEY REFERENCES movimentacao_estoque(id),
    custo_unitario DECIMAL(12,2) NOT NULL,
    fornecedor_id BIGINT REFERENCES fornecedor(id)
);

CREATE TABLE saida (
    id BIGINT PRIMARY KEY REFERENCES movimentacao_estoque(id),
    motivo VARCHAR(20) NOT NULL
);
```

- [ ] **Step 5: Rodar o teste e confirmar sucesso**

Run: `mvn -q -Dtest=MovimentacaoEstoqueRepositoryTest test`
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/estoque/entity/MovimentacaoEstoque.java src/main/java/com/estoque/entity/Entrada.java src/main/java/com/estoque/entity/Saida.java src/main/java/com/estoque/repository/MovimentacaoEstoqueRepository.java src/main/java/com/estoque/repository/EntradaRepository.java src/main/java/com/estoque/repository/SaidaRepository.java src/main/resources/db/migration/V5__schema_movimentacao.sql src/test/java/com/estoque/repository/MovimentacaoEstoqueRepositoryTest.java
git commit -m "feat: adicionar hierarquia de movimentacao de estoque (entrada e saida)"
```

---

## Task 8: Seed de dados (Flyway V6) + smoke test

**Files:**
- Create: `src/main/resources/db/migration/V6__seed.sql`
- Test: `src/test/java/com/estoque/SeedDataIntegrationTest.java`

**Interfaces:**
- Consumes: tabelas `funcionario`, `categoria`, `colecao` (Tasks 3, 5).
- Produces: dado seedado — funcionário ADMIN `admin@estoque.com` (senha em texto puro **admin123**, hash BCrypt já calculado abaixo), 3 categorias, 2 coleções. Usado pelos testes de integração das Tasks seguintes e documentado no README (Task 23).

- [ ] **Step 1: Escrever o teste de integração (falha: dado ainda não existe)**

```java
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
```

- [ ] **Step 2: Rodar e confirmar falha**

Run: `mvn -q -Dtest=SeedDataIntegrationTest test`
Expected: FAIL — nenhum funcionário com esse email.

- [ ] **Step 3: Criar a migration `V6__seed.sql`**

O hash abaixo é um BCrypt real (custo 10) gerado para a senha `admin123` — confirmado com round-trip de verificação, não é um valor de exemplo.

```sql
INSERT INTO funcionario (nome, sobrenome, cpf, email, senha, matricula, data_admissao, setor, role,
                          logradouro, numero, bairro, cidade, estado, cep, ativo, criado_em, atualizado_em)
VALUES ('Admin', 'Sistema', '00000000000', 'admin@estoque.com',
        '$2b$10$RCnF8W47c/o7N.ePdJU2Deq/2NNmz9Bje5XhPTj8epL7T0vParxcW',
        'F000', CURRENT_DATE, 'Administração', 'ADMIN',
        'Rua Principal', '1', 'Centro', 'Curitiba', 'PR', '80000-000',
        TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO categoria (nome, descricao) VALUES
    ('Calçados', 'Tênis, sapatos e sandálias'),
    ('Vestuário', 'Camisetas, calças e jaquetas'),
    ('Acessórios', 'Bolsas, cintos e bonés');

INSERT INTO colecao (nome, descricao) VALUES
    ('Verão 2026', 'Coleção lançada em janeiro de 2026'),
    ('Inverno 2026', 'Coleção lançada em junho de 2026');
```

- [ ] **Step 4: Rodar o teste e confirmar sucesso**

Run: `mvn -q -Dtest=SeedDataIntegrationTest test`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/resources/db/migration/V6__seed.sql src/test/java/com/estoque/SeedDataIntegrationTest.java
git commit -m "feat: adicionar seed de dados iniciais (admin, categorias, colecoes)"
```

---

## Task 9: Hierarquia de exceptions + GlobalExceptionHandler (ProblemDetail)

**Files:**
- Create: `src/main/java/com/estoque/exception/BusinessException.java`
- Create: `src/main/java/com/estoque/exception/RecursoNaoEncontradoException.java`
- Create: `src/main/java/com/estoque/exception/RegistroDuplicadoException.java`
- Modify: `src/main/java/com/estoque/exception/EstoqueInsuficienteException.java` (Task 6) — passa a estender `BusinessException`
- Create: `src/main/java/com/estoque/exception/GlobalExceptionHandler.java`
- Test: `src/test/java/com/estoque/exception/GlobalExceptionHandlerTest.java`

**Interfaces:**
- Produces: `BusinessException` (abstract, `RuntimeException`), `RecursoNaoEncontradoException(String recurso, Object id)`, `RegistroDuplicadoException(String mensagem)`, `EstoqueInsuficienteException(String codigoProduto, int disponivel, int solicitado)` (assinatura inalterada). `GlobalExceptionHandler` (`@RestControllerAdvice`) expõe handlers públicos usados diretamente pelo teste unitário: `handleRecursoNaoEncontrado`, `handleEstoqueInsuficiente`, `handleRegistroDuplicado`, `handleValidacao`, `handleIntegridadeDados`, `handleLockOtimista`, `handleCredenciaisInvalidas`, `handleAcessoNegado`, `handleGenerica` — todos retornando `ProblemDetail`.

- [ ] **Step 1: Escrever o teste unitário do handler (falha: classes não existem)**

```java
package com.estoque.exception;

import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void deveMapearRecursoNaoEncontradoPara404() {
        ProblemDetail problem = handler.handleRecursoNaoEncontrado(
                new RecursoNaoEncontradoException("Produto", 99L));

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(problem.getDetail()).contains("Produto").contains("99");
    }

    @Test
    void deveMapearEstoqueInsuficientePara409() {
        ProblemDetail problem = handler.handleEstoqueInsuficiente(
                new EstoqueInsuficienteException("SKU-1", 2, 5));

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
    }

    @Test
    void deveMapearRegistroDuplicadoPara409() {
        ProblemDetail problem = handler.handleRegistroDuplicado(
                new RegistroDuplicadoException("CNPJ já cadastrado"));

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
    }

    @Test
    void deveMapearErroDeValidacaoPara400ComListaDeCampos() throws Exception {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "produtoRequest");
        bindingResult.addError(new FieldError("produtoRequest", "nome", "não pode ser vazio"));
        MethodParameter methodParameter = mock(MethodParameter.class);
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(methodParameter, bindingResult);

        ProblemDetail problem = handler.handleValidacao(ex);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problem.getProperties()).containsKey("errors");
    }

    @Test
    void deveMapearViolacaoDeIntegridadePara409() {
        ProblemDetail problem = handler.handleIntegridadeDados(
                new DataIntegrityViolationException("constraint violada"));

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
    }

    @Test
    void deveMapearLockOtimistaPara409() {
        ProblemDetail problem = handler.handleLockOtimista(
                new ObjectOptimisticLockingFailureException(Object.class, 1L));

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
    }

    @Test
    void deveMapearCredenciaisInvalidasPara401() {
        ProblemDetail problem = handler.handleCredenciaisInvalidas(new BadCredentialsException("inválido"));

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    void deveMapearAcessoNegadoPara403() {
        ProblemDetail problem = handler.handleAcessoNegado(new AccessDeniedException("negado"));

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
    }

    @Test
    void deveMapearExcecaoGenericaPara500SemVazarDetalhe() {
        ProblemDetail problem = handler.handleGenerica(new RuntimeException("detalhe interno sensível"));

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(problem.getDetail()).doesNotContain("detalhe interno sensível");
    }
}
```

- [ ] **Step 2: Rodar e confirmar falha**

Run: `mvn -q -Dtest=GlobalExceptionHandlerTest test`
Expected: FAIL — classes não existem.

- [ ] **Step 3: Implementar a hierarquia de exceptions**

```java
package com.estoque.exception;

public abstract class BusinessException extends RuntimeException {
    protected BusinessException(String message) {
        super(message);
    }
}
```

```java
package com.estoque.exception;

public class RecursoNaoEncontradoException extends BusinessException {
    public RecursoNaoEncontradoException(String recurso, Object id) {
        super("%s não encontrado(a) com id %s".formatted(recurso, id));
    }
}
```

```java
package com.estoque.exception;

public class RegistroDuplicadoException extends BusinessException {
    public RegistroDuplicadoException(String mensagem) {
        super(mensagem);
    }
}
```

- [ ] **Step 4: Ajustar `EstoqueInsuficienteException` para estender `BusinessException`**

```java
package com.estoque.exception;

public class EstoqueInsuficienteException extends BusinessException {
    public EstoqueInsuficienteException(String codigoProduto, int disponivel, int solicitado) {
        super("Estoque insuficiente para o produto %s: disponível %d, solicitado %d"
                .formatted(codigoProduto, disponivel, solicitado));
    }
}
```

- [ ] **Step 5: Implementar `GlobalExceptionHandler`**

```java
package com.estoque.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ProblemDetail handleRecursoNaoEncontrado(RecursoNaoEncontradoException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(EstoqueInsuficienteException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ProblemDetail handleEstoqueInsuficiente(EstoqueInsuficienteException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(RegistroDuplicadoException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ProblemDetail handleRegistroDuplicado(RegistroDuplicadoException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ProblemDetail handleValidacao(MethodArgumentNotValidException ex) {
        List<Map<String, String>> erros = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> Map.of(
                        "campo", fieldError.getField(),
                        "mensagem", mensagemOuPadrao(fieldError)))
                .toList();

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Dados inválidos");
        problem.setProperty("errors", erros);
        return problem;
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ProblemDetail handleIntegridadeDados(DataIntegrityViolationException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "Operação viola uma restrição de integridade dos dados (registro duplicado ou vínculo existente)");
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ProblemDetail handleLockOtimista(ObjectOptimisticLockingFailureException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "O registro foi alterado por outra operação concorrente. Recarregue os dados e tente novamente.");
    }

    @ExceptionHandler(BadCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ProblemDetail handleCredenciaisInvalidas(BadCredentialsException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "E-mail ou senha inválidos");
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ProblemDetail handleAcessoNegado(AccessDeniedException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN,
                "Você não tem permissão para executar esta operação");
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ProblemDetail handleGenerica(Exception ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocorreu um erro inesperado. Contate o suporte.");
    }

    private String mensagemOuPadrao(FieldError fieldError) {
        return fieldError.getDefaultMessage() != null ? fieldError.getDefaultMessage() : "valor inválido";
    }
}
```

- [ ] **Step 6: Rodar o teste e confirmar sucesso**

Run: `mvn -q -Dtest=GlobalExceptionHandlerTest test`
Expected: PASS

- [ ] **Step 7: Commit**

```bash
git add src/main/java/com/estoque/exception src/test/java/com/estoque/exception
git commit -m "feat: adicionar hierarquia de excecoes e handler global com ProblemDetail"
```

---

## Task 10: JwtService (geração e validação de token)

**Files:**
- Create: `src/main/java/com/estoque/security/JwtService.java`
- Test: `src/test/java/com/estoque/security/JwtServiceTest.java`

**Interfaces:**
- Produces: `JwtService` com construtor `JwtService(String secretBase64, long expirationMs)` (Spring injeta via `@Value` num construtor `@Autowired` implícito — ver Task 11 para o bean), métodos `String gerarToken(String subject, Role role)`, `String extrairSubject(String token)`, `boolean tokenValido(String token, String subject)`.

- [ ] **Step 1: Escrever o teste (falha: classe não existe)**

```java
package com.estoque.security;

import com.estoque.enums.Role;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private static final String SECRET = "MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDEyMzQ1Njc4OTAxMjM0NTY3ODk=";

    private final JwtService jwtService = new JwtService(SECRET, 3_600_000L);

    @Test
    void deveGerarTokenEExtrairSubject() {
        String token = jwtService.gerarToken("admin@estoque.com", Role.ADMIN);

        assertThat(jwtService.extrairSubject(token)).isEqualTo("admin@estoque.com");
    }

    @Test
    void deveValidarTokenParaOSubjectCorreto() {
        String token = jwtService.gerarToken("admin@estoque.com", Role.ADMIN);

        assertThat(jwtService.tokenValido(token, "admin@estoque.com")).isTrue();
        assertThat(jwtService.tokenValido(token, "outro@estoque.com")).isFalse();
    }

    @Test
    void deveRejeitarTokenExpirado() throws InterruptedException {
        JwtService servicoComExpiracaoCurta = new JwtService(SECRET, 1L);
        String token = servicoComExpiracaoCurta.gerarToken("admin@estoque.com", Role.ADMIN);

        Thread.sleep(50);

        assertThat(servicoComExpiracaoCurta.tokenValido(token, "admin@estoque.com")).isFalse();
    }
}
```

- [ ] **Step 2: Rodar e confirmar falha**

Run: `mvn -q -Dtest=JwtServiceTest test`
Expected: FAIL — `JwtService` não existe.

- [ ] **Step 3: Implementar `JwtService`**

```java
package com.estoque.security;

import com.estoque.enums.Role;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey chave;
    private final long expiracaoMs;

    public JwtService(@Value("${jwt.secret}") String secretBase64,
                       @Value("${jwt.expiration-ms}") long expiracaoMs) {
        this.chave = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretBase64));
        this.expiracaoMs = expiracaoMs;
    }

    public String gerarToken(String subject, Role role) {
        Date agora = new Date();
        Date expiracao = new Date(agora.getTime() + expiracaoMs);

        return Jwts.builder()
                .subject(subject)
                .claim("role", role.name())
                .issuedAt(agora)
                .expiration(expiracao)
                .signWith(chave)
                .compact();
    }

    public String extrairSubject(String token) {
        return Jwts.parser().verifyWith(chave).build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    public boolean tokenValido(String token, String subject) {
        try {
            String subjectDoToken = extrairSubject(token);
            return subjectDoToken.equals(subject);
        } catch (ExpiredJwtException | JwtException e) {
            return false;
        }
    }
}
```

- [ ] **Step 4: Rodar o teste e confirmar sucesso**

Run: `mvn -q -Dtest=JwtServiceTest test`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/estoque/security/JwtService.java src/test/java/com/estoque/security/JwtServiceTest.java
git commit -m "feat: adicionar JwtService para geracao e validacao de tokens"
```

---

## Task 11: SecurityConfig, JwtAuthFilter, UserDetailsService e AuthController

**Files:**
- Create: `src/main/java/com/estoque/security/FuncionarioUserDetails.java`
- Create: `src/main/java/com/estoque/security/UserDetailsServiceImpl.java`
- Create: `src/main/java/com/estoque/security/JwtAuthFilter.java`
- Create: `src/main/java/com/estoque/security/RestAuthenticationEntryPoint.java`
- Create: `src/main/java/com/estoque/security/RestAccessDeniedHandler.java`
- Create: `src/main/java/com/estoque/config/SecurityConfig.java`
- Create: `src/main/java/com/estoque/dto/request/LoginRequest.java`
- Create: `src/main/java/com/estoque/dto/response/LoginResponse.java`
- Create: `src/main/java/com/estoque/controller/AuthController.java`
- Test: `src/test/java/com/estoque/controller/AuthControllerIT.java`

**Interfaces:**
- Consumes: `JwtService` (Task 10), `Funcionario`/`FuncionarioRepository` (Task 5), dado seed `admin@estoque.com`/`admin123` (Task 8).
- Produces: `POST /api/v1/auth/login` público, retorna `LoginResponse(token, tipo, expiraEmSegundos)`; qualquer outra rota exige `Authorization: Bearer <token>`; respostas de erro de autenticação/autorização no mesmo formato `ProblemDetail` do `GlobalExceptionHandler`.

- [ ] **Step 1: Escrever o teste de integração do login (falha: endpoint não existe)**

```java
package com.estoque.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

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
        mockMvc.perform(get("/api/v1/produtos"))
                .andExpect(status().isUnauthorized());
    }
}
```

- [ ] **Step 2: Rodar e confirmar falha**

Run: `mvn -q -Dtest=AuthControllerIT test`
Expected: FAIL — endpoint/classes não existem (nota: `/api/v1/produtos` ainda não existe nesta task; o teste `deveRejeitarRequisicaoSemTokenCom401` deve retornar 401 mesmo para rota inexistente, pois o `SecurityConfig` intercepta antes do dispatch ao controller — se o Spring Security ainda não estiver configurado, o Spring MVC devolveria 404; após implementar esta task, o filtro de segurança responde 401 antes disso).

- [ ] **Step 3: Implementar `FuncionarioUserDetails` e `UserDetailsServiceImpl`**

```java
package com.estoque.security;

import com.estoque.entity.Funcionario;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

@RequiredArgsConstructor
public class FuncionarioUserDetails implements UserDetails {

    private final Funcionario funcionario;

    public Long getFuncionarioId() {
        return funcionario.getId();
    }

    @Override
    public List<GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + funcionario.getRole().name()));
    }

    @Override
    public String getPassword() {
        return funcionario.getSenha();
    }

    @Override
    public String getUsername() {
        return funcionario.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return funcionario.isAtivo();
    }
}
```

```java
package com.estoque.security;

import com.estoque.repository.FuncionarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final FuncionarioRepository funcionarioRepository;

    @Override
    public UserDetails loadUserByUsername(String email) {
        return funcionarioRepository.findByEmail(email)
                .map(FuncionarioUserDetails::new)
                .orElseThrow(() -> new UsernameNotFoundException("Funcionário não encontrado para o e-mail informado"));
    }
}
```

- [ ] **Step 4: Implementar `JwtAuthFilter`, entry point e access denied handler**

```java
package com.estoque.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String PREFIXO_BEARER = "Bearer ";

    private final JwtService jwtService;
    private final UserDetailsServiceImpl userDetailsService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                     @NonNull HttpServletResponse response,
                                     @NonNull FilterChain filterChain) throws ServletException, IOException {
        String cabecalhoAuth = request.getHeader("Authorization");

        if (cabecalhoAuth == null || !cabecalhoAuth.startsWith(PREFIXO_BEARER)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = cabecalhoAuth.substring(PREFIXO_BEARER.length());
        String email = jwtService.extrairSubject(token);

        if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);

            if (jwtService.tokenValido(token, userDetails.getUsername())) {
                var authentication = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        filterChain.doFilter(request, response);
    }
}
```

```java
package com.estoque.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws java.io.IOException {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED,
                "Autenticação necessária: informe um token Bearer válido");
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), problem);
    }
}
```

```java
package com.estoque.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException)
            throws java.io.IOException {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN,
                "Você não tem permissão para executar esta operação");
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), problem);
    }
}
```

- [ ] **Step 5: Implementar `SecurityConfig`**

```java
package com.estoque.config;

import com.estoque.security.JwtAuthFilter;
import com.estoque.security.RestAccessDeniedHandler;
import com.estoque.security.RestAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private static final String[] ROTAS_PUBLICAS = {
            "/api/v1/auth/login",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/h2-console/**"
    };

    private final JwtAuthFilter jwtAuthFilter;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;
    private final RestAccessDeniedHandler accessDeniedHandler;
    private final UserDetailsService userDetailsService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .headers(headers -> headers.frameOptions(frame -> frame.disable()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(ROTAS_PUBLICAS).permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider(PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
```

- [ ] **Step 6: Implementar DTOs e `AuthController`**

```java
package com.estoque.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank @Email String email,
        @NotBlank String senha) {
}
```

```java
package com.estoque.dto.response;

public record LoginResponse(String token, String tipo, long expiraEmSegundos) {
}
```

```java
package com.estoque.controller;

import com.estoque.dto.request.LoginRequest;
import com.estoque.dto.response.LoginResponse;
import com.estoque.security.FuncionarioUserDetails;
import com.estoque.security.JwtService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @Value("${jwt.expiration-ms}")
    private long expiracaoMs;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        var authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.senha()));

        var userDetails = (FuncionarioUserDetails) authentication.getPrincipal();
        String token = jwtService.gerarToken(userDetails.getUsername(),
                com.estoque.enums.Role.valueOf(userDetails.getAuthorities().iterator().next()
                        .getAuthority().replace("ROLE_", "")));

        return ResponseEntity.ok(new LoginResponse(token, "Bearer", expiracaoMs / 1000));
    }
}
```

- [ ] **Step 7: Rodar o teste e confirmar sucesso**

Run: `mvn -q -Dtest=AuthControllerIT test`
Expected: PASS

- [ ] **Step 8: Commit**

```bash
git add src/main/java/com/estoque/security src/main/java/com/estoque/config/SecurityConfig.java src/main/java/com/estoque/dto/request/LoginRequest.java src/main/java/com/estoque/dto/response/LoginResponse.java src/main/java/com/estoque/controller/AuthController.java src/test/java/com/estoque/controller/AuthControllerIT.java
git commit -m "feat: adicionar autenticacao JWT com Spring Security"
```

---

## Task 12: Categoria — DTOs, mapper, service, controller, testes

**Files:**
- Create: `src/main/java/com/estoque/dto/request/CategoriaRequest.java`
- Create: `src/main/java/com/estoque/dto/response/CategoriaResponse.java`
- Create: `src/main/java/com/estoque/mapper/CategoriaMapper.java`
- Create: `src/main/java/com/estoque/service/CategoriaService.java`
- Create: `src/main/java/com/estoque/service/impl/CategoriaServiceImpl.java`
- Create: `src/main/java/com/estoque/controller/CategoriaController.java`
- Test: `src/test/java/com/estoque/service/CategoriaServiceTest.java`
- Test: `src/test/java/com/estoque/controller/CategoriaControllerIT.java`

**Interfaces:**
- Consumes: `Categoria`/`CategoriaRepository` (Task 3), `GlobalExceptionHandler` (Task 9), `RecursoNaoEncontradoException`/`RegistroDuplicadoException` (Task 9).
- Produces: `CategoriaRequest(String nome, String descricao)`, `CategoriaResponse(Long id, String nome, String descricao)`, `CategoriaService{listar(Pageable), buscarPorId(Long), criar(CategoriaRequest), atualizar(Long, CategoriaRequest), excluir(Long)}`, rotas `GET/POST/PUT/DELETE /api/v1/categorias`.

- [ ] **Step 1: Escrever o teste unitário do service (falha: classes não existem)**

```java
package com.estoque.service;

import com.estoque.dto.request.CategoriaRequest;
import com.estoque.dto.response.CategoriaResponse;
import com.estoque.entity.Categoria;
import com.estoque.exception.RecursoNaoEncontradoException;
import com.estoque.exception.RegistroDuplicadoException;
import com.estoque.mapper.CategoriaMapper;
import com.estoque.repository.CategoriaRepository;
import com.estoque.service.impl.CategoriaServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoriaServiceTest {

    @Mock private CategoriaRepository categoriaRepository;
    @Mock private CategoriaMapper categoriaMapper;
    @InjectMocks private CategoriaServiceImpl categoriaService;

    @Test
    void deveCriarCategoriaQuandoNomeNaoExiste() {
        CategoriaRequest request = new CategoriaRequest("Calçados", "desc");
        Categoria entidade = Categoria.builder().nome("Calçados").descricao("desc").build();
        CategoriaResponse resposta = new CategoriaResponse(1L, "Calçados", "desc");

        when(categoriaRepository.existsByNomeIgnoreCase("Calçados")).thenReturn(false);
        when(categoriaMapper.toEntity(request)).thenReturn(entidade);
        when(categoriaRepository.save(entidade)).thenReturn(entidade);
        when(categoriaMapper.toResponse(entidade)).thenReturn(resposta);

        CategoriaResponse resultado = categoriaService.criar(request);

        assertThat(resultado.nome()).isEqualTo("Calçados");
        verify(categoriaRepository).save(entidade);
    }

    @Test
    void deveLancarExcecaoAoCriarCategoriaComNomeDuplicado() {
        CategoriaRequest request = new CategoriaRequest("Calçados", "desc");
        when(categoriaRepository.existsByNomeIgnoreCase("Calçados")).thenReturn(true);

        assertThatThrownBy(() -> categoriaService.criar(request))
                .isInstanceOf(RegistroDuplicadoException.class);
    }

    @Test
    void deveLancarExcecaoAoBuscarCategoriaInexistente() {
        when(categoriaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoriaService.buscarPorId(99L))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }
}
```

- [ ] **Step 2: Rodar e confirmar falha**

Run: `mvn -q -Dtest=CategoriaServiceTest test`
Expected: FAIL — classes não existem.

- [ ] **Step 3: Implementar DTOs, mapper, service e controller**

```java
package com.estoque.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoriaRequest(
        @NotBlank @Size(max = 80) String nome,
        @Size(max = 255) String descricao) {
}
```

```java
package com.estoque.dto.response;

public record CategoriaResponse(Long id, String nome, String descricao) {
}
```

```java
package com.estoque.mapper;

import com.estoque.dto.request.CategoriaRequest;
import com.estoque.dto.response.CategoriaResponse;
import com.estoque.entity.Categoria;
import org.mapstruct.BeanMapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Mapper;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface CategoriaMapper {

    Categoria toEntity(CategoriaRequest request);

    CategoriaResponse toResponse(Categoria categoria);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void atualizarEntidade(CategoriaRequest request, @MappingTarget Categoria categoria);
}
```

```java
package com.estoque.service;

import com.estoque.dto.request.CategoriaRequest;
import com.estoque.dto.response.CategoriaResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CategoriaService {
    Page<CategoriaResponse> listar(Pageable pageable);
    CategoriaResponse buscarPorId(Long id);
    CategoriaResponse criar(CategoriaRequest request);
    CategoriaResponse atualizar(Long id, CategoriaRequest request);
    void excluir(Long id);
}
```

```java
package com.estoque.service.impl;

import com.estoque.dto.request.CategoriaRequest;
import com.estoque.dto.response.CategoriaResponse;
import com.estoque.entity.Categoria;
import com.estoque.exception.RecursoNaoEncontradoException;
import com.estoque.exception.RegistroDuplicadoException;
import com.estoque.mapper.CategoriaMapper;
import com.estoque.repository.CategoriaRepository;
import com.estoque.service.CategoriaService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CategoriaServiceImpl implements CategoriaService {

    private final CategoriaRepository categoriaRepository;
    private final CategoriaMapper categoriaMapper;

    @Override
    @Transactional(readOnly = true)
    public Page<CategoriaResponse> listar(Pageable pageable) {
        return categoriaRepository.findAll(pageable).map(categoriaMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public CategoriaResponse buscarPorId(Long id) {
        return categoriaMapper.toResponse(buscarEntidadePorId(id));
    }

    @Override
    @Transactional
    public CategoriaResponse criar(CategoriaRequest request) {
        if (categoriaRepository.existsByNomeIgnoreCase(request.nome())) {
            throw new RegistroDuplicadoException("Já existe uma categoria com o nome '%s'".formatted(request.nome()));
        }
        Categoria categoria = categoriaMapper.toEntity(request);
        return categoriaMapper.toResponse(categoriaRepository.save(categoria));
    }

    @Override
    @Transactional
    public CategoriaResponse atualizar(Long id, CategoriaRequest request) {
        Categoria categoria = buscarEntidadePorId(id);
        categoriaMapper.atualizarEntidade(request, categoria);
        return categoriaMapper.toResponse(categoriaRepository.save(categoria));
    }

    @Override
    @Transactional
    public void excluir(Long id) {
        Categoria categoria = buscarEntidadePorId(id);
        categoriaRepository.delete(categoria);
    }

    private Categoria buscarEntidadePorId(Long id) {
        return categoriaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Categoria", id));
    }
}
```

```java
package com.estoque.controller;

import com.estoque.dto.request.CategoriaRequest;
import com.estoque.dto.response.CategoriaResponse;
import com.estoque.service.CategoriaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/categorias")
@RequiredArgsConstructor
public class CategoriaController {

    private final CategoriaService categoriaService;

    @GetMapping
    public ResponseEntity<Page<CategoriaResponse>> listar(Pageable pageable) {
        return ResponseEntity.ok(categoriaService.listar(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoriaResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(categoriaService.buscarPorId(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CategoriaResponse> criar(@Valid @RequestBody CategoriaRequest request) {
        CategoriaResponse resposta = categoriaService.criar(request);
        return ResponseEntity.created(URI.create("/api/v1/categorias/" + resposta.id())).body(resposta);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CategoriaResponse> atualizar(@PathVariable Long id, @Valid @RequestBody CategoriaRequest request) {
        return ResponseEntity.ok(categoriaService.atualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        categoriaService.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
```

- [ ] **Step 4: Rodar o teste unitário e confirmar sucesso**

Run: `mvn -q -Dtest=CategoriaServiceTest test`
Expected: PASS

- [ ] **Step 5: Escrever o teste de integração do controller**

```java
package com.estoque.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CategoriaControllerIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @Test
    @WithMockUser(roles = "ADMIN")
    void deveCriarCategoriaComSucesso() throws Exception {
        String corpo = objectMapper.writeValueAsString(
                new com.estoque.dto.request.CategoriaRequest("Eletrônicos", "Categoria de teste"));

        mockMvc.perform(post("/api/v1/categorias").contentType("application/json").content(corpo))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.nome").value("Eletrônicos"));
    }

    @Test
    @WithMockUser(roles = "OPERADOR")
    void deveRejeitarCriacaoParaOperadorCom403() throws Exception {
        String corpo = objectMapper.writeValueAsString(
                new com.estoque.dto.request.CategoriaRequest("Eletrônicos", "Categoria de teste"));

        mockMvc.perform(post("/api/v1/categorias").contentType("application/json").content(corpo))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser
    void deveRejeitarNomeEmBrancoCom400() throws Exception {
        String corpo = objectMapper.writeValueAsString(new com.estoque.dto.request.CategoriaRequest("", null));

        mockMvc.perform(post("/api/v1/categorias").contentType("application/json").content(corpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].campo").value("nome"));
    }

    @Test
    @WithMockUser
    void deveListarCategoriasPaginado() throws Exception {
        mockMvc.perform(get("/api/v1/categorias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }
}
```

Nota: o teste `deveRejeitarCriacaoParaOperadorCom403` requer o `role` no `@WithMockUser` mapeado como `OPERADOR` — o Spring Security prefixa `ROLE_` automaticamente, compatível com `hasRole('ADMIN')` no controller.

- [ ] **Step 6: Rodar o teste de integração e confirmar sucesso**

Run: `mvn -q -Dtest=CategoriaControllerIT test`
Expected: PASS

- [ ] **Step 7: Commit**

```bash
git add src/main/java/com/estoque/dto src/main/java/com/estoque/mapper/CategoriaMapper.java src/main/java/com/estoque/service/CategoriaService.java src/main/java/com/estoque/service/impl/CategoriaServiceImpl.java src/main/java/com/estoque/controller/CategoriaController.java src/test/java/com/estoque/service/CategoriaServiceTest.java src/test/java/com/estoque/controller/CategoriaControllerIT.java
git commit -m "feat: adicionar CRUD completo de Categoria"
```

---

## Task 13: Colecao — DTOs, mapper, service, controller, testes

**Files:**
- Create: `src/main/java/com/estoque/dto/request/ColecaoRequest.java`
- Create: `src/main/java/com/estoque/dto/response/ColecaoResponse.java`
- Create: `src/main/java/com/estoque/mapper/ColecaoMapper.java`
- Create: `src/main/java/com/estoque/service/ColecaoService.java`
- Create: `src/main/java/com/estoque/service/impl/ColecaoServiceImpl.java`
- Create: `src/main/java/com/estoque/controller/ColecaoController.java`
- Test: `src/test/java/com/estoque/service/ColecaoServiceTest.java`
- Test: `src/test/java/com/estoque/controller/ColecaoControllerIT.java`

**Interfaces:**
- Consumes: `Colecao`/`ColecaoRepository` (Task 3), `RecursoNaoEncontradoException`/`RegistroDuplicadoException` (Task 9).
- Produces: `ColecaoRequest(String nome, String descricao)`, `ColecaoResponse(Long id, String nome, String descricao)`, `ColecaoService{listar(Pageable), buscarPorId(Long), criar(ColecaoRequest), atualizar(Long, ColecaoRequest), excluir(Long)}`, rotas `GET/POST/PUT/DELETE /api/v1/colecoes`.

- [ ] **Step 1: Escrever o teste unitário do service (falha: classes não existem)**

```java
package com.estoque.service;

import com.estoque.dto.request.ColecaoRequest;
import com.estoque.dto.response.ColecaoResponse;
import com.estoque.entity.Colecao;
import com.estoque.exception.RecursoNaoEncontradoException;
import com.estoque.exception.RegistroDuplicadoException;
import com.estoque.mapper.ColecaoMapper;
import com.estoque.repository.ColecaoRepository;
import com.estoque.service.impl.ColecaoServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ColecaoServiceTest {

    @Mock private ColecaoRepository colecaoRepository;
    @Mock private ColecaoMapper colecaoMapper;
    @InjectMocks private ColecaoServiceImpl colecaoService;

    @Test
    void deveCriarColecaoQuandoNomeNaoExiste() {
        ColecaoRequest request = new ColecaoRequest("Verão 2026", "desc");
        Colecao entidade = Colecao.builder().nome("Verão 2026").descricao("desc").build();
        ColecaoResponse resposta = new ColecaoResponse(1L, "Verão 2026", "desc");

        when(colecaoRepository.existsByNomeIgnoreCase("Verão 2026")).thenReturn(false);
        when(colecaoMapper.toEntity(request)).thenReturn(entidade);
        when(colecaoRepository.save(entidade)).thenReturn(entidade);
        when(colecaoMapper.toResponse(entidade)).thenReturn(resposta);

        ColecaoResponse resultado = colecaoService.criar(request);

        assertThat(resultado.nome()).isEqualTo("Verão 2026");
        verify(colecaoRepository).save(entidade);
    }

    @Test
    void deveLancarExcecaoAoCriarColecaoComNomeDuplicado() {
        ColecaoRequest request = new ColecaoRequest("Verão 2026", "desc");
        when(colecaoRepository.existsByNomeIgnoreCase("Verão 2026")).thenReturn(true);

        assertThatThrownBy(() -> colecaoService.criar(request))
                .isInstanceOf(RegistroDuplicadoException.class);
    }

    @Test
    void deveLancarExcecaoAoBuscarColecaoInexistente() {
        when(colecaoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> colecaoService.buscarPorId(99L))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }
}
```

- [ ] **Step 2: Rodar e confirmar falha**

Run: `mvn -q -Dtest=ColecaoServiceTest test`
Expected: FAIL — classes não existem.

- [ ] **Step 3: Implementar DTOs, mapper, service e controller**

```java
package com.estoque.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ColecaoRequest(
        @NotBlank @Size(max = 80) String nome,
        @Size(max = 255) String descricao) {
}
```

```java
package com.estoque.dto.response;

public record ColecaoResponse(Long id, String nome, String descricao) {
}
```

```java
package com.estoque.mapper;

import com.estoque.dto.request.ColecaoRequest;
import com.estoque.dto.response.ColecaoResponse;
import com.estoque.entity.Colecao;
import org.mapstruct.BeanMapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Mapper;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface ColecaoMapper {

    Colecao toEntity(ColecaoRequest request);

    ColecaoResponse toResponse(Colecao colecao);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void atualizarEntidade(ColecaoRequest request, @MappingTarget Colecao colecao);
}
```

```java
package com.estoque.service;

import com.estoque.dto.request.ColecaoRequest;
import com.estoque.dto.response.ColecaoResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ColecaoService {
    Page<ColecaoResponse> listar(Pageable pageable);
    ColecaoResponse buscarPorId(Long id);
    ColecaoResponse criar(ColecaoRequest request);
    ColecaoResponse atualizar(Long id, ColecaoRequest request);
    void excluir(Long id);
}
```

```java
package com.estoque.service.impl;

import com.estoque.dto.request.ColecaoRequest;
import com.estoque.dto.response.ColecaoResponse;
import com.estoque.entity.Colecao;
import com.estoque.exception.RecursoNaoEncontradoException;
import com.estoque.exception.RegistroDuplicadoException;
import com.estoque.mapper.ColecaoMapper;
import com.estoque.repository.ColecaoRepository;
import com.estoque.service.ColecaoService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ColecaoServiceImpl implements ColecaoService {

    private final ColecaoRepository colecaoRepository;
    private final ColecaoMapper colecaoMapper;

    @Override
    @Transactional(readOnly = true)
    public Page<ColecaoResponse> listar(Pageable pageable) {
        return colecaoRepository.findAll(pageable).map(colecaoMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public ColecaoResponse buscarPorId(Long id) {
        return colecaoMapper.toResponse(buscarEntidadePorId(id));
    }

    @Override
    @Transactional
    public ColecaoResponse criar(ColecaoRequest request) {
        if (colecaoRepository.existsByNomeIgnoreCase(request.nome())) {
            throw new RegistroDuplicadoException("Já existe uma coleção com o nome '%s'".formatted(request.nome()));
        }
        Colecao colecao = colecaoMapper.toEntity(request);
        return colecaoMapper.toResponse(colecaoRepository.save(colecao));
    }

    @Override
    @Transactional
    public ColecaoResponse atualizar(Long id, ColecaoRequest request) {
        Colecao colecao = buscarEntidadePorId(id);
        colecaoMapper.atualizarEntidade(request, colecao);
        return colecaoMapper.toResponse(colecaoRepository.save(colecao));
    }

    @Override
    @Transactional
    public void excluir(Long id) {
        Colecao colecao = buscarEntidadePorId(id);
        colecaoRepository.delete(colecao);
    }

    private Colecao buscarEntidadePorId(Long id) {
        return colecaoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Coleção", id));
    }
}
```

```java
package com.estoque.controller;

import com.estoque.dto.request.ColecaoRequest;
import com.estoque.dto.response.ColecaoResponse;
import com.estoque.service.ColecaoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/colecoes")
@RequiredArgsConstructor
public class ColecaoController {

    private final ColecaoService colecaoService;

    @GetMapping
    public ResponseEntity<Page<ColecaoResponse>> listar(Pageable pageable) {
        return ResponseEntity.ok(colecaoService.listar(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ColecaoResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(colecaoService.buscarPorId(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ColecaoResponse> criar(@Valid @RequestBody ColecaoRequest request) {
        ColecaoResponse resposta = colecaoService.criar(request);
        return ResponseEntity.created(URI.create("/api/v1/colecoes/" + resposta.id())).body(resposta);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ColecaoResponse> atualizar(@PathVariable Long id, @Valid @RequestBody ColecaoRequest request) {
        return ResponseEntity.ok(colecaoService.atualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        colecaoService.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
```

- [ ] **Step 4: Rodar o teste unitário e confirmar sucesso**

Run: `mvn -q -Dtest=ColecaoServiceTest test`
Expected: PASS

- [ ] **Step 5: Escrever o teste de integração do controller**

```java
package com.estoque.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ColecaoControllerIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @Test
    @WithMockUser(roles = "ADMIN")
    void deveCriarColecaoComSucesso() throws Exception {
        String corpo = objectMapper.writeValueAsString(
                new com.estoque.dto.request.ColecaoRequest("Outono 2026", "Coleção de teste"));

        mockMvc.perform(post("/api/v1/colecoes").contentType("application/json").content(corpo))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.nome").value("Outono 2026"));
    }

    @Test
    @WithMockUser(roles = "OPERADOR")
    void deveRejeitarCriacaoParaOperadorCom403() throws Exception {
        String corpo = objectMapper.writeValueAsString(
                new com.estoque.dto.request.ColecaoRequest("Outono 2026", "Coleção de teste"));

        mockMvc.perform(post("/api/v1/colecoes").contentType("application/json").content(corpo))
                .andExpect(status().isForbidden());
    }
}
```

- [ ] **Step 6: Rodar o teste de integração e confirmar sucesso**

Run: `mvn -q -Dtest=ColecaoControllerIT test`
Expected: PASS

- [ ] **Step 7: Commit**

```bash
git add src/main/java/com/estoque/dto/request/ColecaoRequest.java src/main/java/com/estoque/dto/response/ColecaoResponse.java src/main/java/com/estoque/mapper/ColecaoMapper.java src/main/java/com/estoque/service/ColecaoService.java src/main/java/com/estoque/service/impl/ColecaoServiceImpl.java src/main/java/com/estoque/controller/ColecaoController.java src/test/java/com/estoque/service/ColecaoServiceTest.java src/test/java/com/estoque/controller/ColecaoControllerIT.java
git commit -m "feat: adicionar CRUD completo de Colecao"
```

---

## Task 14: Fornecedor — DTOs (com Endereco), mapper, service com soft delete, controller, testes

**Files:**
- Create: `src/main/java/com/estoque/dto/request/EnderecoRequest.java`
- Create: `src/main/java/com/estoque/dto/response/EnderecoResponse.java`
- Create: `src/main/java/com/estoque/dto/request/FornecedorRequest.java`
- Create: `src/main/java/com/estoque/dto/response/FornecedorResponse.java`
- Create: `src/main/java/com/estoque/mapper/FornecedorMapper.java`
- Create: `src/main/java/com/estoque/service/FornecedorService.java`
- Create: `src/main/java/com/estoque/service/impl/FornecedorServiceImpl.java`
- Create: `src/main/java/com/estoque/controller/FornecedorController.java`
- Test: `src/test/java/com/estoque/service/FornecedorServiceTest.java`
- Test: `src/test/java/com/estoque/controller/FornecedorControllerIT.java`

**Interfaces:**
- Consumes: `Fornecedor`/`FornecedorRepository` (Task 4), `Endereco` (Task 2).
- Produces: `EnderecoRequest`/`EnderecoResponse` (reutilizados pela Task 15 — Funcionario), `FornecedorRequest(String cnpj, String razaoSocial, String telefone, String email, EnderecoRequest endereco)`, `FornecedorResponse(Long id, String cnpj, String razaoSocial, String telefone, String email, EnderecoResponse endereco, boolean ativo)`, `FornecedorService{listar, buscarPorId, criar, atualizar, excluir}` (exclusão = soft delete), rotas `GET/POST/PUT/DELETE /api/v1/fornecedores`.

- [ ] **Step 1: Escrever o teste unitário do service (falha: classes não existem)**

```java
package com.estoque.service;

import com.estoque.dto.request.EnderecoRequest;
import com.estoque.dto.request.FornecedorRequest;
import com.estoque.dto.response.FornecedorResponse;
import com.estoque.entity.Endereco;
import com.estoque.entity.Fornecedor;
import com.estoque.exception.RecursoNaoEncontradoException;
import com.estoque.exception.RegistroDuplicadoException;
import com.estoque.mapper.FornecedorMapper;
import com.estoque.repository.FornecedorRepository;
import com.estoque.service.impl.FornecedorServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FornecedorServiceTest {

    @Mock private FornecedorRepository fornecedorRepository;
    @Mock private FornecedorMapper fornecedorMapper;
    @InjectMocks private FornecedorServiceImpl fornecedorService;

    private final EnderecoRequest enderecoRequest =
            new EnderecoRequest("Av. Brasil", "500", "Centro", "São Paulo", "SP", "01000-000");

    @Test
    void deveCriarFornecedorQuandoCnpjNaoExiste() {
        FornecedorRequest request = new FornecedorRequest("12345678000199", "Fornecedor LTDA",
                "11999999999", "contato@fornecedor.com", enderecoRequest);
        Fornecedor entidade = Fornecedor.builder()
                .cnpj("12345678000199").razaoSocial("Fornecedor LTDA")
                .telefone("11999999999").email("contato@fornecedor.com")
                .endereco(new Endereco("Av. Brasil", "500", "Centro", "São Paulo", "SP", "01000-000"))
                .build();

        when(fornecedorRepository.existsByCnpj("12345678000199")).thenReturn(false);
        when(fornecedorMapper.toEntity(request)).thenReturn(entidade);
        when(fornecedorRepository.save(entidade)).thenReturn(entidade);
        when(fornecedorMapper.toResponse(entidade)).thenReturn(
                new FornecedorResponse(1L, "12345678000199", "Fornecedor LTDA", "11999999999",
                        "contato@fornecedor.com", null, true));

        FornecedorResponse resultado = fornecedorService.criar(request);

        assertThat(resultado.razaoSocial()).isEqualTo("Fornecedor LTDA");
        verify(fornecedorRepository).save(entidade);
    }

    @Test
    void deveLancarExcecaoAoCriarFornecedorComCnpjDuplicado() {
        FornecedorRequest request = new FornecedorRequest("12345678000199", "Fornecedor LTDA",
                "11999999999", "contato@fornecedor.com", enderecoRequest);
        when(fornecedorRepository.existsByCnpj("12345678000199")).thenReturn(true);

        assertThatThrownBy(() -> fornecedorService.criar(request))
                .isInstanceOf(RegistroDuplicadoException.class);
    }

    @Test
    void deveLancarExcecaoAoBuscarFornecedorInativoOuInexistente() {
        when(fornecedorRepository.findByIdAndAtivoTrue(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> fornecedorService.buscarPorId(99L))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void deveDesativarFornecedorAoExcluir() {
        Fornecedor entidade = Fornecedor.builder()
                .cnpj("12345678000199").razaoSocial("Fornecedor LTDA")
                .telefone("11999999999").email("contato@fornecedor.com")
                .endereco(new Endereco("Av. Brasil", "500", "Centro", "São Paulo", "SP", "01000-000"))
                .build();
        when(fornecedorRepository.findByIdAndAtivoTrue(1L)).thenReturn(Optional.of(entidade));

        fornecedorService.excluir(1L);

        assertThat(entidade.isAtivo()).isFalse();
        verify(fornecedorRepository).save(entidade);
    }
}
```

- [ ] **Step 2: Rodar e confirmar falha**

Run: `mvn -q -Dtest=FornecedorServiceTest test`
Expected: FAIL — classes não existem.

- [ ] **Step 3: Implementar DTOs, mapper, service e controller**

```java
package com.estoque.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record EnderecoRequest(
        @NotBlank @Size(max = 150) String logradouro,
        @NotBlank @Size(max = 20) String numero,
        @NotBlank @Size(max = 100) String bairro,
        @NotBlank @Size(max = 100) String cidade,
        @NotBlank @Pattern(regexp = "[A-Z]{2}", message = "deve conter a sigla da UF, ex: PR") String estado,
        @NotBlank @Size(max = 9) String cep) {
}
```

```java
package com.estoque.dto.response;

public record EnderecoResponse(
        String logradouro, String numero, String bairro, String cidade, String estado, String cep) {
}
```

```java
package com.estoque.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record FornecedorRequest(
        @NotBlank @Pattern(regexp = "\\d{14}", message = "deve conter 14 dígitos numéricos") String cnpj,
        @NotBlank String razaoSocial,
        @NotBlank String telefone,
        @NotBlank @Email String email,
        @NotNull @Valid EnderecoRequest endereco) {
}
```

```java
package com.estoque.dto.response;

public record FornecedorResponse(
        Long id, String cnpj, String razaoSocial, String telefone, String email,
        EnderecoResponse endereco, boolean ativo) {
}
```

```java
package com.estoque.mapper;

import com.estoque.dto.request.FornecedorRequest;
import com.estoque.dto.response.FornecedorResponse;
import com.estoque.entity.Fornecedor;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface FornecedorMapper {

    Fornecedor toEntity(FornecedorRequest request);

    FornecedorResponse toResponse(Fornecedor fornecedor);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void atualizarEntidade(FornecedorRequest request, @MappingTarget Fornecedor fornecedor);
}
```

```java
package com.estoque.service;

import com.estoque.dto.request.FornecedorRequest;
import com.estoque.dto.response.FornecedorResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface FornecedorService {
    Page<FornecedorResponse> listar(Pageable pageable);
    FornecedorResponse buscarPorId(Long id);
    FornecedorResponse criar(FornecedorRequest request);
    FornecedorResponse atualizar(Long id, FornecedorRequest request);
    void excluir(Long id);
}
```

```java
package com.estoque.service.impl;

import com.estoque.dto.request.FornecedorRequest;
import com.estoque.dto.response.FornecedorResponse;
import com.estoque.entity.Fornecedor;
import com.estoque.exception.RecursoNaoEncontradoException;
import com.estoque.exception.RegistroDuplicadoException;
import com.estoque.mapper.FornecedorMapper;
import com.estoque.repository.FornecedorRepository;
import com.estoque.service.FornecedorService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FornecedorServiceImpl implements FornecedorService {

    private final FornecedorRepository fornecedorRepository;
    private final FornecedorMapper fornecedorMapper;

    @Override
    @Transactional(readOnly = true)
    public Page<FornecedorResponse> listar(Pageable pageable) {
        return fornecedorRepository.findByAtivoTrue(pageable).map(fornecedorMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public FornecedorResponse buscarPorId(Long id) {
        return fornecedorMapper.toResponse(buscarEntidadeAtivaPorId(id));
    }

    @Override
    @Transactional
    public FornecedorResponse criar(FornecedorRequest request) {
        if (fornecedorRepository.existsByCnpj(request.cnpj())) {
            throw new RegistroDuplicadoException("Já existe um fornecedor com o CNPJ informado");
        }
        Fornecedor fornecedor = fornecedorMapper.toEntity(request);
        return fornecedorMapper.toResponse(fornecedorRepository.save(fornecedor));
    }

    @Override
    @Transactional
    public FornecedorResponse atualizar(Long id, FornecedorRequest request) {
        Fornecedor fornecedor = buscarEntidadeAtivaPorId(id);
        fornecedorMapper.atualizarEntidade(request, fornecedor);
        return fornecedorMapper.toResponse(fornecedorRepository.save(fornecedor));
    }

    @Override
    @Transactional
    public void excluir(Long id) {
        Fornecedor fornecedor = buscarEntidadeAtivaPorId(id);
        fornecedor.desativar();
        fornecedorRepository.save(fornecedor);
    }

    private Fornecedor buscarEntidadeAtivaPorId(Long id) {
        return fornecedorRepository.findByIdAndAtivoTrue(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Fornecedor", id));
    }
}
```

```java
package com.estoque.controller;

import com.estoque.dto.request.FornecedorRequest;
import com.estoque.dto.response.FornecedorResponse;
import com.estoque.service.FornecedorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/fornecedores")
@RequiredArgsConstructor
public class FornecedorController {

    private final FornecedorService fornecedorService;

    @GetMapping
    public ResponseEntity<Page<FornecedorResponse>> listar(Pageable pageable) {
        return ResponseEntity.ok(fornecedorService.listar(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<FornecedorResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(fornecedorService.buscarPorId(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FornecedorResponse> criar(@Valid @RequestBody FornecedorRequest request) {
        FornecedorResponse resposta = fornecedorService.criar(request);
        return ResponseEntity.created(URI.create("/api/v1/fornecedores/" + resposta.id())).body(resposta);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FornecedorResponse> atualizar(@PathVariable Long id, @Valid @RequestBody FornecedorRequest request) {
        return ResponseEntity.ok(fornecedorService.atualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        fornecedorService.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
```

- [ ] **Step 4: Rodar o teste unitário e confirmar sucesso**

Run: `mvn -q -Dtest=FornecedorServiceTest test`
Expected: PASS

- [ ] **Step 5: Escrever o teste de integração do controller**

```java
package com.estoque.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FornecedorControllerIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @Test
    @WithMockUser(roles = "ADMIN")
    void deveCriarFornecedorComEnderecoValido() throws Exception {
        String corpo = objectMapper.writeValueAsString(new com.estoque.dto.request.FornecedorRequest(
                "98765432000188", "Nova Fornecedora LTDA", "41988887777", "contato@novafornecedora.com",
                new com.estoque.dto.request.EnderecoRequest("Rua X", "10", "Centro", "Curitiba", "PR", "80000-000")));

        mockMvc.perform(post("/api/v1/fornecedores").contentType("application/json").content(corpo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ativo").value(true))
                .andExpect(jsonPath("$.endereco.cidade").value("Curitiba"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deveRejeitarCnpjComFormatoInvalidoCom400() throws Exception {
        String corpo = objectMapper.writeValueAsString(new com.estoque.dto.request.FornecedorRequest(
                "cnpj-invalido", "Nova Fornecedora LTDA", "41988887777", "contato@novafornecedora.com",
                new com.estoque.dto.request.EnderecoRequest("Rua X", "10", "Centro", "Curitiba", "PR", "80000-000")));

        mockMvc.perform(post("/api/v1/fornecedores").contentType("application/json").content(corpo))
                .andExpect(status().isBadRequest());
    }
}
```

- [ ] **Step 6: Rodar o teste de integração e confirmar sucesso**

Run: `mvn -q -Dtest=FornecedorControllerIT test`
Expected: PASS

- [ ] **Step 7: Commit**

```bash
git add src/main/java/com/estoque/dto src/main/java/com/estoque/mapper/FornecedorMapper.java src/main/java/com/estoque/service/FornecedorService.java src/main/java/com/estoque/service/impl/FornecedorServiceImpl.java src/main/java/com/estoque/controller/FornecedorController.java src/test/java/com/estoque/service/FornecedorServiceTest.java src/test/java/com/estoque/controller/FornecedorControllerIT.java
git commit -m "feat: adicionar CRUD completo de Fornecedor com soft delete"
```

---

## Task 15: Funcionario — DTOs, mapper, service com hash de senha, controller (+ /me), testes

**Files:**
- Create: `src/main/java/com/estoque/dto/request/FuncionarioRequest.java`
- Create: `src/main/java/com/estoque/dto/response/FuncionarioResponse.java`
- Create: `src/main/java/com/estoque/mapper/FuncionarioMapper.java`
- Create: `src/main/java/com/estoque/service/FuncionarioService.java`
- Create: `src/main/java/com/estoque/service/impl/FuncionarioServiceImpl.java`
- Create: `src/main/java/com/estoque/controller/FuncionarioController.java`
- Test: `src/test/java/com/estoque/service/FuncionarioServiceTest.java`
- Test: `src/test/java/com/estoque/controller/FuncionarioControllerIT.java`

**Interfaces:**
- Consumes: `Funcionario`/`FuncionarioRepository` (Task 5), `EnderecoRequest`/`EnderecoResponse` (Task 14), `PasswordEncoder` (Task 11), `FuncionarioUserDetails` (Task 11).
- Produces: `FuncionarioRequest(nome, sobrenome, cpf, email, senha, matricula, dataAdmissao, setor, role, endereco)`, `FuncionarioResponse(id, nome, sobrenome, cpf, email, matricula, dataAdmissao, setor, role, endereco, ativo)` (nunca expõe a senha), `FuncionarioService{listar, buscarPorId, buscarPerfilPorEmail(String email), criar, atualizar, excluir}`, rotas `GET/POST/PUT/DELETE /api/v1/funcionarios` (todas `ADMIN`) e `GET /api/v1/funcionarios/me` (qualquer autenticado).

- [ ] **Step 1: Escrever o teste unitário do service (falha: classes não existem)**

```java
package com.estoque.service;

import com.estoque.dto.request.EnderecoRequest;
import com.estoque.dto.request.FuncionarioRequest;
import com.estoque.dto.response.FuncionarioResponse;
import com.estoque.entity.Endereco;
import com.estoque.entity.Funcionario;
import com.estoque.enums.Role;
import com.estoque.exception.RegistroDuplicadoException;
import com.estoque.mapper.FuncionarioMapper;
import com.estoque.repository.FuncionarioRepository;
import com.estoque.service.impl.FuncionarioServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FuncionarioServiceTest {

    @Mock private FuncionarioRepository funcionarioRepository;
    @Mock private FuncionarioMapper funcionarioMapper;
    @Mock private PasswordEncoder passwordEncoder;
    @InjectMocks private FuncionarioServiceImpl funcionarioService;

    private final EnderecoRequest enderecoRequest =
            new EnderecoRequest("Rua B", "10", "Centro", "Curitiba", "PR", "80000-000");

    @Test
    void deveCriarFuncionarioComSenhaCriptografada() {
        FuncionarioRequest request = new FuncionarioRequest("Ana", "Silva", "11122233344",
                "ana@estoque.com", "senha123", "F001", LocalDate.now(), "Almoxarifado", Role.OPERADOR, enderecoRequest);
        Funcionario entidade = Funcionario.builder()
                .nome("Ana").sobrenome("Silva").cpf("11122233344").email("ana@estoque.com")
                .matricula("F001").dataAdmissao(LocalDate.now()).setor("Almoxarifado").role(Role.OPERADOR)
                .senha(null)
                .endereco(new Endereco("Rua B", "10", "Centro", "Curitiba", "PR", "80000-000"))
                .build();

        when(funcionarioRepository.existsByCpf("11122233344")).thenReturn(false);
        when(funcionarioRepository.existsByEmail("ana@estoque.com")).thenReturn(false);
        when(funcionarioMapper.toEntity(request)).thenReturn(entidade);
        when(passwordEncoder.encode("senha123")).thenReturn("hash-seguro");
        when(funcionarioRepository.save(entidade)).thenReturn(entidade);
        when(funcionarioMapper.toResponse(entidade)).thenReturn(new FuncionarioResponse(
                1L, "Ana", "Silva", "11122233344", "ana@estoque.com", "F001", LocalDate.now(),
                "Almoxarifado", Role.OPERADOR, null, true));

        FuncionarioResponse resultado = funcionarioService.criar(request);

        assertThat(resultado.nome()).isEqualTo("Ana");
        assertThat(entidade.getSenha()).isEqualTo("hash-seguro");
        verify(funcionarioRepository).save(entidade);
    }

    @Test
    void deveLancarExcecaoAoCriarFuncionarioComCpfDuplicado() {
        FuncionarioRequest request = new FuncionarioRequest("Ana", "Silva", "11122233344",
                "ana@estoque.com", "senha123", "F001", LocalDate.now(), "Almoxarifado", Role.OPERADOR, enderecoRequest);
        when(funcionarioRepository.existsByCpf("11122233344")).thenReturn(true);

        assertThatThrownBy(() -> funcionarioService.criar(request))
                .isInstanceOf(RegistroDuplicadoException.class);
    }
}
```

- [ ] **Step 2: Rodar e confirmar falha**

Run: `mvn -q -Dtest=FuncionarioServiceTest test`
Expected: FAIL — classes não existem.

- [ ] **Step 3: Implementar DTOs, mapper, service e controller**

```java
package com.estoque.dto.request;

import com.estoque.enums.Role;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record FuncionarioRequest(
        @NotBlank String nome,
        @NotBlank String sobrenome,
        @NotBlank @Pattern(regexp = "\\d{11}", message = "deve conter 11 dígitos numéricos") String cpf,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 6, message = "deve ter ao menos 6 caracteres") String senha,
        @NotBlank String matricula,
        @NotNull LocalDate dataAdmissao,
        @NotBlank String setor,
        @NotNull Role role,
        @NotNull @Valid EnderecoRequest endereco) {
}
```

```java
package com.estoque.dto.response;

import com.estoque.enums.Role;

import java.time.LocalDate;

public record FuncionarioResponse(
        Long id, String nome, String sobrenome, String cpf, String email, String matricula,
        LocalDate dataAdmissao, String setor, Role role, EnderecoResponse endereco, boolean ativo) {
}
```

```java
package com.estoque.mapper;

import com.estoque.dto.request.FuncionarioRequest;
import com.estoque.dto.response.FuncionarioResponse;
import com.estoque.entity.Funcionario;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface FuncionarioMapper {

    @Mapping(target = "senha", ignore = true)
    Funcionario toEntity(FuncionarioRequest request);

    FuncionarioResponse toResponse(Funcionario funcionario);

    @Mapping(target = "senha", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void atualizarEntidade(FuncionarioRequest request, @MappingTarget Funcionario funcionario);
}
```

```java
package com.estoque.service;

import com.estoque.dto.request.FuncionarioRequest;
import com.estoque.dto.response.FuncionarioResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface FuncionarioService {
    Page<FuncionarioResponse> listar(Pageable pageable);
    FuncionarioResponse buscarPorId(Long id);
    FuncionarioResponse buscarPerfilPorEmail(String email);
    FuncionarioResponse criar(FuncionarioRequest request);
    FuncionarioResponse atualizar(Long id, FuncionarioRequest request);
    void excluir(Long id);
}
```

```java
package com.estoque.service.impl;

import com.estoque.dto.request.FuncionarioRequest;
import com.estoque.dto.response.FuncionarioResponse;
import com.estoque.entity.Funcionario;
import com.estoque.exception.RecursoNaoEncontradoException;
import com.estoque.exception.RegistroDuplicadoException;
import com.estoque.mapper.FuncionarioMapper;
import com.estoque.repository.FuncionarioRepository;
import com.estoque.service.FuncionarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FuncionarioServiceImpl implements FuncionarioService {

    private final FuncionarioRepository funcionarioRepository;
    private final FuncionarioMapper funcionarioMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public Page<FuncionarioResponse> listar(Pageable pageable) {
        return funcionarioRepository.findByAtivoTrue(pageable).map(funcionarioMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public FuncionarioResponse buscarPorId(Long id) {
        return funcionarioMapper.toResponse(buscarEntidadePorId(id));
    }

    @Override
    @Transactional(readOnly = true)
    public FuncionarioResponse buscarPerfilPorEmail(String email) {
        Funcionario funcionario = funcionarioRepository.findByEmail(email)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Funcionário", email));
        return funcionarioMapper.toResponse(funcionario);
    }

    @Override
    @Transactional
    public FuncionarioResponse criar(FuncionarioRequest request) {
        if (funcionarioRepository.existsByCpf(request.cpf())) {
            throw new RegistroDuplicadoException("Já existe um funcionário com o CPF informado");
        }
        if (funcionarioRepository.existsByEmail(request.email())) {
            throw new RegistroDuplicadoException("Já existe um funcionário com o e-mail informado");
        }
        Funcionario funcionario = funcionarioMapper.toEntity(request);
        funcionario.trocarSenha(passwordEncoder.encode(request.senha()));
        return funcionarioMapper.toResponse(funcionarioRepository.save(funcionario));
    }

    @Override
    @Transactional
    public FuncionarioResponse atualizar(Long id, FuncionarioRequest request) {
        Funcionario funcionario = buscarEntidadePorId(id);
        funcionarioMapper.atualizarEntidade(request, funcionario);
        funcionario.trocarSenha(passwordEncoder.encode(request.senha()));
        return funcionarioMapper.toResponse(funcionarioRepository.save(funcionario));
    }

    @Override
    @Transactional
    public void excluir(Long id) {
        Funcionario funcionario = buscarEntidadePorId(id);
        funcionario.desativar();
        funcionarioRepository.save(funcionario);
    }

    private Funcionario buscarEntidadePorId(Long id) {
        return funcionarioRepository.findById(id)
                .filter(Funcionario::isAtivo)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Funcionário", id));
    }
}
```

```java
package com.estoque.controller;

import com.estoque.dto.request.FuncionarioRequest;
import com.estoque.dto.response.FuncionarioResponse;
import com.estoque.security.FuncionarioUserDetails;
import com.estoque.service.FuncionarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/funcionarios")
@RequiredArgsConstructor
public class FuncionarioController {

    private final FuncionarioService funcionarioService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<FuncionarioResponse>> listar(Pageable pageable) {
        return ResponseEntity.ok(funcionarioService.listar(pageable));
    }

    @GetMapping("/me")
    public ResponseEntity<FuncionarioResponse> meuPerfil(@AuthenticationPrincipal FuncionarioUserDetails principal) {
        return ResponseEntity.ok(funcionarioService.buscarPerfilPorEmail(principal.getUsername()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FuncionarioResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(funcionarioService.buscarPorId(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FuncionarioResponse> criar(@Valid @RequestBody FuncionarioRequest request) {
        FuncionarioResponse resposta = funcionarioService.criar(request);
        return ResponseEntity.created(URI.create("/api/v1/funcionarios/" + resposta.id())).body(resposta);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FuncionarioResponse> atualizar(@PathVariable Long id, @Valid @RequestBody FuncionarioRequest request) {
        return ResponseEntity.ok(funcionarioService.atualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        funcionarioService.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
```

- [ ] **Step 4: Rodar o teste unitário e confirmar sucesso**

Run: `mvn -q -Dtest=FuncionarioServiceTest test`
Expected: PASS

- [ ] **Step 5: Escrever o teste de integração do controller**

```java
package com.estoque.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FuncionarioControllerIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @Test
    @WithUserDetails(value = "admin@estoque.com", userDetailsServiceBeanName = "userDetailsServiceImpl")
    void deveRetornarPerfilDoUsuarioAutenticado() throws Exception {
        mockMvc.perform(get("/api/v1/funcionarios/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("admin@estoque.com"));
    }
}
```

Nota: `@WithUserDetails` carrega o funcionário real do seed (Task 8) através do `UserDetailsServiceImpl` (Task 11) — por isso o bean precisa se chamar `userDetailsServiceImpl` (nome padrão gerado pelo Spring a partir do nome da classe); confirme esse nome ao rodar, ou ajuste a anotação `@Service("userDetailsServiceImpl")` explicitamente se o autodetect gerar outro nome.

- [ ] **Step 6: Rodar o teste de integração e confirmar sucesso**

Run: `mvn -q -Dtest=FuncionarioControllerIT test`
Expected: PASS

- [ ] **Step 7: Commit**

```bash
git add src/main/java/com/estoque/dto src/main/java/com/estoque/mapper/FuncionarioMapper.java src/main/java/com/estoque/service/FuncionarioService.java src/main/java/com/estoque/service/impl/FuncionarioServiceImpl.java src/main/java/com/estoque/controller/FuncionarioController.java src/test/java/com/estoque/service/FuncionarioServiceTest.java src/test/java/com/estoque/controller/FuncionarioControllerIT.java
git commit -m "feat: adicionar CRUD completo de Funcionario com senha criptografada e perfil /me"
```

---

## Task 16: Produto — DTOs, mapper, service (resolvendo relacionamentos), controller (+ estoque-baixo), testes

**Files:**
- Create: `src/main/java/com/estoque/dto/request/ProdutoRequest.java`
- Create: `src/main/java/com/estoque/dto/response/ProdutoResponse.java`
- Create: `src/main/java/com/estoque/mapper/ProdutoMapper.java`
- Create: `src/main/java/com/estoque/service/ProdutoService.java`
- Create: `src/main/java/com/estoque/service/impl/ProdutoServiceImpl.java`
- Create: `src/main/java/com/estoque/controller/ProdutoController.java`
- Test: `src/test/java/com/estoque/service/ProdutoServiceTest.java`
- Test: `src/test/java/com/estoque/controller/ProdutoControllerIT.java`

**Interfaces:**
- Consumes: `Produto`/`ProdutoRepository` (Task 6), `CategoriaRepository` (Task 3), `ColecaoRepository` (Task 3), `FornecedorRepository` (Task 4).
- Produces: `ProdutoRequest(codigo, nome, descricao, categoriaId, colecaoId, fornecedorId, precoVenda, estoqueMinimo)` (`colecaoId`/`fornecedorId` opcionais), `ProdutoResponse(id, codigo, nome, descricao, categoriaNome, colecaoNome, fornecedorRazaoSocial, precoVenda, custoMedio, quantidadeEstoque, estoqueMinimo, ativo)`, `ProdutoService{listar, buscarPorId, listarComEstoqueBaixo, criar, atualizar, excluir}`, rotas `GET/POST/PUT/DELETE /api/v1/produtos` e `GET /api/v1/produtos/estoque-baixo`.

- [ ] **Step 1: Escrever o teste unitário do service (falha: classes não existem)**

```java
package com.estoque.service;

import com.estoque.dto.request.ProdutoRequest;
import com.estoque.dto.response.ProdutoResponse;
import com.estoque.entity.Categoria;
import com.estoque.entity.Produto;
import com.estoque.exception.RecursoNaoEncontradoException;
import com.estoque.exception.RegistroDuplicadoException;
import com.estoque.mapper.ProdutoMapper;
import com.estoque.repository.CategoriaRepository;
import com.estoque.repository.ColecaoRepository;
import com.estoque.repository.FornecedorRepository;
import com.estoque.repository.ProdutoRepository;
import com.estoque.service.impl.ProdutoServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProdutoServiceTest {

    @Mock private ProdutoRepository produtoRepository;
    @Mock private CategoriaRepository categoriaRepository;
    @Mock private ColecaoRepository colecaoRepository;
    @Mock private FornecedorRepository fornecedorRepository;
    @Mock private ProdutoMapper produtoMapper;
    @InjectMocks private ProdutoServiceImpl produtoService;

    @Test
    void deveCriarProdutoQuandoCodigoNaoExisteECategoriaValida() {
        ProdutoRequest request = new ProdutoRequest("SKU-1", "Camiseta", "desc", 1L, null, null,
                new BigDecimal("49.90"), 10);
        Categoria categoria = Categoria.builder().nome("Camisetas").build();
        Produto produtoSalvo = Produto.builder().codigo("SKU-1").nome("Camiseta").categoria(categoria)
                .precoVenda(new BigDecimal("49.90")).estoqueMinimo(10).build();

        when(produtoRepository.existsByCodigo("SKU-1")).thenReturn(false);
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria));
        when(produtoRepository.save(org.mockito.ArgumentMatchers.any(Produto.class))).thenReturn(produtoSalvo);
        when(produtoMapper.toResponse(produtoSalvo)).thenReturn(new ProdutoResponse(
                1L, "SKU-1", "Camiseta", "desc", "Camisetas", null, null,
                new BigDecimal("49.90"), BigDecimal.ZERO, 0, 10, true));

        ProdutoResponse resultado = produtoService.criar(request);

        assertThat(resultado.codigo()).isEqualTo("SKU-1");
        verify(produtoRepository).save(org.mockito.ArgumentMatchers.any(Produto.class));
    }

    @Test
    void deveLancarExcecaoAoCriarProdutoComCodigoDuplicado() {
        ProdutoRequest request = new ProdutoRequest("SKU-1", "Camiseta", "desc", 1L, null, null,
                new BigDecimal("49.90"), 10);
        when(produtoRepository.existsByCodigo("SKU-1")).thenReturn(true);

        assertThatThrownBy(() -> produtoService.criar(request))
                .isInstanceOf(RegistroDuplicadoException.class);
    }

    @Test
    void deveLancarExcecaoQuandoCategoriaInformadaNaoExiste() {
        ProdutoRequest request = new ProdutoRequest("SKU-1", "Camiseta", "desc", 99L, null, null,
                new BigDecimal("49.90"), 10);
        when(produtoRepository.existsByCodigo("SKU-1")).thenReturn(false);
        when(categoriaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> produtoService.criar(request))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void deveListarProdutosComEstoqueBaixo() {
        Produto produto = Produto.builder().codigo("SKU-2").nome("Bolsa")
                .categoria(Categoria.builder().nome("Bolsas").build())
                .precoVenda(BigDecimal.TEN).estoqueMinimo(5).build();
        when(produtoRepository.buscarComEstoqueAbaixoDoMinimo()).thenReturn(List.of(produto));
        when(produtoMapper.toResponse(produto)).thenReturn(new ProdutoResponse(
                2L, "SKU-2", "Bolsa", null, "Bolsas", null, null, BigDecimal.TEN, BigDecimal.ZERO, 0, 5, true));

        List<ProdutoResponse> resultado = produtoService.listarComEstoqueBaixo();

        assertThat(resultado).hasSize(1);
    }
}
```

- [ ] **Step 2: Rodar e confirmar falha**

Run: `mvn -q -Dtest=ProdutoServiceTest test`
Expected: FAIL — classes não existem.

- [ ] **Step 3: Implementar DTOs, mapper, service e controller**

```java
package com.estoque.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ProdutoRequest(
        @NotBlank @Size(max = 40) String codigo,
        @NotBlank @Size(max = 150) String nome,
        @Size(max = 500) String descricao,
        @NotNull Long categoriaId,
        Long colecaoId,
        Long fornecedorId,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal precoVenda,
        @PositiveOrZero int estoqueMinimo) {
}
```

```java
package com.estoque.dto.response;

import java.math.BigDecimal;

public record ProdutoResponse(
        Long id, String codigo, String nome, String descricao, String categoriaNome, String colecaoNome,
        String fornecedorRazaoSocial, BigDecimal precoVenda, BigDecimal custoMedio, int quantidadeEstoque,
        int estoqueMinimo, boolean ativo) {
}
```

```java
package com.estoque.mapper;

import com.estoque.dto.response.ProdutoResponse;
import com.estoque.entity.Produto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProdutoMapper {

    @Mapping(source = "categoria.nome", target = "categoriaNome")
    @Mapping(source = "colecao.nome", target = "colecaoNome")
    @Mapping(source = "fornecedor.razaoSocial", target = "fornecedorRazaoSocial")
    ProdutoResponse toResponse(Produto produto);
}
```

Nota: a resolução de `categoriaId`/`colecaoId`/`fornecedorId` para as entidades relacionadas fica no service (não no mapper), pois exige consulta a repositórios — responsabilidade que não pertence a um mapeador de DTO.

```java
package com.estoque.service;

import com.estoque.dto.request.ProdutoRequest;
import com.estoque.dto.response.ProdutoResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ProdutoService {
    Page<ProdutoResponse> listar(Pageable pageable);
    ProdutoResponse buscarPorId(Long id);
    List<ProdutoResponse> listarComEstoqueBaixo();
    ProdutoResponse criar(ProdutoRequest request);
    ProdutoResponse atualizar(Long id, ProdutoRequest request);
    void excluir(Long id);
}
```

```java
package com.estoque.service.impl;

import com.estoque.dto.request.ProdutoRequest;
import com.estoque.dto.response.ProdutoResponse;
import com.estoque.entity.Categoria;
import com.estoque.entity.Colecao;
import com.estoque.entity.Fornecedor;
import com.estoque.entity.Produto;
import com.estoque.exception.RecursoNaoEncontradoException;
import com.estoque.exception.RegistroDuplicadoException;
import com.estoque.mapper.ProdutoMapper;
import com.estoque.repository.CategoriaRepository;
import com.estoque.repository.ColecaoRepository;
import com.estoque.repository.FornecedorRepository;
import com.estoque.repository.ProdutoRepository;
import com.estoque.service.ProdutoService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProdutoServiceImpl implements ProdutoService {

    private final ProdutoRepository produtoRepository;
    private final CategoriaRepository categoriaRepository;
    private final ColecaoRepository colecaoRepository;
    private final FornecedorRepository fornecedorRepository;
    private final ProdutoMapper produtoMapper;

    @Override
    @Transactional(readOnly = true)
    public Page<ProdutoResponse> listar(Pageable pageable) {
        return produtoRepository.findByAtivoTrue(pageable).map(produtoMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public ProdutoResponse buscarPorId(Long id) {
        return produtoMapper.toResponse(buscarEntidadeAtivaPorId(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProdutoResponse> listarComEstoqueBaixo() {
        return produtoRepository.buscarComEstoqueAbaixoDoMinimo().stream()
                .map(produtoMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public ProdutoResponse criar(ProdutoRequest request) {
        if (produtoRepository.existsByCodigo(request.codigo())) {
            throw new RegistroDuplicadoException("Já existe um produto com o código '%s'".formatted(request.codigo()));
        }
        Categoria categoria = buscarCategoria(request.categoriaId());
        Colecao colecao = request.colecaoId() != null ? buscarColecao(request.colecaoId()) : null;
        Fornecedor fornecedor = request.fornecedorId() != null ? buscarFornecedor(request.fornecedorId()) : null;

        Produto produto = Produto.builder()
                .codigo(request.codigo())
                .nome(request.nome())
                .descricao(request.descricao())
                .categoria(categoria)
                .colecao(colecao)
                .fornecedor(fornecedor)
                .precoVenda(request.precoVenda())
                .estoqueMinimo(request.estoqueMinimo())
                .build();

        return produtoMapper.toResponse(produtoRepository.save(produto));
    }

    @Override
    @Transactional
    public ProdutoResponse atualizar(Long id, ProdutoRequest request) {
        Produto produto = buscarEntidadeAtivaPorId(id);
        produto.setNome(request.nome());
        produto.setDescricao(request.descricao());
        produto.setCategoria(buscarCategoria(request.categoriaId()));
        produto.setColecao(request.colecaoId() != null ? buscarColecao(request.colecaoId()) : null);
        produto.setFornecedor(request.fornecedorId() != null ? buscarFornecedor(request.fornecedorId()) : null);
        produto.setPrecoVenda(request.precoVenda());
        produto.setEstoqueMinimo(request.estoqueMinimo());

        return produtoMapper.toResponse(produtoRepository.save(produto));
    }

    @Override
    @Transactional
    public void excluir(Long id) {
        Produto produto = buscarEntidadeAtivaPorId(id);
        produto.desativar();
        produtoRepository.save(produto);
    }

    private Produto buscarEntidadeAtivaPorId(Long id) {
        return produtoRepository.findByIdAndAtivoTrue(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Produto", id));
    }

    private Categoria buscarCategoria(Long id) {
        return categoriaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Categoria", id));
    }

    private Colecao buscarColecao(Long id) {
        return colecaoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Coleção", id));
    }

    private Fornecedor buscarFornecedor(Long id) {
        return fornecedorRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Fornecedor", id));
    }
}
```

```java
package com.estoque.controller;

import com.estoque.dto.request.ProdutoRequest;
import com.estoque.dto.response.ProdutoResponse;
import com.estoque.service.ProdutoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/produtos")
@RequiredArgsConstructor
public class ProdutoController {

    private final ProdutoService produtoService;

    @GetMapping
    public ResponseEntity<Page<ProdutoResponse>> listar(Pageable pageable) {
        return ResponseEntity.ok(produtoService.listar(pageable));
    }

    @GetMapping("/estoque-baixo")
    public ResponseEntity<List<ProdutoResponse>> listarComEstoqueBaixo() {
        return ResponseEntity.ok(produtoService.listarComEstoqueBaixo());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProdutoResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(produtoService.buscarPorId(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProdutoResponse> criar(@Valid @RequestBody ProdutoRequest request) {
        ProdutoResponse resposta = produtoService.criar(request);
        return ResponseEntity.created(URI.create("/api/v1/produtos/" + resposta.id())).body(resposta);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProdutoResponse> atualizar(@PathVariable Long id, @Valid @RequestBody ProdutoRequest request) {
        return ResponseEntity.ok(produtoService.atualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        produtoService.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
```

- [ ] **Step 4: Rodar o teste unitário e confirmar sucesso**

Run: `mvn -q -Dtest=ProdutoServiceTest test`
Expected: PASS

- [ ] **Step 5: Escrever o teste de integração do controller**

```java
package com.estoque.controller;

import com.estoque.repository.CategoriaRepository;
import com.estoque.entity.Categoria;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ProdutoControllerIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private CategoriaRepository categoriaRepository;

    @Test
    @WithMockUser(roles = "ADMIN")
    void deveCriarProdutoComCategoriaValida() throws Exception {
        Categoria categoria = categoriaRepository.save(Categoria.builder().nome("Categoria Teste IT").build());

        String corpo = objectMapper.writeValueAsString(new com.estoque.dto.request.ProdutoRequest(
                "SKU-IT-1", "Produto Teste", "desc", categoria.getId(), null, null,
                new BigDecimal("29.90"), 5));

        mockMvc.perform(post("/api/v1/produtos").contentType("application/json").content(corpo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.categoriaNome").value("Categoria Teste IT"))
                .andExpect(jsonPath("$.quantidadeEstoque").value(0));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deveRejeitarProdutoComCategoriaInexistenteCom404() throws Exception {
        String corpo = objectMapper.writeValueAsString(new com.estoque.dto.request.ProdutoRequest(
                "SKU-IT-2", "Produto Teste", "desc", 99999L, null, null,
                new BigDecimal("29.90"), 5));

        mockMvc.perform(post("/api/v1/produtos").contentType("application/json").content(corpo))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void deveListarProdutosComEstoqueBaixo() throws Exception {
        mockMvc.perform(get("/api/v1/produtos/estoque-baixo"))
                .andExpect(status().isOk());
    }
}
```

- [ ] **Step 6: Rodar o teste de integração e confirmar sucesso**

Run: `mvn -q -Dtest=ProdutoControllerIT test`
Expected: PASS

- [ ] **Step 7: Commit**

```bash
git add src/main/java/com/estoque/dto src/main/java/com/estoque/mapper/ProdutoMapper.java src/main/java/com/estoque/service/ProdutoService.java src/main/java/com/estoque/service/impl/ProdutoServiceImpl.java src/main/java/com/estoque/controller/ProdutoController.java src/test/java/com/estoque/service/ProdutoServiceTest.java src/test/java/com/estoque/controller/ProdutoControllerIT.java
git commit -m "feat: adicionar CRUD completo de Produto com estoque baixo e soft delete"
```

---

## Task 17: Entrada — registro de entrada (atualiza estoque e custo médio)

**Files:**
- Create: `src/main/java/com/estoque/dto/request/EntradaRequest.java`
- Create: `src/main/java/com/estoque/dto/response/MovimentacaoResponse.java`
- Create: `src/main/java/com/estoque/service/EntradaService.java`
- Create: `src/main/java/com/estoque/service/impl/EntradaServiceImpl.java`
- Create: `src/main/java/com/estoque/controller/EntradaController.java`
- Test: `src/test/java/com/estoque/service/EntradaServiceTest.java`
- Test: `src/test/java/com/estoque/controller/EntradaControllerIT.java`

**Interfaces:**
- Consumes: `Produto` (Task 6, método `registrarEntrada`), `Entrada`/`EntradaRepository` (Task 7), `FuncionarioRepository` (Task 5), `FornecedorRepository` (Task 4).
- Produces: `EntradaRequest(Long produtoId, Long fornecedorId, int quantidade, BigDecimal custoUnitario, String observacao)` (`fornecedorId` opcional), `MovimentacaoResponse(Long id, String tipo, String produtoCodigo, String produtoNome, String funcionarioNome, int quantidade, LocalDateTime dataMovimentacao, String observacao, BigDecimal custoUnitario, String motivo)` (reutilizado também pela Task 18/19 — `custoUnitario` preenchido só em entradas, `motivo` só em saídas), `EntradaService{listar(Long produtoId, Pageable), registrar(EntradaRequest, String emailFuncionario)}`, rotas `GET/POST /api/v1/entradas`.

- [ ] **Step 1: Escrever o teste unitário do service (falha: classes não existem)**

```java
package com.estoque.service;

import com.estoque.dto.request.EntradaRequest;
import com.estoque.entity.Categoria;
import com.estoque.entity.Endereco;
import com.estoque.entity.Funcionario;
import com.estoque.entity.Produto;
import com.estoque.enums.Role;
import com.estoque.exception.RecursoNaoEncontradoException;
import com.estoque.repository.EntradaRepository;
import com.estoque.repository.FornecedorRepository;
import com.estoque.repository.FuncionarioRepository;
import com.estoque.repository.ProdutoRepository;
import com.estoque.service.impl.EntradaServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EntradaServiceTest {

    @Mock private ProdutoRepository produtoRepository;
    @Mock private FuncionarioRepository funcionarioRepository;
    @Mock private FornecedorRepository fornecedorRepository;
    @Mock private EntradaRepository entradaRepository;
    @InjectMocks private EntradaServiceImpl entradaService;

    private Produto novoProduto(int estoqueInicial) {
        Produto produto = Produto.builder().codigo("SKU-1").nome("Camiseta")
                .categoria(Categoria.builder().nome("Roupas").build())
                .precoVenda(new BigDecimal("49.90")).estoqueMinimo(5).build();
        if (estoqueInicial > 0) {
            produto.registrarEntrada(estoqueInicial, new BigDecimal("10.00"));
        }
        return produto;
    }

    private Funcionario novoFuncionario() {
        return Funcionario.builder().nome("Ana").sobrenome("Silva").cpf("11122233344")
                .email("ana@estoque.com").senha("hash").matricula("F001").dataAdmissao(LocalDate.now())
                .setor("Loja").role(Role.OPERADOR)
                .endereco(new Endereco("Rua B", "10", "Centro", "Curitiba", "PR", "80000-000"))
                .build();
    }

    @Test
    void deveRegistrarEntradaEAumentarEstoqueDoProduto() {
        Produto produto = novoProduto(0);
        Funcionario funcionario = novoFuncionario();
        EntradaRequest request = new EntradaRequest(1L, null, 10, new BigDecimal("20.00"), "compra inicial");

        when(produtoRepository.findByIdAndAtivoTrue(1L)).thenReturn(Optional.of(produto));
        when(funcionarioRepository.findByEmail("ana@estoque.com")).thenReturn(Optional.of(funcionario));
        when(produtoRepository.save(produto)).thenReturn(produto);
        when(entradaRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        entradaService.registrar(request, "ana@estoque.com");

        assertThat(produto.getQuantidadeEstoque()).isEqualTo(10);
        assertThat(produto.getCustoMedio()).isEqualByComparingTo("20.00");
        verify(produtoRepository).save(produto);
    }

    @Test
    void deveLancarExcecaoQuandoProdutoNaoExisteOuInativo() {
        when(produtoRepository.findByIdAndAtivoTrue(1L)).thenReturn(Optional.empty());
        EntradaRequest request = new EntradaRequest(1L, null, 10, new BigDecimal("20.00"), null);

        assertThatThrownBy(() -> entradaService.registrar(request, "ana@estoque.com"))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }
}
```

- [ ] **Step 2: Rodar e confirmar falha**

Run: `mvn -q -Dtest=EntradaServiceTest test`
Expected: FAIL — classes não existem.

- [ ] **Step 3: Implementar DTOs, service e controller**

```java
package com.estoque.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record EntradaRequest(
        @NotNull Long produtoId,
        Long fornecedorId,
        @Positive int quantidade,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal custoUnitario,
        @Size(max = 255) String observacao) {
}
```

```java
package com.estoque.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MovimentacaoResponse(
        Long id, String tipo, String produtoCodigo, String produtoNome, String funcionarioNome,
        int quantidade, LocalDateTime dataMovimentacao, String observacao,
        BigDecimal custoUnitario, String motivo) {
}
```

```java
package com.estoque.service;

import com.estoque.dto.request.EntradaRequest;
import com.estoque.dto.response.MovimentacaoResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface EntradaService {
    Page<MovimentacaoResponse> listar(Long produtoId, Pageable pageable);
    MovimentacaoResponse registrar(EntradaRequest request, String emailFuncionarioAutenticado);
}
```

```java
package com.estoque.service.impl;

import com.estoque.dto.request.EntradaRequest;
import com.estoque.dto.response.MovimentacaoResponse;
import com.estoque.entity.Entrada;
import com.estoque.entity.Fornecedor;
import com.estoque.entity.Funcionario;
import com.estoque.entity.Produto;
import com.estoque.exception.RecursoNaoEncontradoException;
import com.estoque.repository.EntradaRepository;
import com.estoque.repository.FornecedorRepository;
import com.estoque.repository.FuncionarioRepository;
import com.estoque.repository.ProdutoRepository;
import com.estoque.service.EntradaService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EntradaServiceImpl implements EntradaService {

    private final ProdutoRepository produtoRepository;
    private final FuncionarioRepository funcionarioRepository;
    private final FornecedorRepository fornecedorRepository;
    private final EntradaRepository entradaRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<MovimentacaoResponse> listar(Long produtoId, Pageable pageable) {
        Page<Entrada> pagina = produtoId != null
                ? entradaRepository.findByProdutoId(produtoId, pageable)
                : entradaRepository.findAll(pageable);
        return pagina.map(this::toResponse);
    }

    @Override
    @Transactional
    public MovimentacaoResponse registrar(EntradaRequest request, String emailFuncionarioAutenticado) {
        Produto produto = produtoRepository.findByIdAndAtivoTrue(request.produtoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Produto", request.produtoId()));
        Funcionario funcionario = funcionarioRepository.findByEmail(emailFuncionarioAutenticado)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Funcionário", emailFuncionarioAutenticado));
        Fornecedor fornecedor = request.fornecedorId() != null
                ? fornecedorRepository.findById(request.fornecedorId())
                        .orElseThrow(() -> new RecursoNaoEncontradoException("Fornecedor", request.fornecedorId()))
                : null;

        produto.registrarEntrada(request.quantidade(), request.custoUnitario());
        produtoRepository.save(produto);

        Entrada entrada = Entrada.builder()
                .produto(produto)
                .funcionario(funcionario)
                .quantidade(request.quantidade())
                .custoUnitario(request.custoUnitario())
                .fornecedor(fornecedor)
                .observacao(request.observacao())
                .build();

        return toResponse(entradaRepository.save(entrada));
    }

    private MovimentacaoResponse toResponse(Entrada entrada) {
        return new MovimentacaoResponse(
                entrada.getId(), "ENTRADA", entrada.getProduto().getCodigo(), entrada.getProduto().getNome(),
                entrada.getFuncionario().getNome() + " " + entrada.getFuncionario().getSobrenome(),
                entrada.getQuantidade(), entrada.getDataMovimentacao(), entrada.getObservacao(),
                entrada.getCustoUnitario(), null);
    }
}
```

```java
package com.estoque.controller;

import com.estoque.dto.request.EntradaRequest;
import com.estoque.dto.response.MovimentacaoResponse;
import com.estoque.service.EntradaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/entradas")
@RequiredArgsConstructor
public class EntradaController {

    private final EntradaService entradaService;

    @GetMapping
    public ResponseEntity<Page<MovimentacaoResponse>> listar(
            @RequestParam(required = false) Long produtoId, Pageable pageable) {
        return ResponseEntity.ok(entradaService.listar(produtoId, pageable));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','OPERADOR')")
    public ResponseEntity<MovimentacaoResponse> registrar(@Valid @RequestBody EntradaRequest request,
                                                            Authentication authentication) {
        MovimentacaoResponse resposta = entradaService.registrar(request, authentication.getName());
        return ResponseEntity.created(URI.create("/api/v1/entradas/" + resposta.id())).body(resposta);
    }
}
```

- [ ] **Step 4: Rodar o teste unitário e confirmar sucesso**

Run: `mvn -q -Dtest=EntradaServiceTest test`
Expected: PASS

- [ ] **Step 5: Escrever o teste de integração do controller**

```java
package com.estoque.controller;

import com.estoque.entity.Categoria;
import com.estoque.entity.Produto;
import com.estoque.repository.CategoriaRepository;
import com.estoque.repository.ProdutoRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class EntradaControllerIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private CategoriaRepository categoriaRepository;
    @Autowired private ProdutoRepository produtoRepository;

    @Test
    @WithUserDetails(value = "admin@estoque.com", userDetailsServiceBeanName = "userDetailsServiceImpl")
    void deveRegistrarEntradaEAtualizarEstoque() throws Exception {
        Categoria categoria = categoriaRepository.save(Categoria.builder().nome("Categoria Entrada IT").build());
        Produto produto = produtoRepository.save(Produto.builder()
                .codigo("SKU-ENTRADA-IT").nome("Produto Entrada IT").categoria(categoria)
                .precoVenda(new BigDecimal("10.00")).estoqueMinimo(1).build());

        String corpo = objectMapper.writeValueAsString(
                new com.estoque.dto.request.EntradaRequest(produto.getId(), null, 20, new BigDecimal("5.00"), "compra"));

        mockMvc.perform(post("/api/v1/entradas").contentType("application/json").content(corpo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("ENTRADA"))
                .andExpect(jsonPath("$.quantidade").value(20));
    }
}
```

- [ ] **Step 6: Rodar o teste de integração e confirmar sucesso**

Run: `mvn -q -Dtest=EntradaControllerIT test`
Expected: PASS

- [ ] **Step 7: Commit**

```bash
git add src/main/java/com/estoque/dto/request/EntradaRequest.java src/main/java/com/estoque/dto/response/MovimentacaoResponse.java src/main/java/com/estoque/service/EntradaService.java src/main/java/com/estoque/service/impl/EntradaServiceImpl.java src/main/java/com/estoque/controller/EntradaController.java src/test/java/com/estoque/service/EntradaServiceTest.java src/test/java/com/estoque/controller/EntradaControllerIT.java
git commit -m "feat: adicionar registro de entrada de estoque com atualizacao de custo medio"
```

---

## Task 18: Saida — registro de saída com validação de saldo e concorrência

**Files:**
- Create: `src/main/java/com/estoque/dto/request/SaidaRequest.java`
- Create: `src/main/java/com/estoque/service/SaidaService.java`
- Create: `src/main/java/com/estoque/service/impl/SaidaServiceImpl.java`
- Create: `src/main/java/com/estoque/controller/SaidaController.java`
- Test: `src/test/java/com/estoque/service/SaidaServiceTest.java`
- Test: `src/test/java/com/estoque/controller/SaidaControllerIT.java`
- Test: `src/test/java/com/estoque/service/SaidaConcorrenciaIT.java`

**Interfaces:**
- Consumes: `Produto` (Task 6, método `registrarSaida`), `Saida`/`SaidaRepository` (Task 7), `MovimentacaoResponse` (Task 17), `EstoqueInsuficienteException` (Task 9).
- Produces: `SaidaRequest(Long produtoId, int quantidade, MotivoSaida motivo, String observacao)`, `SaidaService{listar(Long produtoId, Pageable), registrar(SaidaRequest, String emailFuncionarioAutenticado)}`, rotas `GET/POST /api/v1/saidas`.

- [ ] **Step 1: Escrever o teste unitário do service (falha: classes não existem)**

```java
package com.estoque.service;

import com.estoque.dto.request.SaidaRequest;
import com.estoque.entity.Categoria;
import com.estoque.entity.Endereco;
import com.estoque.entity.Funcionario;
import com.estoque.entity.Produto;
import com.estoque.enums.MotivoSaida;
import com.estoque.enums.Role;
import com.estoque.exception.EstoqueInsuficienteException;
import com.estoque.repository.FuncionarioRepository;
import com.estoque.repository.ProdutoRepository;
import com.estoque.repository.SaidaRepository;
import com.estoque.service.impl.SaidaServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SaidaServiceTest {

    @Mock private ProdutoRepository produtoRepository;
    @Mock private FuncionarioRepository funcionarioRepository;
    @Mock private SaidaRepository saidaRepository;
    @InjectMocks private SaidaServiceImpl saidaService;

    private Produto produtoComEstoque(int quantidade) {
        Produto produto = Produto.builder().codigo("SKU-1").nome("Camiseta")
                .categoria(Categoria.builder().nome("Roupas").build())
                .precoVenda(new BigDecimal("49.90")).estoqueMinimo(5).build();
        produto.registrarEntrada(quantidade, new BigDecimal("10.00"));
        return produto;
    }

    private Funcionario novoFuncionario() {
        return Funcionario.builder().nome("Ana").sobrenome("Silva").cpf("11122233344")
                .email("ana@estoque.com").senha("hash").matricula("F001").dataAdmissao(LocalDate.now())
                .setor("Loja").role(Role.OPERADOR)
                .endereco(new Endereco("Rua B", "10", "Centro", "Curitiba", "PR", "80000-000"))
                .build();
    }

    @Test
    void deveRegistrarSaidaEDecrementarEstoqueQuandoHaSaldo() {
        Produto produto = produtoComEstoque(10);
        Funcionario funcionario = novoFuncionario();
        SaidaRequest request = new SaidaRequest(1L, 4, MotivoSaida.VENDA, "venda balcão");

        when(produtoRepository.findByIdAndAtivoTrue(1L)).thenReturn(Optional.of(produto));
        when(funcionarioRepository.findByEmail("ana@estoque.com")).thenReturn(Optional.of(funcionario));
        when(produtoRepository.save(produto)).thenReturn(produto);
        when(saidaRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        saidaService.registrar(request, "ana@estoque.com");

        assertThat(produto.getQuantidadeEstoque()).isEqualTo(6);
        verify(produtoRepository).save(produto);
    }

    @Test
    void deveLancarEstoqueInsuficienteQuandoQuantidadeMaiorQueSaldo() {
        Produto produto = produtoComEstoque(2);
        SaidaRequest request = new SaidaRequest(1L, 5, MotivoSaida.VENDA, null);

        when(produtoRepository.findByIdAndAtivoTrue(1L)).thenReturn(Optional.of(produto));

        assertThatThrownBy(() -> saidaService.registrar(request, "ana@estoque.com"))
                .isInstanceOf(EstoqueInsuficienteException.class);

        verify(produtoRepository, org.mockito.Mockito.never()).save(any());
    }
}
```

- [ ] **Step 2: Rodar e confirmar falha**

Run: `mvn -q -Dtest=SaidaServiceTest test`
Expected: FAIL — classes não existem.

- [ ] **Step 3: Implementar DTO, service e controller**

```java
package com.estoque.dto.request;

import com.estoque.enums.MotivoSaida;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record SaidaRequest(
        @NotNull Long produtoId,
        @Positive int quantidade,
        @NotNull MotivoSaida motivo,
        @Size(max = 255) String observacao) {
}
```

```java
package com.estoque.service;

import com.estoque.dto.request.SaidaRequest;
import com.estoque.dto.response.MovimentacaoResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SaidaService {
    Page<MovimentacaoResponse> listar(Long produtoId, Pageable pageable);
    MovimentacaoResponse registrar(SaidaRequest request, String emailFuncionarioAutenticado);
}
```

```java
package com.estoque.service.impl;

import com.estoque.dto.request.SaidaRequest;
import com.estoque.dto.response.MovimentacaoResponse;
import com.estoque.entity.Funcionario;
import com.estoque.entity.Produto;
import com.estoque.entity.Saida;
import com.estoque.exception.RecursoNaoEncontradoException;
import com.estoque.repository.FuncionarioRepository;
import com.estoque.repository.ProdutoRepository;
import com.estoque.repository.SaidaRepository;
import com.estoque.service.SaidaService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SaidaServiceImpl implements SaidaService {

    private final ProdutoRepository produtoRepository;
    private final FuncionarioRepository funcionarioRepository;
    private final SaidaRepository saidaRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<MovimentacaoResponse> listar(Long produtoId, Pageable pageable) {
        Page<Saida> pagina = produtoId != null
                ? saidaRepository.findByProdutoId(produtoId, pageable)
                : saidaRepository.findAll(pageable);
        return pagina.map(this::toResponse);
    }

    /**
     * A validação de saldo (dentro de {@link Produto#registrarSaida}) e a persistência do produto
     * acontecem na mesma transação: se duas requisições concorrentes lerem o mesmo saldo, o
     * {@code @Version} de {@link Produto} garante que a segunda gravação falhe com
     * {@link org.springframework.orm.ObjectOptimisticLockingFailureException} (mapeada para 409),
     * em vez de sobrescrever silenciosamente o resultado da primeira (lost update).
     */
    @Override
    @Transactional
    public MovimentacaoResponse registrar(SaidaRequest request, String emailFuncionarioAutenticado) {
        Produto produto = produtoRepository.findByIdAndAtivoTrue(request.produtoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Produto", request.produtoId()));
        Funcionario funcionario = funcionarioRepository.findByEmail(emailFuncionarioAutenticado)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Funcionário", emailFuncionarioAutenticado));

        produto.registrarSaida(request.quantidade());
        produtoRepository.save(produto);

        Saida saida = Saida.builder()
                .produto(produto)
                .funcionario(funcionario)
                .quantidade(request.quantidade())
                .motivo(request.motivo())
                .observacao(request.observacao())
                .build();

        return toResponse(saidaRepository.save(saida));
    }

    private MovimentacaoResponse toResponse(Saida saida) {
        return new MovimentacaoResponse(
                saida.getId(), "SAIDA", saida.getProduto().getCodigo(), saida.getProduto().getNome(),
                saida.getFuncionario().getNome() + " " + saida.getFuncionario().getSobrenome(),
                saida.getQuantidade(), saida.getDataMovimentacao(), saida.getObservacao(),
                null, saida.getMotivo().name());
    }
}
```

```java
package com.estoque.controller;

import com.estoque.dto.request.SaidaRequest;
import com.estoque.dto.response.MovimentacaoResponse;
import com.estoque.service.SaidaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/saidas")
@RequiredArgsConstructor
public class SaidaController {

    private final SaidaService saidaService;

    @GetMapping
    public ResponseEntity<Page<MovimentacaoResponse>> listar(
            @RequestParam(required = false) Long produtoId, Pageable pageable) {
        return ResponseEntity.ok(saidaService.listar(produtoId, pageable));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','OPERADOR')")
    public ResponseEntity<MovimentacaoResponse> registrar(@Valid @RequestBody SaidaRequest request,
                                                            Authentication authentication) {
        MovimentacaoResponse resposta = saidaService.registrar(request, authentication.getName());
        return ResponseEntity.created(URI.create("/api/v1/saidas/" + resposta.id())).body(resposta);
    }
}
```

- [ ] **Step 4: Rodar o teste unitário e confirmar sucesso**

Run: `mvn -q -Dtest=SaidaServiceTest test`
Expected: PASS

- [ ] **Step 5: Escrever o teste de integração do controller**

```java
package com.estoque.controller;

import com.estoque.entity.Categoria;
import com.estoque.entity.Produto;
import com.estoque.repository.CategoriaRepository;
import com.estoque.repository.ProdutoRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SaidaControllerIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private CategoriaRepository categoriaRepository;
    @Autowired private ProdutoRepository produtoRepository;

    @Test
    @WithUserDetails(value = "admin@estoque.com", userDetailsServiceBeanName = "userDetailsServiceImpl")
    void deveRejeitarSaidaComEstoqueInsuficienteCom409() throws Exception {
        Categoria categoria = categoriaRepository.save(Categoria.builder().nome("Categoria Saida IT").build());
        Produto produto = produtoRepository.save(Produto.builder()
                .codigo("SKU-SAIDA-IT").nome("Produto Saida IT").categoria(categoria)
                .precoVenda(new BigDecimal("10.00")).estoqueMinimo(1).build());

        String corpo = objectMapper.writeValueAsString(new com.estoque.dto.request.SaidaRequest(
                produto.getId(), 5, com.estoque.enums.MotivoSaida.VENDA, "venda sem estoque"));

        mockMvc.perform(post("/api/v1/saidas").contentType("application/json").content(corpo))
                .andExpect(status().isConflict());
    }
}
```

- [ ] **Step 6: Rodar o teste de integração e confirmar sucesso**

Run: `mvn -q -Dtest=SaidaControllerIT test`
Expected: PASS

- [ ] **Step 7: Escrever o teste de concorrência (integração real, sem mocks)**

```java
package com.estoque.service;

import com.estoque.dto.request.SaidaRequest;
import com.estoque.entity.Categoria;
import com.estoque.entity.Produto;
import com.estoque.enums.MotivoSaida;
import com.estoque.exception.BusinessException;
import com.estoque.repository.CategoriaRepository;
import com.estoque.repository.ProdutoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class SaidaConcorrenciaIT {

    @Autowired private SaidaService saidaService;
    @Autowired private ProdutoRepository produtoRepository;
    @Autowired private CategoriaRepository categoriaRepository;

    @Test
    void naoDevePermitirVenderMaisDoQueOEstoqueDisponivelSobConcorrencia() throws Exception {
        Categoria categoria = categoriaRepository.save(Categoria.builder().nome("Categoria Concorrencia IT").build());
        Produto produto = produtoRepository.save(Produto.builder()
                .codigo("SKU-CONCORRENCIA-IT").nome("Produto Concorrência").categoria(categoria)
                .precoVenda(new BigDecimal("10.00")).estoqueMinimo(1).build());
        produto.registrarEntrada(10, new BigDecimal("5.00"));
        produtoRepository.saveAndFlush(produto);

        int totalThreads = 5;
        int quantidadePorThread = 3; // 5 x 3 = 15 > 10 disponíveis: overselling deve ser impedido
        ExecutorService executor = Executors.newFixedThreadPool(totalThreads);
        CountDownLatch largada = new CountDownLatch(1);
        AtomicInteger sucessos = new AtomicInteger();
        AtomicInteger falhasControladas = new AtomicInteger();

        List<Callable<Void>> tarefas = java.util.stream.IntStream.range(0, totalThreads)
                .<Callable<Void>>mapToObj(i -> () -> {
                    largada.await();
                    try {
                        saidaService.registrar(
                                new SaidaRequest(produto.getId(), quantidadePorThread, MotivoSaida.VENDA, "teste concorrência"),
                                "admin@estoque.com");
                        sucessos.incrementAndGet();
                    } catch (BusinessException | ObjectOptimisticLockingFailureException e) {
                        falhasControladas.incrementAndGet();
                    }
                    return null;
                })
                .toList();

        List<Future<Void>> futures = executor.invokeAll(tarefas);
        largada.countDown();
        for (Future<Void> future : futures) {
            future.get(10, TimeUnit.SECONDS);
        }
        executor.shutdown();

        Produto produtoFinal = produtoRepository.findById(produto.getId()).orElseThrow();

        assertThat(produtoFinal.getQuantidadeEstoque()).isGreaterThanOrEqualTo(0);
        assertThat(sucessos.get() + falhasControladas.get()).isEqualTo(totalThreads);
        assertThat(sucessos.get() * quantidadePorThread).isLessThanOrEqualTo(10);
    }
}
```

Nota: `invokeAll` já bloqueia até todas as tarefas serem submetidas às threads do pool antes de `largada.countDown()` liberar a execução simultânea — isso maximiza a chance real de colisão, mas o teste é correto independentemente da ordem exata de entrelaçamento, pois a asserção é sobre o resultado final agregado (nunca vender mais do que existia), não sobre timing.

- [ ] **Step 8: Rodar o teste de concorrência e confirmar sucesso**

Run: `mvn -q -Dtest=SaidaConcorrenciaIT test`
Expected: PASS (pode levar alguns segundos; se falhar de forma intermitente, aumentar `totalThreads` não é necessário — investigar se `@Version` está de fato mapeado em `Produto`, Task 6)

- [ ] **Step 9: Commit**

```bash
git add src/main/java/com/estoque/dto/request/SaidaRequest.java src/main/java/com/estoque/service/SaidaService.java src/main/java/com/estoque/service/impl/SaidaServiceImpl.java src/main/java/com/estoque/controller/SaidaController.java src/test/java/com/estoque/service/SaidaServiceTest.java src/test/java/com/estoque/controller/SaidaControllerIT.java src/test/java/com/estoque/service/SaidaConcorrenciaIT.java
git commit -m "feat: adicionar registro de saida com validacao de saldo e teste de concorrencia"
```

---

## Task 19: Kardex — histórico polimórfico de movimentações por produto

**Files:**
- Create: `src/main/java/com/estoque/mapper/MovimentacaoEstoqueMapper.java`
- Modify: `src/main/java/com/estoque/service/impl/EntradaServiceImpl.java` (Task 17) — passa a usar `MovimentacaoEstoqueMapper` em vez do método privado `toResponse`
- Modify: `src/main/java/com/estoque/service/impl/SaidaServiceImpl.java` (Task 18) — idem
- Modify: `src/main/java/com/estoque/service/ProdutoService.java` (Task 16) — novo método `listarMovimentacoes`
- Modify: `src/main/java/com/estoque/service/impl/ProdutoServiceImpl.java` (Task 16)
- Modify: `src/main/java/com/estoque/controller/ProdutoController.java` (Task 16) — nova rota
- Test: `src/test/java/com/estoque/mapper/MovimentacaoEstoqueMapperTest.java`
- Test: `src/test/java/com/estoque/controller/ProdutoKardexIT.java`

**Interfaces:**
- Consumes: `MovimentacaoEstoque`/`MovimentacaoEstoqueRepository` (Task 7), `MovimentacaoResponse` (Task 17).
- Produces: `MovimentacaoEstoqueMapper.toResponse(MovimentacaoEstoque)`, `ProdutoService.listarMovimentacoes(Long produtoId, Pageable pageable)`, rota `GET /api/v1/produtos/{id}/movimentacoes`.

- [ ] **Step 1: Escrever o teste unitário do mapper (falha: classe não existe)**

```java
package com.estoque.mapper;

import com.estoque.entity.Categoria;
import com.estoque.entity.Endereco;
import com.estoque.entity.Entrada;
import com.estoque.entity.Funcionario;
import com.estoque.entity.Produto;
import com.estoque.entity.Saida;
import com.estoque.enums.MotivoSaida;
import com.estoque.enums.Role;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class MovimentacaoEstoqueMapperTest {

    private final MovimentacaoEstoqueMapper mapper = new MovimentacaoEstoqueMapper();

    private Produto produto() {
        return Produto.builder().codigo("SKU-1").nome("Camiseta")
                .categoria(Categoria.builder().nome("Roupas").build())
                .precoVenda(new BigDecimal("49.90")).estoqueMinimo(5).build();
    }

    private Funcionario funcionario() {
        return Funcionario.builder().nome("Ana").sobrenome("Silva").cpf("11122233344")
                .email("ana@estoque.com").senha("hash").matricula("F001").dataAdmissao(LocalDate.now())
                .setor("Loja").role(Role.OPERADOR)
                .endereco(new Endereco("Rua B", "10", "Centro", "Curitiba", "PR", "80000-000"))
                .build();
    }

    @Test
    void deveMapearEntradaComTipoECustoUnitario() {
        Entrada entrada = Entrada.builder().produto(produto()).funcionario(funcionario())
                .quantidade(5).custoUnitario(new BigDecimal("12.50")).build();

        var resposta = mapper.toResponse(entrada);

        assertThat(resposta.tipo()).isEqualTo("ENTRADA");
        assertThat(resposta.custoUnitario()).isEqualByComparingTo("12.50");
        assertThat(resposta.motivo()).isNull();
    }

    @Test
    void deveMapearSaidaComTipoEMotivo() {
        Saida saida = Saida.builder().produto(produto()).funcionario(funcionario())
                .quantidade(2).motivo(MotivoSaida.PERDA).build();

        var resposta = mapper.toResponse(saida);

        assertThat(resposta.tipo()).isEqualTo("SAIDA");
        assertThat(resposta.motivo()).isEqualTo("PERDA");
        assertThat(resposta.custoUnitario()).isNull();
    }
}
```

- [ ] **Step 2: Rodar e confirmar falha**

Run: `mvn -q -Dtest=MovimentacaoEstoqueMapperTest test`
Expected: FAIL — classe não existe.

- [ ] **Step 3: Implementar `MovimentacaoEstoqueMapper`**

```java
package com.estoque.mapper;

import com.estoque.dto.response.MovimentacaoResponse;
import com.estoque.entity.Entrada;
import com.estoque.entity.MovimentacaoEstoque;
import com.estoque.entity.Saida;
import org.springframework.stereotype.Component;

@Component
public class MovimentacaoEstoqueMapper {

    public MovimentacaoResponse toResponse(MovimentacaoEstoque movimentacao) {
        String nomeFuncionario = movimentacao.getFuncionario().getNome() + " " + movimentacao.getFuncionario().getSobrenome();

        if (movimentacao instanceof Entrada entrada) {
            return new MovimentacaoResponse(entrada.getId(), "ENTRADA", entrada.getProduto().getCodigo(),
                    entrada.getProduto().getNome(), nomeFuncionario, entrada.getQuantidade(),
                    entrada.getDataMovimentacao(), entrada.getObservacao(), entrada.getCustoUnitario(), null);
        }
        if (movimentacao instanceof Saida saida) {
            return new MovimentacaoResponse(saida.getId(), "SAIDA", saida.getProduto().getCodigo(),
                    saida.getProduto().getNome(), nomeFuncionario, saida.getQuantidade(),
                    saida.getDataMovimentacao(), saida.getObservacao(), null, saida.getMotivo().name());
        }
        throw new IllegalStateException("Tipo de movimentação não suportado: " + movimentacao.getClass());
    }
}
```

- [ ] **Step 4: Rodar o teste do mapper e confirmar sucesso**

Run: `mvn -q -Dtest=MovimentacaoEstoqueMapperTest test`
Expected: PASS

- [ ] **Step 5: Atualizar `EntradaServiceImpl` e `SaidaServiceImpl` para reutilizar o mapper (remove duplicação de mapeamento)**

```java
package com.estoque.service.impl;

import com.estoque.dto.request.EntradaRequest;
import com.estoque.dto.response.MovimentacaoResponse;
import com.estoque.entity.Entrada;
import com.estoque.entity.Fornecedor;
import com.estoque.entity.Funcionario;
import com.estoque.entity.Produto;
import com.estoque.exception.RecursoNaoEncontradoException;
import com.estoque.mapper.MovimentacaoEstoqueMapper;
import com.estoque.repository.EntradaRepository;
import com.estoque.repository.FornecedorRepository;
import com.estoque.repository.FuncionarioRepository;
import com.estoque.repository.ProdutoRepository;
import com.estoque.service.EntradaService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EntradaServiceImpl implements EntradaService {

    private final ProdutoRepository produtoRepository;
    private final FuncionarioRepository funcionarioRepository;
    private final FornecedorRepository fornecedorRepository;
    private final EntradaRepository entradaRepository;
    private final MovimentacaoEstoqueMapper movimentacaoMapper;

    @Override
    @Transactional(readOnly = true)
    public Page<MovimentacaoResponse> listar(Long produtoId, Pageable pageable) {
        Page<Entrada> pagina = produtoId != null
                ? entradaRepository.findByProdutoId(produtoId, pageable)
                : entradaRepository.findAll(pageable);
        return pagina.map(movimentacaoMapper::toResponse);
    }

    @Override
    @Transactional
    public MovimentacaoResponse registrar(EntradaRequest request, String emailFuncionarioAutenticado) {
        Produto produto = produtoRepository.findByIdAndAtivoTrue(request.produtoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Produto", request.produtoId()));
        Funcionario funcionario = funcionarioRepository.findByEmail(emailFuncionarioAutenticado)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Funcionário", emailFuncionarioAutenticado));
        Fornecedor fornecedor = request.fornecedorId() != null
                ? fornecedorRepository.findById(request.fornecedorId())
                        .orElseThrow(() -> new RecursoNaoEncontradoException("Fornecedor", request.fornecedorId()))
                : null;

        produto.registrarEntrada(request.quantidade(), request.custoUnitario());
        produtoRepository.save(produto);

        Entrada entrada = Entrada.builder()
                .produto(produto)
                .funcionario(funcionario)
                .quantidade(request.quantidade())
                .custoUnitario(request.custoUnitario())
                .fornecedor(fornecedor)
                .observacao(request.observacao())
                .build();

        return movimentacaoMapper.toResponse(entradaRepository.save(entrada));
    }
}
```

```java
package com.estoque.service.impl;

import com.estoque.dto.request.SaidaRequest;
import com.estoque.dto.response.MovimentacaoResponse;
import com.estoque.entity.Funcionario;
import com.estoque.entity.Produto;
import com.estoque.entity.Saida;
import com.estoque.exception.RecursoNaoEncontradoException;
import com.estoque.mapper.MovimentacaoEstoqueMapper;
import com.estoque.repository.FuncionarioRepository;
import com.estoque.repository.ProdutoRepository;
import com.estoque.repository.SaidaRepository;
import com.estoque.service.SaidaService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SaidaServiceImpl implements SaidaService {

    private final ProdutoRepository produtoRepository;
    private final FuncionarioRepository funcionarioRepository;
    private final SaidaRepository saidaRepository;
    private final MovimentacaoEstoqueMapper movimentacaoMapper;

    @Override
    @Transactional(readOnly = true)
    public Page<MovimentacaoResponse> listar(Long produtoId, Pageable pageable) {
        Page<Saida> pagina = produtoId != null
                ? saidaRepository.findByProdutoId(produtoId, pageable)
                : saidaRepository.findAll(pageable);
        return pagina.map(movimentacaoMapper::toResponse);
    }

    @Override
    @Transactional
    public MovimentacaoResponse registrar(SaidaRequest request, String emailFuncionarioAutenticado) {
        Produto produto = produtoRepository.findByIdAndAtivoTrue(request.produtoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Produto", request.produtoId()));
        Funcionario funcionario = funcionarioRepository.findByEmail(emailFuncionarioAutenticado)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Funcionário", emailFuncionarioAutenticado));

        produto.registrarSaida(request.quantidade());
        produtoRepository.save(produto);

        Saida saida = Saida.builder()
                .produto(produto)
                .funcionario(funcionario)
                .quantidade(request.quantidade())
                .motivo(request.motivo())
                .observacao(request.observacao())
                .build();

        return movimentacaoMapper.toResponse(saidaRepository.save(saida));
    }
}
```

- [ ] **Step 6: Adicionar `listarMovimentacoes` em `ProdutoService`/`ProdutoServiceImpl` e a rota em `ProdutoController`**

Em `ProdutoService` (Task 16), adicionar à interface:

```java
    Page<com.estoque.dto.response.MovimentacaoResponse> listarMovimentacoes(Long produtoId, Pageable pageable);
```

Em `ProdutoServiceImpl` (Task 16), injetar `MovimentacaoEstoqueRepository` e `MovimentacaoEstoqueMapper` no construtor (Lombok `@RequiredArgsConstructor` já gera o construtor a partir dos novos campos `private final`) e implementar:

```java
    @Override
    @Transactional(readOnly = true)
    public Page<com.estoque.dto.response.MovimentacaoResponse> listarMovimentacoes(Long produtoId, Pageable pageable) {
        buscarEntidadeAtivaPorId(produtoId);
        return movimentacaoEstoqueRepository
                .findByProdutoIdOrderByDataMovimentacaoDesc(produtoId, pageable)
                .map(movimentacaoEstoqueMapper::toResponse);
    }
```

Em `ProdutoController` (Task 16), adicionar:

```java
    @GetMapping("/{id}/movimentacoes")
    public ResponseEntity<Page<com.estoque.dto.response.MovimentacaoResponse>> listarMovimentacoes(
            @PathVariable Long id, Pageable pageable) {
        return ResponseEntity.ok(produtoService.listarMovimentacoes(id, pageable));
    }
```

- [ ] **Step 7: Escrever o teste de integração do kardex**

```java
package com.estoque.controller;

import com.estoque.entity.Categoria;
import com.estoque.entity.Produto;
import com.estoque.repository.CategoriaRepository;
import com.estoque.repository.ProdutoRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ProdutoKardexIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private CategoriaRepository categoriaRepository;
    @Autowired private ProdutoRepository produtoRepository;

    @Test
    @WithUserDetails(value = "admin@estoque.com", userDetailsServiceBeanName = "userDetailsServiceImpl")
    void deveListarEntradasESaidasMisturadasNoKardex() throws Exception {
        Categoria categoria = categoriaRepository.save(Categoria.builder().nome("Categoria Kardex IT").build());
        Produto produto = produtoRepository.save(Produto.builder()
                .codigo("SKU-KARDEX-IT").nome("Produto Kardex IT").categoria(categoria)
                .precoVenda(new BigDecimal("10.00")).estoqueMinimo(1).build());

        mockMvc.perform(post("/api/v1/entradas").contentType("application/json").content(
                        objectMapper.writeValueAsString(new com.estoque.dto.request.EntradaRequest(
                                produto.getId(), null, 10, new BigDecimal("5.00"), null))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/saidas").contentType("application/json").content(
                        objectMapper.writeValueAsString(new com.estoque.dto.request.SaidaRequest(
                                produto.getId(), 3, com.estoque.enums.MotivoSaida.VENDA, null))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/produtos/" + produto.getId() + "/movimentacoes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));
    }
}
```

- [ ] **Step 8: Rodar todos os testes afetados e confirmar sucesso**

Run: `mvn -q -Dtest=MovimentacaoEstoqueMapperTest,ProdutoKardexIT,EntradaServiceTest,SaidaServiceTest,EntradaControllerIT,SaidaControllerIT test`
Expected: PASS

- [ ] **Step 9: Commit**

```bash
git add src/main/java/com/estoque/mapper/MovimentacaoEstoqueMapper.java src/main/java/com/estoque/service/impl/EntradaServiceImpl.java src/main/java/com/estoque/service/impl/SaidaServiceImpl.java src/main/java/com/estoque/service/ProdutoService.java src/main/java/com/estoque/service/impl/ProdutoServiceImpl.java src/main/java/com/estoque/controller/ProdutoController.java src/test/java/com/estoque/mapper/MovimentacaoEstoqueMapperTest.java src/test/java/com/estoque/controller/ProdutoKardexIT.java
git commit -m "feat: adicionar kardex de movimentacoes por produto e remover duplicacao de mapeamento"
```

---

## Task 20: Relatórios — valor total do estoque e estoque baixo consolidado

**Files:**
- Modify: `src/main/java/com/estoque/repository/ProdutoRepository.java` (Task 6) — adiciona `somaValorEstoque()` e `countByAtivoTrue()`
- Create: `src/main/java/com/estoque/dto/response/RelatorioEstoqueResponse.java`
- Create: `src/main/java/com/estoque/service/RelatorioService.java`
- Create: `src/main/java/com/estoque/service/impl/RelatorioServiceImpl.java`
- Create: `src/main/java/com/estoque/controller/RelatorioController.java`
- Test: `src/test/java/com/estoque/service/RelatorioServiceTest.java`
- Test: `src/test/java/com/estoque/controller/RelatorioControllerIT.java`

**Interfaces:**
- Consumes: `ProdutoRepository` (Task 6), `ProdutoService.listarComEstoqueBaixo()` (Task 16).
- Produces: `RelatorioEstoqueResponse(BigDecimal valorTotalEstoque, long quantidadeProdutosAtivos, LocalDateTime geradoEm)`, `RelatorioService{gerarRelatorioValorEstoque(), listarProdutosComEstoqueBaixo()}`, rotas `GET /api/v1/relatorios/valor-estoque` e `GET /api/v1/relatorios/estoque-baixo`.

- [ ] **Step 1: Escrever o teste unitário do service (falha: classes não existem)**

```java
package com.estoque.service;

import com.estoque.dto.response.RelatorioEstoqueResponse;
import com.estoque.repository.ProdutoRepository;
import com.estoque.service.impl.RelatorioServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RelatorioServiceTest {

    @Mock private ProdutoRepository produtoRepository;
    @Mock private ProdutoService produtoService;
    @InjectMocks private RelatorioServiceImpl relatorioService;

    @Test
    void deveCalcularValorTotalDoEstoqueEQuantidadeDeProdutosAtivos() {
        when(produtoRepository.somaValorEstoque()).thenReturn(new BigDecimal("1250.75"));
        when(produtoRepository.countByAtivoTrue()).thenReturn(8L);

        RelatorioEstoqueResponse resultado = relatorioService.gerarRelatorioValorEstoque();

        assertThat(resultado.valorTotalEstoque()).isEqualByComparingTo("1250.75");
        assertThat(resultado.quantidadeProdutosAtivos()).isEqualTo(8L);
        assertThat(resultado.geradoEm()).isNotNull();
    }
}
```

- [ ] **Step 2: Rodar e confirmar falha**

Run: `mvn -q -Dtest=RelatorioServiceTest test`
Expected: FAIL — classes não existem.

- [ ] **Step 3: Adicionar as consultas em `ProdutoRepository`**

```java
    @Query("SELECT COALESCE(SUM(p.quantidadeEstoque * p.custoMedio), 0) FROM Produto p WHERE p.ativo = true")
    java.math.BigDecimal somaValorEstoque();

    long countByAtivoTrue();
```

(adicionar essas duas linhas à interface `ProdutoRepository` criada na Task 6, mantendo os métodos já existentes)

- [ ] **Step 4: Implementar DTO, service e controller**

```java
package com.estoque.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RelatorioEstoqueResponse(
        BigDecimal valorTotalEstoque, long quantidadeProdutosAtivos, LocalDateTime geradoEm) {
}
```

```java
package com.estoque.service;

import com.estoque.dto.response.ProdutoResponse;
import com.estoque.dto.response.RelatorioEstoqueResponse;

import java.util.List;

public interface RelatorioService {
    RelatorioEstoqueResponse gerarRelatorioValorEstoque();
    List<ProdutoResponse> listarProdutosComEstoqueBaixo();
}
```

```java
package com.estoque.service.impl;

import com.estoque.dto.response.ProdutoResponse;
import com.estoque.dto.response.RelatorioEstoqueResponse;
import com.estoque.repository.ProdutoRepository;
import com.estoque.service.ProdutoService;
import com.estoque.service.RelatorioService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RelatorioServiceImpl implements RelatorioService {

    private final ProdutoRepository produtoRepository;
    private final ProdutoService produtoService;

    @Override
    @Transactional(readOnly = true)
    public RelatorioEstoqueResponse gerarRelatorioValorEstoque() {
        return new RelatorioEstoqueResponse(
                produtoRepository.somaValorEstoque(),
                produtoRepository.countByAtivoTrue(),
                LocalDateTime.now());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProdutoResponse> listarProdutosComEstoqueBaixo() {
        return produtoService.listarComEstoqueBaixo();
    }
}
```

```java
package com.estoque.controller;

import com.estoque.dto.response.ProdutoResponse;
import com.estoque.dto.response.RelatorioEstoqueResponse;
import com.estoque.service.RelatorioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/relatorios")
@RequiredArgsConstructor
public class RelatorioController {

    private final RelatorioService relatorioService;

    @GetMapping("/valor-estoque")
    public ResponseEntity<RelatorioEstoqueResponse> valorEstoque() {
        return ResponseEntity.ok(relatorioService.gerarRelatorioValorEstoque());
    }

    @GetMapping("/estoque-baixo")
    public ResponseEntity<List<ProdutoResponse>> estoqueBaixo() {
        return ResponseEntity.ok(relatorioService.listarProdutosComEstoqueBaixo());
    }
}
```

- [ ] **Step 5: Rodar o teste unitário e confirmar sucesso**

Run: `mvn -q -Dtest=RelatorioServiceTest test`
Expected: PASS

- [ ] **Step 6: Escrever o teste de integração**

```java
package com.estoque.controller;

import com.estoque.entity.Categoria;
import com.estoque.entity.Produto;
import com.estoque.repository.CategoriaRepository;
import com.estoque.repository.ProdutoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class RelatorioControllerIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private CategoriaRepository categoriaRepository;
    @Autowired private ProdutoRepository produtoRepository;

    @Test
    @WithMockUser
    void deveRetornarValorTotalDoEstoque() throws Exception {
        Categoria categoria = categoriaRepository.save(Categoria.builder().nome("Categoria Relatorio IT").build());
        Produto produto = Produto.builder().codigo("SKU-RELATORIO-IT").nome("Produto Relatorio IT")
                .categoria(categoria).precoVenda(new BigDecimal("10.00")).estoqueMinimo(1).build();
        produto.registrarEntrada(4, new BigDecimal("25.00"));
        produtoRepository.save(produto);

        mockMvc.perform(get("/api/v1/relatorios/valor-estoque"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valorTotalEstoque").exists())
                .andExpect(jsonPath("$.quantidadeProdutosAtivos").exists());
    }
}
```

- [ ] **Step 7: Rodar o teste de integração e confirmar sucesso**

Run: `mvn -q -Dtest=RelatorioControllerIT test`
Expected: PASS

- [ ] **Step 8: Commit**

```bash
git add src/main/java/com/estoque/repository/ProdutoRepository.java src/main/java/com/estoque/dto/response/RelatorioEstoqueResponse.java src/main/java/com/estoque/service/RelatorioService.java src/main/java/com/estoque/service/impl/RelatorioServiceImpl.java src/main/java/com/estoque/controller/RelatorioController.java src/test/java/com/estoque/service/RelatorioServiceTest.java src/test/java/com/estoque/controller/RelatorioControllerIT.java
git commit -m "feat: adicionar relatorios de valor de estoque e estoque baixo"
```

---

## Task 21: Documentação OpenAPI/Swagger com esquema JWT

**Files:**
- Create: `src/main/java/com/estoque/config/OpenApiConfig.java`
- Test: `src/test/java/com/estoque/config/OpenApiConfigIT.java`

**Interfaces:**
- Consumes: nenhum bean adicional além do springdoc já presente no `pom.xml` (Task 1).
- Produces: `GET /v3/api-docs` (JSON OpenAPI) e `GET /swagger-ui/index.html`, ambos públicos (já liberados em `SecurityConfig.ROTAS_PUBLICAS`, Task 11), com esquema de segurança `bearer-jwt` documentado.

- [ ] **Step 1: Escrever o teste de integração (falha: título customizado ainda não aparece)**

```java
package com.estoque.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OpenApiConfigIT {

    @Autowired private MockMvc mockMvc;

    @Test
    void deveExporDocumentacaoOpenApiComEsquemaJwtSemAutenticacao() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Controle de Estoque API"))
                .andExpect(jsonPath("$.components.securitySchemes.bearer-jwt.scheme").value("bearer"));
    }
}
```

- [ ] **Step 2: Rodar e confirmar falha**

Run: `mvn -q -Dtest=OpenApiConfigIT test`
Expected: FAIL — título/esquema padrão do springdoc não correspondem ao esperado.

- [ ] **Step 3: Implementar `OpenApiConfig`**

```java
package com.estoque.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String ESQUEMA_JWT = "bearer-jwt";

    @Bean
    public OpenAPI estoqueOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Controle de Estoque API")
                        .description("API REST para controle de estoque: produtos, fornecedores, funcionários e movimentações de entrada/saída.")
                        .version("v1"))
                .addSecurityItem(new SecurityRequirement().addList(ESQUEMA_JWT))
                .components(new Components().addSecuritySchemes(ESQUEMA_JWT,
                        new SecurityScheme()
                                .name(ESQUEMA_JWT)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
```

- [ ] **Step 4: Rodar o teste e confirmar sucesso**

Run: `mvn -q -Dtest=OpenApiConfigIT test`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/estoque/config/OpenApiConfig.java src/test/java/com/estoque/config/OpenApiConfigIT.java
git commit -m "feat: configurar documentacao OpenAPI com esquema de autenticacao JWT"
```

---

## Task 22: Docker Compose (PostgreSQL) e perfil de produção

**Files:**
- Create: `Dockerfile`
- Create: `docker-compose.yml`
- Create: `src/main/resources/application-prod.yml`
- Create: `.dockerignore`

**Interfaces:**
- Produces: `docker compose up --build` sobe PostgreSQL 16 + a aplicação no perfil `prod`, aplicando as migrations Flyway das Tasks 3–8 contra Postgres real (validando que o SQL das migrations — escrito com sintaxe compatível H2/Postgres — também funciona fora do H2).

- [ ] **Step 1: Criar `.dockerignore`**

```
target/
.git/
.metadata/
bin/
*.md
```

- [ ] **Step 2: Criar o `Dockerfile` (multi-stage)**

```dockerfile
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q dependency:go-offline
COPY src ./src
RUN mvn -q clean package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/controle-estoque-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

- [ ] **Step 3: Criar `application-prod.yml`**

```yaml
spring:
  datasource:
    url: ${SPRING_DATASOURCE_URL}
    username: ${SPRING_DATASOURCE_USERNAME}
    password: ${SPRING_DATASOURCE_PASSWORD}
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: validate
    open-in-view: false
  flyway:
    enabled: true
    locations: classpath:db/migration
  h2:
    console:
      enabled: false

logging:
  level:
    com.estoque: INFO
```

- [ ] **Step 4: Criar `docker-compose.yml`**

```yaml
services:
  postgres:
    image: postgres:16
    environment:
      POSTGRES_DB: estoque
      POSTGRES_USER: estoque
      POSTGRES_PASSWORD: estoque
    ports:
      - "5432:5432"
    volumes:
      - estoque-postgres-data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U estoque"]
      interval: 5s
      timeout: 5s
      retries: 5

  app:
    build: .
    depends_on:
      postgres:
        condition: service_healthy
    environment:
      SPRING_PROFILES_ACTIVE: prod
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/estoque
      SPRING_DATASOURCE_USERNAME: estoque
      SPRING_DATASOURCE_PASSWORD: estoque
      JWT_SECRET: MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDEyMzQ1Njc4OTAxMjM0NTY3ODk=
    ports:
      - "8080:8080"

volumes:
  estoque-postgres-data:
```

- [ ] **Step 5: Validar que o build de produção compila (verificação possível sem depender do Docker daemon)**

Run: `mvn -q clean package -DskipTests`
Expected: BUILD SUCCESS, gera `target/controle-estoque-1.0.0.jar`.

Se o Docker estiver disponível no ambiente de quem for rodar o projeto depois, `docker compose up --build` é o passo de verificação completo (sobe Postgres real e valida as migrations Flyway fora do H2) — documentar isso no README (Task 23) em vez de depender do Docker aqui, já que o daemon pode não estar disponível neste ambiente de implementação.

- [ ] **Step 6: Commit**

```bash
git add Dockerfile docker-compose.yml src/main/resources/application-prod.yml .dockerignore
git commit -m "feat: adicionar Dockerfile, docker-compose e perfil de producao com PostgreSQL"
```

---

## Task 23: README profissional

**Files:**
- Modify: `README.md` (substitui integralmente o README do app de console legado)

**Interfaces:**
- Nenhuma (documentação).

- [ ] **Step 1: Reescrever `README.md`**

```markdown
# Controle de Estoque — API REST

API REST em Spring Boot para controle de estoque: produtos, categorias, coleções, fornecedores, funcionários e movimentações de entrada/saída, com autenticação JWT, controle de concorrência e documentação OpenAPI.

## Stack

- Java 17 · Spring Boot 3.3 · Spring Web · Spring Data JPA · Spring Security 6 (JWT)
- Flyway · H2 (dev/test) · PostgreSQL 16 (produção, via Docker Compose)
- springdoc-openapi (Swagger UI) · Bean Validation · MapStruct · Lombok
- JUnit 5 · Mockito · MockMvc · Maven

## Arquitetura

```
Controller  →  Service (interface + impl)  →  Repository (Spring Data JPA)
     ↓                    ↓
   DTO (record)      Entity (JPA)
     ↓                    ↓
GlobalExceptionHandler (ProblemDetail / RFC 7807)
```

- Injeção de dependência exclusivamente via construtor.
- Toda escrita em service é `@Transactional`; leituras são `@Transactional(readOnly = true)`.
- Regras de negócio de estoque vivem na entidade `Produto` (`registrarEntrada`/`registrarSaida`), nunca no controller.
- `Produto.quantidadeEstoque` é protegido por lock otimista (`@Version`): duas movimentações concorrentes no mesmo produto nunca causam *lost update* — a segunda recebe `409 Conflict` e deve reenviar a operação.
- Erros seguem RFC 7807 (`ProblemDetail`) de forma consistente em toda a API, inclusive falhas de autenticação/autorização geradas pelo Spring Security.

## Como rodar localmente (H2, sem dependências externas)

```bash
mvn spring-boot:run
```

A aplicação sobe em `http://localhost:8080` com banco H2 em memória e dados de seed já carregados (veja credenciais abaixo). Console H2 disponível em `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:estoque`).

## Como rodar com PostgreSQL (Docker Compose)

```bash
docker compose up --build
```

Sobe PostgreSQL 16 + a aplicação no perfil `prod`, executando as mesmas migrations Flyway usadas em desenvolvimento.

## Usuário de seed

| Email | Senha | Papel |
|---|---|---|
| `admin@estoque.com` | `admin123` | `ADMIN` |

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@estoque.com","senha":"admin123"}'
```

A resposta traz o `token` JWT a ser enviado em `Authorization: Bearer <token>` nas demais requisições.

## Documentação interativa

Swagger UI: `http://localhost:8080/swagger-ui.html`
OpenAPI JSON: `http://localhost:8080/v3/api-docs`

## Principais endpoints

| Método | Rota | Papel exigido |
|---|---|---|
| POST | `/api/v1/auth/login` | público |
| GET/POST/PUT/DELETE | `/api/v1/produtos` | leitura: autenticado · escrita: `ADMIN` |
| GET | `/api/v1/produtos/estoque-baixo` | autenticado |
| GET | `/api/v1/produtos/{id}/movimentacoes` | autenticado (kardex) |
| GET/POST/PUT/DELETE | `/api/v1/categorias`, `/api/v1/colecoes` | leitura: autenticado · escrita: `ADMIN` |
| GET/POST/PUT/DELETE | `/api/v1/fornecedores` | leitura: autenticado · escrita: `ADMIN` |
| GET/POST/PUT/DELETE | `/api/v1/funcionarios` | `ADMIN` |
| GET | `/api/v1/funcionarios/me` | autenticado |
| GET/POST | `/api/v1/entradas`, `/api/v1/saidas` | leitura: autenticado · escrita: `ADMIN` ou `OPERADOR` |
| GET | `/api/v1/relatorios/valor-estoque`, `/api/v1/relatorios/estoque-baixo` | autenticado |

## Testes

```bash
mvn clean verify
```

Cobre testes unitários de service (Mockito), testes de integração de controller (MockMvc + H2) e um teste de concorrência real (`SaidaConcorrenciaIT`) que dispara múltiplas saídas simultâneas sobre o mesmo produto e confirma que o estoque nunca fica negativo.

## Estrutura de pastas

```
src/main/java/com/estoque/
├── config/         SecurityConfig, OpenApiConfig
├── controller/      endpoints REST
├── dto/             request/response (records)
├── entity/           entidades JPA
├── enums/            Role, MotivoSaida
├── exception/         hierarquia de exceptions + GlobalExceptionHandler
├── mapper/            MapStruct + mapeador polimórfico de movimentações
├── repository/        Spring Data JPA
├── security/          JWT, UserDetailsService, filtros
└── service/           regras de negócio (interface + impl)
```

## Decisões de design

- **`ProblemDetail` (RFC 7807)** em vez de um corpo de erro ad-hoc: é o padrão nativo do Spring 6/Boot 3 e já é entendido por qualquer client HTTP moderno.
- **Lock otimista em vez de lock pessimista** no estoque: melhor throughput em operações concorrentes; o custo é o cliente eventualmente precisar reenviar uma operação em conflito, tratado explicitamente com `409 Conflict`.
- **Soft delete** em Produto/Fornecedor/Funcionário: preserva o histórico de movimentações mesmo após a "exclusão" (uma saída nunca perde a referência ao produto que a originou).
- **Herança `JOINED`** em `MovimentacaoEstoque`: permite consultar o kardex de um produto com uma única query polimórfica, sem duplicar colunas comuns entre `Entrada` e `Saida`.

## Roadmap

- Pipeline de CI (build + testes a cada push)
- Deploy automatizado (ex: Railway/Render)
- Cache de leitura para relatórios
```

- [ ] **Step 2: Commit**

```bash
git add README.md
git commit -m "docs: reescrever README com arquitetura, setup e documentacao da API"
```

---

## Task 24: Verificação final da suíte completa

**Files:**
- Nenhum arquivo novo — apenas verificação.

**Interfaces:**
- Nenhuma.

- [ ] **Step 1: Rodar a suíte completa de testes**

Run: `mvn clean verify`
Expected: BUILD SUCCESS, todos os testes das Tasks 1–23 passam (unitários, `@DataJpaTest`, `@SpringBootTest`/MockMvc e o teste de concorrência).

- [ ] **Step 2: Subir a aplicação localmente e validar manualmente o fluxo principal**

Run: `mvn spring-boot:run` (em um terminal separado, depois `Ctrl+C` para encerrar)

Validar manualmente com `curl` ou Swagger UI (`http://localhost:8080/swagger-ui.html`):
1. `POST /api/v1/auth/login` com `admin@estoque.com`/`admin123` retorna token.
2. `POST /api/v1/produtos` (com o token) cria um produto.
3. `POST /api/v1/entradas` incrementa o estoque do produto criado.
4. `POST /api/v1/saidas` decrementa o estoque; repetir com quantidade maior que o saldo deve retornar `409`.
5. `GET /api/v1/produtos/{id}/movimentacoes` mostra a entrada e a saída registradas.
6. `GET /api/v1/relatorios/valor-estoque` reflete o valor calculado a partir do custo médio.

- [ ] **Step 3: Commit final (se houver ajustes pontuais feitos durante a validação manual)**

```bash
git status
```

Se tudo já estiver committed ao final da Task 23, nenhuma ação adicional é necessária aqui — este passo existe para capturar qualquer correção de última hora encontrada na validação manual do Step 2.

