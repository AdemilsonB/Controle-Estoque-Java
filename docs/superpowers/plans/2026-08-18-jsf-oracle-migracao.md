# Migração para Spring Tradicional + JSF + Oracle — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Migrar a API REST Spring Boot existente para Spring Framework tradicional (sem autoconfiguração, empacotada como WAR, deployável num Tomcat externo), preservando toda a API REST e os 105 testes atuais, e adicionar duas telas JSF de demonstração (`Produto`) mais um perfil Oracle (via Docker) substituindo o Postgres em ambiente "prod"-equivalente.

**Architecture:** Um único módulo Maven, packaging `war`. `WebAppInitializer` (substitui `@SpringBootApplication`) registra dois contextos servlet: `DispatcherServlet` REST em `/api/*` (controllers inalterados) e `FacesServlet` JSF em `/faces/*` (managed beans CDI via Weld, injetando os `@Service` Spring existentes). Duas `SecurityFilterChain`: a JWT stateless atual para `/api/v1/**`, e uma nova por sessão/form-login para `/faces/**`. `DataSource`/dialect Hibernate/pasta de migrations Flyway selecionados por perfil Spring (`h2`, `test`, `oracle`) via `@Profile`, sem `application.yml`.

**Tech Stack:** Spring Framework 6.1.13 (sem Boot), Spring Security 6.3.3, Hibernate ORM 6.5.3.Final, Jakarta Faces 4.0 (Mojarra/GlassFish) + Weld Servlet 5.1 (CDI), Flyway, H2 + Oracle XE 21 (`gvenzl/oracle-xe`, Docker), Tomcat 10.1, `cargo-maven3-plugin`, Maven.

**Spec:** `docs/superpowers/specs/2026-08-18-jsf-oracle-migracao-design.md`

## Global Constraints

- Nenhuma classe em `entity/`, `dto/`, `mapper/`, `service/`, `repository/`, `controller/`, `exception/` muda de comportamento — só a infraestrutura ao redor (config, bootstrap, empacotamento).
- Toda task termina com `mvn clean verify` verde (ou, nas tasks de infraestrutura pura sem teste novo, `mvn clean compile` bem-sucedido — explicitado por task) antes do commit.
- Sem menção de coautoria de IA em nenhum commit, comentário ou arquivo (constraint herdada do plano original).
- `ddl-auto: validate` (ou equivalente Hibernate `hbm2ddl.auto=validate`) continua obrigatório em todo perfil — é a rede de segurança que pega migration desalinhada com entidade.
- H2 continua sendo o único banco usado pelos testes automatizados — Oracle não tem modo embarcado; isso é uma lacuna documentada, não um TODO.
- Cada arquivo de teste convertido de `@SpringBootTest`/`@DataJpaTest` mantém 100% do corpo de teste (métodos `@Test`, asserções, fixtures) — só o bloco de anotações/imports do topo muda.

---

## Task 1: `pom.xml` sem Spring Boot

**Files:**
- Modify: `pom.xml`

**Interfaces:**
- Produces: projeto compila (`mvn clean compile`) como WAR, com todas as dependências que hoje vêm dos starters do Boot declaradas explicitamente, mais JSF (Jakarta Faces 4.0) e Weld (CDI, necessário porque Jakarta Faces 4.0 removeu o `@ManagedBean`/XML managed-bean não-CDI — só resta `@Named` via CDI).

- [ ] **Step 1: Reescrever `pom.xml`**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.estoque</groupId>
    <artifactId>controle-estoque</artifactId>
    <version>2.0.0</version>
    <packaging>war</packaging>
    <name>controle-estoque</name>
    <description>API REST + JSF de controle de estoque (Spring tradicional)</description>

    <properties>
        <maven.compiler.source>17</maven.compiler.source>
        <maven.compiler.target>17</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <spring.version>6.1.13</spring.version>
        <spring-security.version>6.3.3</spring-security.version>
        <hibernate.version>6.5.3.Final</hibernate.version>
        <mapstruct.version>1.6.2</mapstruct.version>
        <jjwt.version>0.12.6</jjwt.version>
        <springdoc.version>2.6.0</springdoc.version>
        <flyway.version>10.15.0</flyway.version>
        <faces.version>4.0.7</faces.version>
        <weld.version>5.1.2.Final</weld.version>
    </properties>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework</groupId>
                <artifactId>spring-framework-bom</artifactId>
                <version>${spring.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
            <dependency>
                <groupId>org.springframework.security</groupId>
                <artifactId>spring-security-bom</artifactId>
                <version>${spring-security.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <dependencies>
        <!-- Spring core / MVC / ORM / TX -->
        <dependency><groupId>org.springframework</groupId><artifactId>spring-context</artifactId></dependency>
        <dependency><groupId>org.springframework</groupId><artifactId>spring-webmvc</artifactId></dependency>
        <dependency><groupId>org.springframework</groupId><artifactId>spring-orm</artifactId></dependency>
        <dependency><groupId>org.springframework</groupId><artifactId>spring-tx</artifactId></dependency>
        <dependency><groupId>org.springframework.data</groupId><artifactId>spring-data-jpa</artifactId><version>3.3.4</version></dependency>

        <!-- Spring Security -->
        <dependency><groupId>org.springframework.security</groupId><artifactId>spring-security-web</artifactId></dependency>
        <dependency><groupId>org.springframework.security</groupId><artifactId>spring-security-config</artifactId></dependency>

        <!-- Bean Validation -->
        <dependency><groupId>org.hibernate.validator</groupId><artifactId>hibernate-validator</artifactId><version>8.0.1.Final</version></dependency>
        <dependency><groupId>org.glassfish</groupId><artifactId>jakarta.el</artifactId><version>4.0.2</version></dependency>

        <!-- Servlet / JSON -->
        <dependency>
            <groupId>jakarta.servlet</groupId><artifactId>jakarta.servlet-api</artifactId>
            <version>6.0.0</version><scope>provided</scope>
        </dependency>
        <dependency><groupId>com.fasterxml.jackson.core</groupId><artifactId>jackson-databind</artifactId><version>2.17.2</version></dependency>
        <dependency><groupId>com.fasterxml.jackson.datatype</groupId><artifactId>jackson-datatype-jsr310</artifactId><version>2.17.2</version></dependency>

        <!-- Persistência -->
        <dependency><groupId>org.hibernate.orm</groupId><artifactId>hibernate-core</artifactId><version>${hibernate.version}</version></dependency>
        <dependency><groupId>com.zaxxer</groupId><artifactId>HikariCP</artifactId><version>5.1.0</version></dependency>
        <dependency><groupId>org.flywaydb</groupId><artifactId>flyway-core</artifactId><version>${flyway.version}</version></dependency>
        <dependency><groupId>org.flywaydb</groupId><artifactId>flyway-database-oracle</artifactId><version>${flyway.version}</version></dependency>
        <dependency><groupId>com.h2database</groupId><artifactId>h2</artifactId><scope>runtime</scope></dependency>
        <dependency><groupId>com.oracle.database.jdbc</groupId><artifactId>ojdbc11</artifactId><version>23.5.0.24.07</version><scope>runtime</scope></dependency>

        <!-- OpenAPI -->
        <dependency>
            <groupId>org.springdoc</groupId><artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
            <version>${springdoc.version}</version>
        </dependency>

        <!-- MapStruct / Lombok / JJWT -->
        <dependency><groupId>org.mapstruct</groupId><artifactId>mapstruct</artifactId><version>${mapstruct.version}</version></dependency>
        <dependency><groupId>io.jsonwebtoken</groupId><artifactId>jjwt-api</artifactId><version>${jjwt.version}</version></dependency>
        <dependency><groupId>io.jsonwebtoken</groupId><artifactId>jjwt-impl</artifactId><version>${jjwt.version}</version><scope>runtime</scope></dependency>
        <dependency><groupId>io.jsonwebtoken</groupId><artifactId>jjwt-jackson</artifactId><version>${jjwt.version}</version><scope>runtime</scope></dependency>
        <dependency><groupId>org.projectlombok</groupId><artifactId>lombok</artifactId><optional>true</optional></dependency>

        <!-- JSF + CDI (Weld) -->
        <dependency><groupId>org.glassfish</groupId><artifactId>jakarta.faces</artifactId><version>${faces.version}</version></dependency>
        <dependency><groupId>org.jboss.weld.servlet</groupId><artifactId>weld-servlet-shaded</artifactId><version>${weld.version}</version></dependency>

        <!-- Testes -->
        <dependency><groupId>org.junit.jupiter</groupId><artifactId>junit-jupiter</artifactId><version>5.10.3</version><scope>test</scope></dependency>
        <dependency><groupId>org.assertj</groupId><artifactId>assertj-core</artifactId><version>3.26.3</version><scope>test</scope></dependency>
        <dependency><groupId>org.mockito</groupId><artifactId>mockito-junit-jupiter</artifactId><version>5.13.0</version><scope>test</scope></dependency>
        <dependency><groupId>org.springframework</groupId><artifactId>spring-test</artifactId><scope>test</scope></dependency>
        <dependency><groupId>org.springframework.security</groupId><artifactId>spring-security-test</artifactId><scope>test</scope></dependency>
    </dependencies>

    <build>
        <finalName>controle-estoque</finalName>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-war-plugin</artifactId>
                <version>3.4.0</version>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.13.0</version>
                <configuration>
                    <annotationProcessorPaths>
                        <path><groupId>org.projectlombok</groupId><artifactId>lombok</artifactId><version>1.18.34</version></path>
                        <path><groupId>org.mapstruct</groupId><artifactId>mapstruct-processor</artifactId><version>${mapstruct.version}</version></path>
                        <path><groupId>org.projectlombok</groupId><artifactId>lombok-mapstruct-binding</artifactId><version>0.2.0</version></path>
                    </annotationProcessorPaths>
                </configuration>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-surefire-plugin</artifactId>
                <version>3.2.5</version>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 2: Confirmar que o Maven resolve as dependências**

Run: `mvn -q dependency:resolve`
Expected: BUILD SUCCESS, sem erro de artefato não encontrado. Se `ojdbc11` ou `flyway-database-oracle` falharem por causa de repositório, adicionar o Maven Central padrão já resolve (são publicados lá).

Nesta task o projeto ainda não compila (as classes referenciam `EstoqueApplication`/`application.yml`, que só somem na Task 4) — o objetivo aqui é só validar a árvore de dependências.

- [ ] **Step 3: Commit**

```bash
git add pom.xml
git commit -m "build: remover Spring Boot do pom.xml, empacotar como WAR"
```

---

## Task 2: `PersistenceConfig` — DataSource, JPA, TransactionManager

**Files:**
- Create: `src/main/java/com/estoque/config/PersistenceConfig.java`
- Create: `src/main/resources/app.properties`
- Create: `src/main/resources/app-test.properties`
- Delete: `src/main/resources/application.yml`
- Delete: `src/main/resources/application-test.yml`
- Delete: `src/main/resources/application-prod.yml`

**Interfaces:**
- Consumes: nenhum.
- Produces: bean `DataSource` (um por perfil: `h2`, `test`, `oracle` — Oracle entra na Task 11), bean `LocalContainerEntityManagerFactoryBean` (nome `entityManagerFactory`), bean `PlatformTransactionManager` (nome `transactionManager`), `@EnableTransactionManagement`, `@EnableJpaRepositories(basePackages = "com.estoque.repository")`.

- [ ] **Step 1: Criar `app.properties` (perfil `h2`, equivalente ao `application.yml` atual)**

```properties
db.url=jdbc:h2:mem:estoque;DB_CLOSE_DELAY=-1
db.username=sa
db.password=
db.driver=org.h2.Driver
hibernate.dialect=org.hibernate.dialect.H2Dialect
flyway.locations=classpath:db/migration/h2

jwt.secret=MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDEyMzQ1Njc4OTAxMjM0NTY3ODk=
jwt.expiration-ms=3600000
```

- [ ] **Step 2: Criar `app-test.properties`**

```properties
db.url=jdbc:h2:mem:estoque-test;DB_CLOSE_DELAY=-1
db.username=sa
db.password=
db.driver=org.h2.Driver
hibernate.dialect=org.hibernate.dialect.H2Dialect
flyway.locations=classpath:db/migration/h2

jwt.secret=MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDEyMzQ1Njc4OTAxMjM0NTY3ODk=
jwt.expiration-ms=3600000
```

- [ ] **Step 3: Apagar os `.yml`**

```bash
git rm src/main/resources/application.yml src/main/resources/application-test.yml src/main/resources/application-prod.yml
```

- [ ] **Step 4: Criar `PersistenceConfig`**

```java
package com.estoque.config;

import com.zaxxer.hikari.HikariDataSource;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.Properties;

@Configuration
@ComponentScan(basePackages = {"com.estoque.repository", "com.estoque.service", "com.estoque.security", "com.estoque.mapper"})
@EnableJpaRepositories(basePackages = "com.estoque.repository")
@EnableJpaAuditing
@EnableTransactionManagement
@PropertySource("classpath:app-${spring.profiles.active:h2}.properties")
public class PersistenceConfig {

    @Value("${db.url}") private String dbUrl;
    @Value("${db.username}") private String dbUsername;
    @Value("${db.password}") private String dbPassword;
    @Value("${db.driver}") private String dbDriver;
    @Value("${hibernate.dialect}") private String hibernateDialect;

    @Bean
    public DataSource dataSource() {
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl(dbUrl);
        ds.setUsername(dbUsername);
        ds.setPassword(dbPassword);
        ds.setDriverClassName(dbDriver);
        return ds;
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
        LocalContainerEntityManagerFactoryBean emf = new LocalContainerEntityManagerFactoryBean();
        emf.setDataSource(dataSource);
        emf.setPackagesToScan("com.estoque.entity");
        emf.setPersistenceProvider(new HibernatePersistenceProvider());

        JpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        emf.setJpaVendorAdapter(vendorAdapter);

        Properties props = new Properties();
        props.setProperty("hibernate.dialect", hibernateDialect);
        props.setProperty("hibernate.hbm2ddl.auto", "validate");
        props.setProperty("hibernate.format_sql", "true");
        // Equivalente a spring.jpa.open-in-view: false no Boot: sem OpenEntityManagerInViewFilter
        // registrado no WebAppInitializer (Task 4), o comportamento já é o mesmo por padrão.
        emf.setJpaProperties(props);

        return emf;
    }

    @Bean
    public PlatformTransactionManager transactionManager(LocalContainerEntityManagerFactoryBean emf) {
        return new JpaTransactionManager(emf.getObject());
    }
}
```

Note: `@Value("${jwt.secret}")`/`${jwt.expiration-ms}` usados em `JwtService`/`AuthController` continuam funcionando sem alteração nenhuma nessas classes — resolvidos pelo `PropertySourcesPlaceholderConfigurer` que a Task 4 registra a partir do mesmo `app.properties`.

- [ ] **Step 5: Rodar compilação**

Run: `mvn -q clean compile`
Expected: ainda FALHA — `EstoqueApplication` referencia `@SpringBootApplication`, que não existe mais no classpath. Isso é esperado; resolvido na Task 4.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/estoque/config/PersistenceConfig.java src/main/resources/app.properties src/main/resources/app-test.properties
git rm src/main/resources/application.yml src/main/resources/application-test.yml src/main/resources/application-prod.yml
git commit -m "feat: adicionar PersistenceConfig manual, remover application.yml"
```

---

## Task 3: `FlywayConfig`

**Files:**
- Create: `src/main/java/com/estoque/config/FlywayConfig.java`
- Modify: `src/main/resources/app.properties`
- Modify: `src/main/resources/app-test.properties`
- Move: `src/main/resources/db/migration/*.sql` → `src/main/resources/db/migration/h2/*.sql`

**Interfaces:**
- Consumes: `DataSource` (Task 2).
- Produces: migração executada automaticamente na subida do contexto, antes do `EntityManagerFactory` ser inicializado.

- [ ] **Step 1: Mover as migrations existentes pra pasta `h2/`**

```bash
mkdir -p src/main/resources/db/migration/h2
git mv src/main/resources/db/migration/V1__schema_categoria_colecao.sql src/main/resources/db/migration/h2/
git mv src/main/resources/db/migration/V2__schema_fornecedor.sql src/main/resources/db/migration/h2/
git mv src/main/resources/db/migration/V3__schema_funcionario.sql src/main/resources/db/migration/h2/
git mv src/main/resources/db/migration/V4__schema_produto.sql src/main/resources/db/migration/h2/
git mv src/main/resources/db/migration/V5__schema_movimentacao.sql src/main/resources/db/migration/h2/
git mv src/main/resources/db/migration/V6__seed.sql src/main/resources/db/migration/h2/
git mv src/main/resources/db/migration/V7__schema_auditoria.sql src/main/resources/db/migration/h2/
```

- [ ] **Step 2: Criar `FlywayConfig`**

```java
package com.estoque.config;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.io.support.PropertySourcesPlaceholderConfigurer;

import javax.sql.DataSource;

@Configuration
public class FlywayConfig {

    @Value("${flyway.locations}")
    private String flywayLocations;

    // static: precisa rodar antes de qualquer @Value/${...} ser resolvido no restante do contexto.
    @Bean
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }

    @Bean
    @DependsOn("propertySourcesPlaceholderConfigurer")
    public Flyway flyway(DataSource dataSource) {
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations(flywayLocations)
                .load();
        flyway.migrate();
        return flyway;
    }
}
```

- [ ] **Step 3: Garantir ordem de inicialização (Flyway antes do EntityManagerFactory)**

Em `PersistenceConfig.entityManagerFactory(...)`, adicionar o parâmetro `Flyway flyway` (não usado no corpo, só como dependência de ordem):

```java
    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource, org.flywaydb.core.Flyway flyway) {
```

- [ ] **Step 4: Adicionar `flyway.locations` já estava em `app.properties`/`app-test.properties` (Task 2, Steps 1-2) — confirmar valor**

Ambos os arquivos já têm `flyway.locations=classpath:db/migration/h2` desde a Task 2. Nenhuma mudança adicional aqui.

- [ ] **Step 5: Rodar compilação**

Run: `mvn -q clean compile`
Expected: ainda FALHA (mesma razão da Task 2 — `EstoqueApplication` continua no classpath até a Task 4). Confirmar que o erro é exclusivamente sobre `EstoqueApplication`/Boot, não sobre `FlywayConfig`/`PersistenceConfig`.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/estoque/config/FlywayConfig.java src/main/java/com/estoque/config/PersistenceConfig.java
git add src/main/resources/db/migration/h2
git commit -m "feat: mover migrations para pasta h2/, adicionar FlywayConfig manual"
```

---

## Task 4: `WebAppInitializer` — bootstrap sem Boot, REST funcionando

**Files:**
- Create: `src/main/java/com/estoque/config/WebAppInitializer.java`
- Modify: `src/main/java/com/estoque/config/WebConfig.java` (conteúdo totalmente substituído — o `WebConfig` atual só tem `@EnableSpringDataWebSupport`)
- Delete: `src/main/java/com/estoque/EstoqueApplication.java`
- Delete: `src/test/java/com/estoque/EstoqueApplicationTests.java`

**Interfaces:**
- Consumes: `PersistenceConfig`, `FlywayConfig` (Tasks 2-3).
- Produces: aplicação sobe num Tomcat externo respondendo em `/api/v1/**` exatamente como respondia via `mvn spring-boot:run` antes da migração.

- [ ] **Step 1: Apagar `EstoqueApplication` e seu teste**

```bash
git rm src/main/java/com/estoque/EstoqueApplication.java
git rm src/test/java/com/estoque/EstoqueApplicationTests.java
```

- [ ] **Step 2: Substituir `WebConfig` (Spring MVC puro, sem `@EnableSpringDataWebSupport` do Boot)**

```java
package com.estoque.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

import static org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO;

@Configuration
@EnableWebMvc
@EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO)
@ComponentScan(basePackages = {"com.estoque.controller", "com.estoque.exception"})
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.add(new MappingJackson2HttpMessageConverter());
    }
}
```

- [ ] **Step 3: Criar `WebAppInitializer`**

```java
package com.estoque.config;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRegistration;
import org.springframework.web.WebApplicationInitializer;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

public class WebAppInitializer implements WebApplicationInitializer {

    @Override
    public void onStartup(ServletContext servletContext) throws ServletException {
        // Perfil ativo: variável de ambiente SPRING_PROFILES_ACTIVE (h2 por padrão em dev local).
        String activeProfile = System.getenv().getOrDefault("SPRING_PROFILES_ACTIVE", "h2");
        servletContext.setInitParameter("spring.profiles.active", activeProfile);

        // Contexto raiz: persistência, serviços, segurança — compartilhado entre REST e JSF.
        AnnotationConfigWebApplicationContext rootContext = new AnnotationConfigWebApplicationContext();
        rootContext.getEnvironment().setActiveProfiles(activeProfile);
        rootContext.register(PersistenceConfig.class, FlywayConfig.class, SecurityConfig.class, OpenApiConfig.class);
        servletContext.addListener(new ContextLoaderListener(rootContext));

        // Contexto filho REST: só os @RestController, herda os beans do contexto raiz.
        AnnotationConfigWebApplicationContext restContext = new AnnotationConfigWebApplicationContext();
        restContext.register(WebConfig.class);
        ServletRegistration.Dynamic dispatcher = servletContext.addServlet("dispatcher", new DispatcherServlet(restContext));
        dispatcher.setLoadOnStartup(1);
        dispatcher.addMapping("/api/*");
    }
}
```

- [ ] **Step 4: Rodar compilação**

Run: `mvn -q clean compile`
Expected: BUILD SUCCESS. É o primeiro sucesso desde a Task 1 — confirma que todo o código principal (`entity`, `dto`, `mapper`, `service`, `repository`, `controller`, `exception`, `security`) compila sem nenhuma dependência de Boot.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/estoque/config/WebAppInitializer.java src/main/java/com/estoque/config/WebConfig.java
git rm src/main/java/com/estoque/EstoqueApplication.java src/test/java/com/estoque/EstoqueApplicationTests.java
git commit -m "feat: adicionar WebAppInitializer, remover EstoqueApplication (Boot)"
```

---

## Task 5: Migrar os testes de `@DataJpaTest`/`@SpringBootTest` para `@ContextConfiguration`

**Files:**
- Create: `src/test/java/com/estoque/config/TestPersistenceConfig.java`
- Modify: os 6 arquivos `@DataJpaTest` e os 14 arquivos `@SpringBootTest` restantes listados abaixo (`EstoqueApplicationTests` já foi apagado na Task 4).

**Interfaces:**
- Consumes: `PersistenceConfig`, `FlywayConfig`, `WebConfig`, `SecurityConfig` (Tasks 2-4).
- Produces: os 105 testes existentes voltam a rodar e passar sem `spring-boot-starter-test`.

- [ ] **Step 1: Criar `TestPersistenceConfig` (ativa o perfil `test`)**

```java
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
```

Esse arquivo existe só como ponto de documentação — o mecanismo real de ativação de perfil em teste é a anotação `@ActiveProfiles("test")` do Spring Test, que continua funcionando sem Boot (é `spring-test`, não `spring-boot-test`).

- [ ] **Step 2: Converter os 6 testes `@DataJpaTest` — mesmo padrão para todos**

Para cada um dos arquivos abaixo, trocar exatamente o bloco de imports/anotações no topo. O corpo da classe (campos `@Autowired`, métodos `@Test`, asserções) **não muda uma linha**.

**Antes** (em todos os 6):
```java
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class NomeDoTeste {
```

**Depois** (em todos os 6):
```java
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Transactional;
import com.estoque.config.PersistenceConfig;
import com.estoque.config.FlywayConfig;

@SpringJUnitConfig(classes = {PersistenceConfig.class, FlywayConfig.class})
@ActiveProfiles("test")
@Transactional
class NomeDoTeste {
```

Aplicar essa troca exata nos 6 arquivos:
- `src/test/java/com/estoque/repository/CategoriaRepositoryTest.java`
- `src/test/java/com/estoque/repository/ColecaoRepositoryTest.java`
- `src/test/java/com/estoque/repository/FornecedorRepositoryTest.java`
- `src/test/java/com/estoque/repository/FuncionarioRepositoryTest.java`
- `src/test/java/com/estoque/repository/MovimentacaoEstoqueRepositoryTest.java`
- `src/test/java/com/estoque/repository/ProdutoRepositoryTest.java`

- [ ] **Step 3: Rodar os 6 testes de repositório**

Run: `mvn -q -Dtest=CategoriaRepositoryTest,ColecaoRepositoryTest,FornecedorRepositoryTest,FuncionarioRepositoryTest,MovimentacaoEstoqueRepositoryTest,ProdutoRepositoryTest test`
Expected: PASS, 6/6.

- [ ] **Step 4: Commit**

```bash
git add src/test/java/com/estoque/config/TestPersistenceConfig.java src/test/java/com/estoque/repository/
git commit -m "test: migrar testes de repositorio de @DataJpaTest para @SpringJUnitConfig"
```

- [ ] **Step 5: Converter `SaidaConcorrenciaIT` e `AuditoriaSaidaNegadaIT` (padrão `@SpringBootTest` de serviço, sem MockMvc)**

**Antes** (nos 2 arquivos):
```java
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class NomeDoTeste {
```

**Depois** (nos 2 arquivos):
```java
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import com.estoque.config.PersistenceConfig;
import com.estoque.config.FlywayConfig;

@SpringJUnitConfig(classes = {PersistenceConfig.class, FlywayConfig.class})
@ActiveProfiles("test")
class NomeDoTeste {
```

Aplicar em:
- `src/test/java/com/estoque/service/SaidaConcorrenciaIT.java`
- `src/test/java/com/estoque/service/AuditoriaSaidaNegadaIT.java`
- `src/test/java/com/estoque/SeedDataIntegrationTest.java`

- [ ] **Step 6: Rodar os 3 testes**

Run: `mvn -q -Dtest=SaidaConcorrenciaIT,AuditoriaSaidaNegadaIT,SeedDataIntegrationTest test`
Expected: PASS, 3/3.

- [ ] **Step 7: Commit**

```bash
git add src/test/java/com/estoque/service/SaidaConcorrenciaIT.java src/test/java/com/estoque/service/AuditoriaSaidaNegadaIT.java src/test/java/com/estoque/SeedDataIntegrationTest.java
git commit -m "test: migrar SaidaConcorrenciaIT, AuditoriaSaidaNegadaIT e SeedDataIntegrationTest"
```

- [ ] **Step 8: Converter os 8 testes de controller + `OpenApiConfigIT` (padrão `@SpringBootTest` + MockMvc)**

Esses precisam de `@WebAppConfiguration` e construir o `MockMvc` manualmente a partir do `WebApplicationContext`, já que `@AutoConfigureMockMvc` não existe fora do Boot.

**Antes** (nos 9 arquivos — exemplo `AuthControllerIT`):
```java
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerIT {

    @Autowired private MockMvc mockMvc;
```

**Depois**:
```java
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import com.estoque.config.PersistenceConfig;
import com.estoque.config.FlywayConfig;
import com.estoque.config.WebConfig;
import com.estoque.config.SecurityConfig;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

@SpringJUnitConfig(classes = {PersistenceConfig.class, FlywayConfig.class, WebConfig.class, SecurityConfig.class})
@WebAppConfiguration
@ActiveProfiles("test")
class AuthControllerIT {

    @Autowired private WebApplicationContext webApplicationContext;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }
```

Aplicar esse mesmo padrão (trocando só o nome da classe) em:
- `src/test/java/com/estoque/controller/AuthControllerIT.java`
- `src/test/java/com/estoque/controller/CategoriaControllerIT.java`
- `src/test/java/com/estoque/controller/ColecaoControllerIT.java`
- `src/test/java/com/estoque/controller/EntradaControllerIT.java`
- `src/test/java/com/estoque/controller/FornecedorControllerIT.java`
- `src/test/java/com/estoque/controller/FuncionarioControllerIT.java`
- `src/test/java/com/estoque/controller/ProdutoControllerIT.java`
- `src/test/java/com/estoque/controller/ProdutoKardexIT.java`
- `src/test/java/com/estoque/controller/RelatorioControllerIT.java`
- `src/test/java/com/estoque/controller/SaidaControllerIT.java`
- `src/test/java/com/estoque/config/OpenApiConfigIT.java` (usa MockMvc contra `/v3/api-docs` e `/swagger-ui.html` — aplicar o mesmo padrão `@WebAppConfiguration` + `MockMvcBuilders.webAppContextSetup(...)` acima, com `@SpringJUnitConfig(classes = {PersistenceConfig.class, FlywayConfig.class, WebConfig.class, SecurityConfig.class})`, já que `/swagger-ui.html` é uma rota pública declarada em `SecurityConfig.ROTAS_PUBLICAS` e o teste precisa da security chain carregada para validar isso).

- [ ] **Step 9: Rodar os 10 testes de controller de negócio primeiro, isolados de `OpenApiConfigIT`**

Run: `mvn -q -Dtest=AuthControllerIT,CategoriaControllerIT,ColecaoControllerIT,EntradaControllerIT,FornecedorControllerIT,FuncionarioControllerIT,ProdutoControllerIT,ProdutoKardexIT,RelatorioControllerIT,SaidaControllerIT test`
Expected: PASS, 10/10.

- [ ] **Step 10: Rodar `OpenApiConfigIT` separadamente — risco técnico conhecido**

Run: `mvn -q -Dtest=OpenApiConfigIT test`

`springdoc-openapi-starter-webmvc-ui` registra `/v3/api-docs` e `/swagger-ui.html` através de classes de configuração (`org.springdoc.core.configuration.SpringDocConfiguration`, `org.springdoc.webmvc.core.configuration.SpringDocWebMvcConfiguration`, `org.springdoc.webmvc.ui.SwaggerConfig`) que hoje são carregadas pela autoconfiguração do Boot — `OpenApiConfig` sozinho (só o bean `OpenAPI`) pode não ser suficiente sem o Boot.

**Se passar:** ótimo, springdoc resolveu via classpath scanning; seguir pro Step 11.

**Se falhar** (404 em `/v3/api-docs` ou `/swagger-ui.html`): adicionar a essas três classes ao `@Import` de `WebConfig` (Task 4, Step 2):

```java
@Import({
        org.springdoc.core.configuration.SpringDocConfiguration.class,
        org.springdoc.webmvc.core.configuration.SpringDocWebMvcConfiguration.class,
        org.springdoc.webmvc.ui.SwaggerConfig.class
})
```

Se os nomes de pacote acima não baterem com o que `springdoc-openapi-starter-webmvc-ui:2.6.0` realmente expõe (confirmar com `mvn dependency:tree` + inspecionar o jar em `~/.m2/repository/org/springdoc/`), localizar as classes `@Configuration` do artefato e importar essas — o objetivo é só garantir que os `@Bean` de `@RestController`/`@Bean` de rota do springdoc entrem no `ApplicationContext` do `WebConfig`, já que sem Boot elas não são descobertas automaticamente.

- [ ] **Step 11: Commit**

```bash
git add src/test/java/com/estoque/controller/ src/test/java/com/estoque/config/OpenApiConfigIT.java
git commit -m "test: migrar testes de controller de @SpringBootTest para @SpringJUnitConfig + WebAppConfiguration"
```

- [ ] **Step 12: Rodar a suíte completa**

Run: `mvn clean verify`
Expected: BUILD SUCCESS, 105 testes, 0 falhas — os mesmos 105 de antes da migração, agora sem nenhuma dependência de `spring-boot-starter-test`.

---

## Task 6: `SecurityConfig` sem autoconfiguração + registro no Tomcat

**Files:**
- Modify: `src/main/java/com/estoque/config/SecurityConfig.java`
- Create: `src/main/java/com/estoque/config/SecurityWebAppInitializer.java`

**Interfaces:**
- Consumes: `JwtAuthFilter`, `RestAccessDeniedHandler`, `RestAuthenticationEntryPoint`, `UserDetailsServiceImpl` (inalterados).
- Produces: o filtro `springSecurityFilterChain` registrado no `web.xml` implícito do Tomcat, na frente do `DispatcherServlet`.

- [ ] **Step 1: Ajustar `SecurityConfig` — trocar `Environment.matchesProfiles("prod")` por checagem do novo perfil `oracle`**

O perfil que hoje se chama `prod` no Boot vira `oracle` no novo esquema de perfis (Task 2/11). Editar a linha:

```java
boolean producao = environment.matchesProfiles("prod");
```

para:

```java
boolean producao = environment.matchesProfiles("oracle");
```

Nenhuma outra linha de `SecurityConfig` muda — toda a lógica de rotas públicas, JWT stateless e `H2_CONSOLE` continua igual.

- [ ] **Step 2: Criar `SecurityWebAppInitializer`**

```java
package com.estoque.config;

import org.springframework.security.web.context.AbstractSecurityWebApplicationInitializer;

public class SecurityWebAppInitializer extends AbstractSecurityWebApplicationInitializer {
}
```

Essa classe, por herdar de `AbstractSecurityWebApplicationInitializer`, registra automaticamente um `DelegatingFilterProxy` chamado `springSecurityFilterChain` no `ServletContext`, mapeado em `/*`, delegando para o bean `SecurityFilterChain` que `SecurityConfig` já declara. É o equivalente exato ao que `spring-boot-starter-security` fazia via autoconfiguração.

- [ ] **Step 3: Rodar a suíte completa**

Run: `mvn clean verify`
Expected: BUILD SUCCESS, 105 testes, 0 falhas — os testes de segurança (401/403, JWT) continuam passando porque `SecurityConfig` está registrada tanto no contexto de teste (`@SpringJUnitConfig`, Task 5) quanto, agora, no bootstrap real via `SecurityWebAppInitializer`.

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/estoque/config/SecurityConfig.java src/main/java/com/estoque/config/SecurityWebAppInitializer.java
git commit -m "feat: registrar SecurityFilterChain via AbstractSecurityWebApplicationInitializer"
```

---

## Task 7: Deploy local em Tomcat — primeira verificação manual da API REST sem Boot

**Files:**
- Modify: `pom.xml` (plugin `cargo-maven3-plugin`)

**Interfaces:**
- Consumes: WAR gerado pela Task 6.
- Produces: `mvn cargo:run` sobe um Tomcat 10.1 local com o WAR deployado, servindo a API REST em `http://localhost:8080/controle-estoque/api/v1/**`.

- [ ] **Step 1: Adicionar o `cargo-maven3-plugin` ao `pom.xml`**

Dentro de `<build><plugins>`, adicionar:

```xml
<plugin>
    <groupId>org.codehaus.cargo</groupId>
    <artifactId>cargo-maven3-plugin</artifactId>
    <version>1.10.13</version>
    <configuration>
        <container>
            <containerId>tomcat10x</containerId>
            <type>embedded</type>
            <artifactInstaller>
                <groupId>org.apache.tomcat</groupId>
                <artifactId>tomcat</artifactId>
                <version>10.1.28</version>
            </artifactInstaller>
        </container>
        <configuration>
            <properties>
                <cargo.servlet.port>8080</cargo.servlet.port>
            </properties>
        </configuration>
    </configuration>
</plugin>
```

Usar `type=embedded` (em vez de `installed`) evita depender de um Tomcat baixado manualmente pelo desenvolvedor — o Cargo baixa o Tomcat embarcado via Maven automaticamente na primeira execução.

- [ ] **Step 2: Subir a aplicação**

Run: `mvn clean package cargo:run` (em terminal separado; `Ctrl+C` pra encerrar)
Expected: log do Tomcat embarcado subindo, sem stacktrace de erro, aplicação respondendo.

- [ ] **Step 3: Validar manualmente com curl**

```bash
curl -X POST http://localhost:8080/controle-estoque/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@estoque.com","senha":"admin123"}'
```

Expected: `200`, corpo com `token` JWT — o mesmo comportamento de antes da migração, agora servido por um WAR num Tomcat externo em vez de um jar com Tomcat embarcado do Boot.

- [ ] **Step 4: Encerrar o Tomcat, commit**

```bash
git add pom.xml
git commit -m "build: adicionar cargo-maven3-plugin para rodar a aplicacao em Tomcat local"
```

Neste ponto a API REST está 100% migrada e verificada — as próximas tasks só adicionam superfície nova (JSF, Oracle) sobre uma base já estável.

---

## Task 8: Dependências JSF/CDI + registro do `FacesServlet`

**Files:**
- Create: `src/main/webapp/WEB-INF/beans.xml`
- Create: `src/main/webapp/WEB-INF/faces-config.xml`
- Modify: `src/main/java/com/estoque/config/WebAppInitializer.java`

**Interfaces:**
- Consumes: nenhum.
- Produces: `FacesServlet` respondendo em `/faces/*`, com CDI (Weld) ativo — pré-requisito para os managed beans `@Named`/`@ViewScoped` das próximas tasks (Jakarta Faces 4.0 removeu o `@ManagedBean` não-CDI; `@Named` exige um bean manager CDI, que o Tomcat não traz por padrão).

- [ ] **Step 1: Criar `WEB-INF/beans.xml` (marcador de ativação do CDI)**

```bash
mkdir -p src/main/webapp/WEB-INF
```

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="https://jakarta.ee/xml/ns/jakartaee"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xsi:schemaLocation="https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/beans_4_0.xsd"
       bean-discovery-mode="annotated">
</beans>
```

- [ ] **Step 2: Criar `WEB-INF/faces-config.xml` (mínimo, sem navigation rules — JSF 2.2+ resolve por convenção)**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<faces-config xmlns="https://jakarta.ee/xml/ns/jakartaee"
              xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
              xsi:schemaLocation="https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/web-facesconfig_4_0.xsd"
              version="4.0">
</faces-config>
```

- [ ] **Step 3: Registrar o listener do Weld e o `FacesServlet` em `WebAppInitializer`**

Adicionar ao método `onStartup`, depois do registro do `dispatcher` REST:

```java
        // CDI (Weld) — pré-requisito dos managed beans JSF (@Named) das próximas tasks.
        servletContext.addListener("org.jboss.weld.environment.servlet.Listener");

        // Contexto filho JSF: FacesServlet exige um Listener próprio (com.sun.faces.config.ConfigureListener
        // via jakarta.faces.webapp.FacesServlet, auto-registrado pelo container de Faces).
        ServletRegistration.Dynamic facesServlet = servletContext.addServlet(
                "facesServlet", "jakarta.faces.webapp.FacesServlet");
        facesServlet.setLoadOnStartup(1);
        facesServlet.addMapping("/faces/*");
```

O import `jakarta.servlet.ServletRegistration` já está presente do Step 3 da Task 4.

- [ ] **Step 4: Rodar a suíte completa (nenhum teste novo ainda — só confirmar que nada quebrou)**

Run: `mvn clean verify`
Expected: BUILD SUCCESS, 105 testes, 0 falhas.

- [ ] **Step 5: Verificação manual — o Weld sobe sem erro**

Run: `mvn clean package cargo:run`
Expected: no log do Tomcat, nenhuma exceção `org.jboss.weld.exceptions.DeploymentException`. Se aparecer erro de versão incompatível entre Weld 5.1.2.Final e Jakarta Faces 4.0.7, verificar `mvn dependency:tree` por conflito de versão de `jakarta.enterprise:jakarta.enterprise.cdi-api` (Weld e Mojarra podem trazer versões transitivas diferentes) e alinhar manualmente via `<dependencyManagement>`.

- [ ] **Step 6: Commit**

```bash
git add src/main/webapp/WEB-INF/beans.xml src/main/webapp/WEB-INF/faces-config.xml src/main/java/com/estoque/config/WebAppInitializer.java
git commit -m "feat: registrar FacesServlet e CDI (Weld) para managed beans JSF"
```

---

## Task 9: Tela JSF — listagem de produtos (`ProdutoListBean`)

**Files:**
- Create: `src/main/java/com/estoque/jsf/ProdutoListBean.java`
- Create: `src/main/webapp/produtos.xhtml`
- Test: `src/test/java/com/estoque/jsf/ProdutoListBeanTest.java`

**Interfaces:**
- Consumes: `ProdutoService.listar(Pageable)` (já existe, inalterado).
- Produces: `ProdutoListBean` com `List<ProdutoResponse> getProdutos()`, `int getPaginaAtual()`, `void proximaPagina()`, `void paginaAnterior()`, `boolean isTemProximaPagina()`.

- [ ] **Step 1: Escrever o teste (falha: classe não existe)**

```java
package com.estoque.jsf;

import com.estoque.dto.response.ProdutoResponse;
import com.estoque.service.ProdutoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProdutoListBeanTest {

    @Mock private ProdutoService produtoService;

    @Test
    void deveCarregarPrimeiraPaginaAoIniciar() {
        ProdutoResponse produto = new ProdutoResponse(1L, "SKU-1", "Camiseta", null, "Roupas", null,
                null, new BigDecimal("49.90"), new BigDecimal("20.00"), 10, 5, true);
        Page<ProdutoResponse> pagina = new PageImpl<>(List.of(produto), PageRequest.of(0, 10), 1);
        when(produtoService.listar(any())).thenReturn(pagina);

        ProdutoListBean bean = new ProdutoListBean(produtoService);
        bean.iniciar();

        assertThat(bean.getProdutos()).hasSize(1);
        assertThat(bean.getProdutos().get(0).codigo()).isEqualTo("SKU-1");
        assertThat(bean.isTemProximaPagina()).isFalse();
    }

    @Test
    void proximaPaginaAvancaEChamaServiceComPaginaSeguinte() {
        Page<ProdutoResponse> paginaVazia = new PageImpl<>(List.of(), PageRequest.of(1, 10), 20);
        when(produtoService.listar(any())).thenReturn(paginaVazia);

        ProdutoListBean bean = new ProdutoListBean(produtoService);
        bean.iniciar();
        bean.proximaPagina();

        assertThat(bean.getPaginaAtual()).isEqualTo(1);
    }
}
```

- [ ] **Step 2: Rodar e confirmar falha**

Run: `mvn -q -Dtest=ProdutoListBeanTest test`
Expected: FAIL — `ProdutoListBean` não existe.

- [ ] **Step 3: Implementar `ProdutoListBean`**

```java
package com.estoque.jsf;

import com.estoque.dto.response.ProdutoResponse;
import com.estoque.service.ProdutoService;
import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.context.support.SpringBeanAutowiringSupport;

import java.io.Serializable;
import java.util.List;

/**
 * Bean CDI genuíno ({@code @Named} + {@code @ViewScoped} do próprio JSF, não do Spring) — o
 * {@link ProdutoService} do Spring é injetado via {@link SpringBeanAutowiringSupport}, ponte
 * padrão do Spring pra frameworks web fora do seu próprio container de DI.
 */
@Named
@ViewScoped
public class ProdutoListBean implements Serializable {

    private static final int TAMANHO_PAGINA = 10;

    private ProdutoService produtoService;
    private List<ProdutoResponse> produtos;
    private int paginaAtual;
    private boolean temProximaPagina;

    public ProdutoListBean() {
        // Construtor sem args exigido pelo CDI para proxy de @ViewScoped.
    }

    // Construtor usado pelo teste (injeção direta, sem passar pelo Spring).
    ProdutoListBean(ProdutoService produtoService) {
        this.produtoService = produtoService;
    }

    @PostConstruct
    public void iniciar() {
        if (produtoService == null) {
            // processInjectionBasedOnCurrentContext usa RequestContextHolder, que só é populado
            // em requisições que passam pelo DispatcherServlet — as do FacesServlet não passam.
            // Por isso pega o ServletContext direto do FacesContext, que sempre está disponível.
            jakarta.servlet.ServletContext servletContext =
                    (jakarta.servlet.ServletContext) jakarta.faces.context.FacesContext
                            .getCurrentInstance().getExternalContext().getContext();
            SpringBeanAutowiringSupport.processInjectionBasedOnServletContext(this, servletContext);
        }
        paginaAtual = 0;
        carregarPagina();
    }

    // @Autowired do Spring, não @Inject do CDI: quem chama este setter é o
    // SpringBeanAutowiringSupport acima, por reflection direta — se fosse @Inject, o próprio
    // Weld tentaria resolver ProdutoService como bean CDI (que não é) e falharia o deploy antes
    // do @PostConstruct sequer rodar.
    @Autowired
    void setProdutoService(ProdutoService produtoService) {
        this.produtoService = produtoService;
    }

    private void carregarPagina() {
        Page<ProdutoResponse> pagina = produtoService.listar(PageRequest.of(paginaAtual, TAMANHO_PAGINA));
        this.produtos = pagina.getContent();
        this.temProximaPagina = pagina.hasNext();
    }

    public void proximaPagina() {
        paginaAtual++;
        carregarPagina();
    }

    public void paginaAnterior() {
        if (paginaAtual > 0) {
            paginaAtual--;
            carregarPagina();
        }
    }

    public List<ProdutoResponse> getProdutos() {
        return produtos;
    }

    public int getPaginaAtual() {
        return paginaAtual;
    }

    public boolean isTemProximaPagina() {
        return temProximaPagina;
    }
}
```

- [ ] **Step 4: Rodar o teste e confirmar sucesso**

Run: `mvn -q -Dtest=ProdutoListBeanTest test`
Expected: PASS, 2/2.

- [ ] **Step 5: Criar `produtos.xhtml`**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE html>
<html xmlns="http://www.w3.org/1999/xhtml"
      xmlns:h="jakarta.faces.html"
      xmlns:f="jakarta.faces.core">
<h:head>
    <title>Produtos — Controle de Estoque</title>
</h:head>
<h:body>
    <h1>Produtos</h1>
    <h:messages globalOnly="true" />

    <h:form id="formListagem">
        <h:dataTable value="#{produtoListBean.produtos}" var="produto" border="1">
            <h:column><f:facet name="header">Código</f:facet>#{produto.codigo}</h:column>
            <h:column><f:facet name="header">Nome</f:facet>#{produto.nome}</h:column>
            <h:column><f:facet name="header">Preço</f:facet>#{produto.precoVenda}</h:column>
            <h:column><f:facet name="header">Estoque</f:facet>#{produto.quantidadeEstoque}</h:column>
        </h:dataTable>

        <h:commandButton value="Anterior" action="#{produtoListBean.paginaAnterior}" />
        <h:outputText value="Página #{produtoListBean.paginaAtual + 1}" />
        <h:commandButton value="Próxima" action="#{produtoListBean.proximaPagina}"
                          rendered="#{produtoListBean.temProximaPagina}" />
    </h:form>

    <h:link outcome="produto-form" value="Novo produto" />
</h:body>
</html>
```

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/estoque/jsf/ProdutoListBean.java src/main/webapp/produtos.xhtml src/test/java/com/estoque/jsf/ProdutoListBeanTest.java
git commit -m "feat: adicionar tela JSF de listagem de produtos (ProdutoListBean)"
```

---

## Task 10: Tela JSF — cadastro de produto, login e segunda `SecurityFilterChain`

**Files:**
- Create: `src/main/java/com/estoque/jsf/ProdutoFormBean.java`
- Create: `src/main/webapp/produto-form.xhtml`
- Create: `src/main/webapp/login.xhtml`
- Modify: `src/main/java/com/estoque/config/SecurityConfig.java`
- Test: `src/test/java/com/estoque/jsf/ProdutoFormBeanTest.java`

**Interfaces:**
- Consumes: `ProdutoService.criar(ProdutoRequest)`, `CategoriaService` (para o combo de categoria — usar `CategoriaService.listar(Pageable)` já existente), `UserDetailsServiceImpl` (login).
- Produces: `ProdutoFormBean` com `criar()` (action do form), `getRequest()`/campos do formulário; segunda `SecurityFilterChain` em `/faces/**`.

- [ ] **Step 1: Escrever o teste (falha: classe não existe)**

```java
package com.estoque.jsf;

import com.estoque.dto.request.ProdutoRequest;
import com.estoque.dto.response.ProdutoResponse;
import com.estoque.service.ProdutoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProdutoFormBeanTest {

    @Mock private ProdutoService produtoService;

    @Test
    void deveChamarProdutoServiceComOsCamposPreenchidos() {
        ProdutoFormBean bean = new ProdutoFormBean(produtoService);
        bean.setCodigo("SKU-JSF-1");
        bean.setNome("Produto via JSF");
        bean.setCategoriaId(1L);
        bean.setPrecoVenda(new BigDecimal("29.90"));
        bean.setEstoqueMinimo(5);

        when(produtoService.criar(new ProdutoRequest("SKU-JSF-1", "Produto via JSF", null, 1L, null, null,
                new BigDecimal("29.90"), 5)))
                .thenReturn(new ProdutoResponse(1L, "SKU-JSF-1", "Produto via JSF", null, "Roupas", null,
                        null, new BigDecimal("29.90"), BigDecimal.ZERO, 0, 5, true));

        String outcome = bean.criar();

        assertThat(outcome).isEqualTo("produtos?faces-redirect=true");
        verify(produtoService).criar(new ProdutoRequest("SKU-JSF-1", "Produto via JSF", null, 1L, null, null,
                new BigDecimal("29.90"), 5));
    }
}
```

- [ ] **Step 2: Rodar e confirmar falha**

Run: `mvn -q -Dtest=ProdutoFormBeanTest test`
Expected: FAIL — `ProdutoFormBean` não existe.

- [ ] **Step 3: Implementar `ProdutoFormBean`**

```java
package com.estoque.jsf;

import com.estoque.dto.request.ProdutoRequest;
import com.estoque.service.ProdutoService;
import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.context.support.SpringBeanAutowiringSupport;

import java.io.Serializable;
import java.math.BigDecimal;

@Named
@ViewScoped
public class ProdutoFormBean implements Serializable {

    private ProdutoService produtoService;

    private String codigo;
    private String nome;
    private Long categoriaId;
    private BigDecimal precoVenda;
    private int estoqueMinimo;

    public ProdutoFormBean() {
    }

    // Construtor usado pelo teste (injeção direta, sem passar pelo Spring).
    ProdutoFormBean(ProdutoService produtoService) {
        this.produtoService = produtoService;
    }

    @PostConstruct
    public void iniciar() {
        if (produtoService == null) {
            jakarta.servlet.ServletContext servletContext =
                    (jakarta.servlet.ServletContext) jakarta.faces.context.FacesContext
                            .getCurrentInstance().getExternalContext().getContext();
            SpringBeanAutowiringSupport.processInjectionBasedOnServletContext(this, servletContext);
        }
    }

    // @Autowired do Spring, não @Inject do CDI — mesma razão documentada em ProdutoListBean.
    @Autowired
    void setProdutoService(ProdutoService produtoService) {
        this.produtoService = produtoService;
    }

    public String criar() {
        // Validação de precoVenda <= 0 é feita pelo Bean Validation em ProdutoRequest via
        // @NotNull @DecimalMin — no fluxo REST isso vira 400 via @Valid no controller; aqui,
        // sem @Valid do JSF, a exceção sobe crua. É intencional: é o "bug" documentado no
        // README (Experimento JSF) para reproduzir o desvio de fase Process Validations →
        // Render Response quando o form JSF ganhar <f:validateBean> numa iteração futura.
        produtoService.criar(new ProdutoRequest(codigo, nome, null, categoriaId, null, null,
                precoVenda, estoqueMinimo));
        return "produtos?faces-redirect=true";
    }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public Long getCategoriaId() { return categoriaId; }
    public void setCategoriaId(Long categoriaId) { this.categoriaId = categoriaId; }
    public BigDecimal getPrecoVenda() { return precoVenda; }
    public void setPrecoVenda(BigDecimal precoVenda) { this.precoVenda = precoVenda; }
    public int getEstoqueMinimo() { return estoqueMinimo; }
    public void setEstoqueMinimo(int estoqueMinimo) { this.estoqueMinimo = estoqueMinimo; }
}
```

- [ ] **Step 4: Rodar o teste e confirmar sucesso**

Run: `mvn -q -Dtest=ProdutoFormBeanTest test`
Expected: PASS.

- [ ] **Step 5: Criar `produto-form.xhtml`**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE html>
<html xmlns="http://www.w3.org/1999/xhtml"
      xmlns:h="jakarta.faces.html"
      xmlns:f="jakarta.faces.core">
<h:head>
    <title>Novo produto — Controle de Estoque</title>
</h:head>
<h:body>
    <h1>Novo produto</h1>
    <h:messages />

    <h:form id="formCadastro">
        <h:panelGrid columns="2">
            <h:outputLabel for="codigo" value="Código" />
            <h:inputText id="codigo" value="#{produtoFormBean.codigo}" required="true" />

            <h:outputLabel for="nome" value="Nome" />
            <h:inputText id="nome" value="#{produtoFormBean.nome}" required="true" />

            <h:outputLabel for="categoriaId" value="ID da categoria" />
            <h:inputText id="categoriaId" value="#{produtoFormBean.categoriaId}" required="true" />

            <h:outputLabel for="precoVenda" value="Preço de venda" />
            <h:inputText id="precoVenda" value="#{produtoFormBean.precoVenda}" required="true" />

            <h:outputLabel for="estoqueMinimo" value="Estoque mínimo" />
            <h:inputText id="estoqueMinimo" value="#{produtoFormBean.estoqueMinimo}" />
        </h:panelGrid>

        <h:commandButton value="Salvar" action="#{produtoFormBean.criar}" />
    </h:form>
</h:body>
</html>
```

- [ ] **Step 6: Criar `login.xhtml` (form login tradicional do Spring Security)**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE html>
<html xmlns="http://www.w3.org/1999/xhtml">
<head><title>Login — Controle de Estoque</title></head>
<body>
    <h1>Login</h1>
    <form action="#{request.contextPath}/faces/j_spring_security_check" method="post">
        <label>E-mail: <input type="text" name="username" /></label><br/>
        <label>Senha: <input type="password" name="password" /></label><br/>
        <input type="submit" value="Entrar" />
    </form>
</body>
</html>
```

- [ ] **Step 7: Adicionar a segunda `SecurityFilterChain` em `SecurityConfig`**

Adicionar `@Order(1)` na `securityFilterChain` REST existente (pra garantir que ela é avaliada antes) e criar uma nova `@Bean` `@Order(2)` para `/faces/**`:

```java
    @Bean
    @org.springframework.core.annotation.Order(1)
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // ... conteúdo existente, inalterado, mas com http.securityMatcher("/api/v1/**") adicionado
        // logo após o http.csrf(...) para restringir essa chain só às rotas REST:
```

Adicionar `.securityMatcher("/api/v1/**")` como primeira chamada do builder `http`.

Nova chain:

```java
    @Bean
    @org.springframework.core.annotation.Order(2)
    public SecurityFilterChain jsfSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/faces/**")
                // login.xhtml é um <form> HTML puro, sem token CSRF — desabilitar aqui replica a
                // mesma postura que a chain REST já tem (.csrf(csrf -> csrf.disable()) acima).
                // Sem isso, o POST para j_spring_security_check tomaria 403 por falta de token.
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/faces/login.xhtml", "/faces/javax.faces.resource/**").permitAll()
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/faces/login.xhtml")
                        .loginProcessingUrl("/faces/j_spring_security_check")
                        .defaultSuccessUrl("/faces/produtos.xhtml", true)
                        .permitAll())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED));

        return http.build();
    }
```

Reaproveita o mesmo `UserDetailsService`/`DaoAuthenticationProvider` já configurados nos beans existentes de `SecurityConfig` — nenhum bean novo de autenticação é necessário, só a `SecurityFilterChain`.

- [ ] **Step 8: Rodar a suíte completa**

Run: `mvn clean verify`
Expected: BUILD SUCCESS. Os testes de `SecurityConfig`/controllers continuam passando porque a chain REST ganhou `@Order(1)` + `securityMatcher("/api/v1/**")`, preservando o comportamento exato que os testes já verificam.

- [ ] **Step 9: Verificação manual no navegador**

Run: `mvn clean package cargo:run`

1. Acessar `http://localhost:8080/controle-estoque/faces/produtos.xhtml` sem login → redireciona pra `login.xhtml`.
2. Logar com `admin@estoque.com`/`admin123` → redireciona pra `produtos.xhtml`, tabela carregada.
3. Ir em "Novo produto", preencher e salvar → volta pra listagem, produto novo aparece.

- [ ] **Step 10: Commit**

```bash
git add src/main/java/com/estoque/jsf/ProdutoFormBean.java src/main/webapp/produto-form.xhtml src/main/webapp/login.xhtml src/main/java/com/estoque/config/SecurityConfig.java src/test/java/com/estoque/jsf/ProdutoFormBeanTest.java
git commit -m "feat: adicionar tela JSF de cadastro de produto, login e segunda SecurityFilterChain"
```

---

## Task 11: Oracle — Docker Compose, driver, migrations, dialect

**Files:**
- Modify: `docker-compose.yml`
- Create: `src/main/resources/app-oracle.properties`
- Create: `src/main/resources/db/migration/oracle/V1__schema_categoria_colecao.sql` (e V2-V7, ver abaixo)

**Interfaces:**
- Consumes: `PersistenceConfig`, `FlywayConfig` (Tasks 2-3), lidos com o perfil `oracle` ativo.
- Produces: `docker compose up` sobe Oracle XE + a aplicação no perfil `oracle`, com schema aplicado via Flyway.

- [ ] **Step 1: Trocar `docker-compose.yml`, substituindo o serviço `postgres` por `oracle`**

```yaml
services:
  oracle:
    image: gvenzl/oracle-xe:21-slim
    environment:
      ORACLE_PASSWORD: estoque
      APP_USER: estoque
      APP_USER_PASSWORD: estoque
    ports:
      - "1521:1521"
    volumes:
      - estoque-oracle-data:/opt/oracle/oradata
    healthcheck:
      test: ["CMD", "healthcheck.sh"]
      interval: 10s
      timeout: 10s
      retries: 10
      start_period: 60s

  app:
    build: .
    depends_on:
      oracle:
        condition: service_healthy
    environment:
      SPRING_PROFILES_ACTIVE: oracle
      JWT_SECRET: MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDEyMzQ1Njc4OTAxMjM0NTY3ODk=
    ports:
      - "8080:8080"

volumes:
  estoque-oracle-data:
```

- [ ] **Step 2: Criar `app-oracle.properties`**

```properties
db.url=jdbc:oracle:thin:@//oracle:1521/XEPDB1
db.username=estoque
db.password=estoque
db.driver=oracle.jdbc.OracleDriver
hibernate.dialect=org.hibernate.dialect.OracleDialect
flyway.locations=classpath:db/migration/oracle

jwt.secret=${JWT_SECRET}
jwt.expiration-ms=3600000
```

- [ ] **Step 3: Criar as 7 migrations Oracle, traduzindo tipo por tipo (`BIGINT`→`NUMBER(19)`, `INT`→`NUMBER(10)`, `VARCHAR`→`VARCHAR2`, `BOOLEAN`→`NUMBER(1,0)`, `DECIMAL(p,s)`→`NUMBER(p,s)`)**

`V1__schema_categoria_colecao.sql`:
```sql
CREATE TABLE categoria (
    id NUMBER(19) GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    nome VARCHAR2(80) NOT NULL UNIQUE,
    descricao VARCHAR2(255)
);

CREATE TABLE colecao (
    id NUMBER(19) GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    nome VARCHAR2(80) NOT NULL UNIQUE,
    descricao VARCHAR2(255)
);
```

`V2__schema_fornecedor.sql`:
```sql
CREATE TABLE fornecedor (
    id NUMBER(19) GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    cnpj VARCHAR2(14) NOT NULL UNIQUE,
    razao_social VARCHAR2(150) NOT NULL,
    telefone VARCHAR2(20) NOT NULL,
    email VARCHAR2(150) NOT NULL,
    logradouro VARCHAR2(150) NOT NULL,
    numero VARCHAR2(20) NOT NULL,
    bairro VARCHAR2(100) NOT NULL,
    cidade VARCHAR2(100) NOT NULL,
    estado VARCHAR2(2) NOT NULL,
    cep VARCHAR2(9) NOT NULL,
    ativo NUMBER(1,0) DEFAULT 1 NOT NULL,
    criado_em TIMESTAMP NOT NULL,
    atualizado_em TIMESTAMP NOT NULL
);
```

`V3__schema_funcionario.sql`:
```sql
CREATE TABLE funcionario (
    id NUMBER(19) GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    nome VARCHAR2(100) NOT NULL,
    sobrenome VARCHAR2(100) NOT NULL,
    cpf VARCHAR2(11) NOT NULL UNIQUE,
    email VARCHAR2(150) NOT NULL UNIQUE,
    senha VARCHAR2(255) NOT NULL,
    matricula VARCHAR2(20) NOT NULL,
    data_admissao DATE NOT NULL,
    setor VARCHAR2(80) NOT NULL,
    role VARCHAR2(20) NOT NULL,
    logradouro VARCHAR2(150) NOT NULL,
    numero VARCHAR2(20) NOT NULL,
    bairro VARCHAR2(100) NOT NULL,
    cidade VARCHAR2(100) NOT NULL,
    estado VARCHAR2(2) NOT NULL,
    cep VARCHAR2(9) NOT NULL,
    ativo NUMBER(1,0) DEFAULT 1 NOT NULL,
    criado_em TIMESTAMP NOT NULL,
    atualizado_em TIMESTAMP NOT NULL
);
```

`V4__schema_produto.sql`:
```sql
CREATE TABLE produto (
    id NUMBER(19) GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    codigo VARCHAR2(40) NOT NULL UNIQUE,
    nome VARCHAR2(150) NOT NULL,
    descricao VARCHAR2(500),
    categoria_id NUMBER(19) NOT NULL REFERENCES categoria(id),
    colecao_id NUMBER(19) REFERENCES colecao(id),
    fornecedor_id NUMBER(19) REFERENCES fornecedor(id),
    preco_venda NUMBER(12,2) NOT NULL,
    custo_medio NUMBER(12,2) DEFAULT 0 NOT NULL,
    quantidade_estoque NUMBER(10) DEFAULT 0 NOT NULL,
    estoque_minimo NUMBER(10) DEFAULT 0 NOT NULL,
    version NUMBER(19) DEFAULT 0 NOT NULL,
    ativo NUMBER(1,0) DEFAULT 1 NOT NULL,
    criado_em TIMESTAMP NOT NULL,
    atualizado_em TIMESTAMP NOT NULL
);
```

`V5__schema_movimentacao.sql`:
```sql
CREATE TABLE movimentacao_estoque (
    id NUMBER(19) GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    produto_id NUMBER(19) NOT NULL REFERENCES produto(id),
    funcionario_id NUMBER(19) NOT NULL REFERENCES funcionario(id),
    quantidade NUMBER(10) NOT NULL,
    observacao VARCHAR2(255),
    data_movimentacao TIMESTAMP NOT NULL
);

CREATE TABLE entrada (
    id NUMBER(19) PRIMARY KEY REFERENCES movimentacao_estoque(id),
    custo_unitario NUMBER(12,2) NOT NULL,
    fornecedor_id NUMBER(19) REFERENCES fornecedor(id)
);

CREATE TABLE saida (
    id NUMBER(19) PRIMARY KEY REFERENCES movimentacao_estoque(id),
    motivo VARCHAR2(20) NOT NULL
);
```

`V6__seed.sql`:
```sql
INSERT INTO funcionario (nome, sobrenome, cpf, email, senha, matricula, data_admissao, setor, role,
                          logradouro, numero, bairro, cidade, estado, cep, ativo, criado_em, atualizado_em)
VALUES ('Admin', 'Sistema', '00000000000', 'admin@estoque.com',
        '$2b$10$RCnF8W47c/o7N.ePdJU2Deq/2NNmz9Bje5XhPTj8epL7T0vParxcW',
        'F000', CURRENT_DATE, 'Administração', 'ADMIN',
        'Rua Principal', '1', 'Centro', 'Curitiba', 'PR', '80000-000',
        1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO categoria (nome, descricao) VALUES
    ('Calçados', 'Tênis, sapatos e sandálias');
INSERT INTO categoria (nome, descricao) VALUES
    ('Vestuário', 'Camisetas, calças e jaquetas');
INSERT INTO categoria (nome, descricao) VALUES
    ('Acessórios', 'Bolsas, cintos e bonés');

INSERT INTO colecao (nome, descricao) VALUES
    ('Verão 2026', 'Coleção lançada em janeiro de 2026');
INSERT INTO colecao (nome, descricao) VALUES
    ('Inverno 2026', 'Coleção lançada em junho de 2026');
```

Nota: Oracle não aceita `INSERT INTO ... VALUES (...), (...), (...)` (sintaxe multi-linha do H2/Postgres) — por isso o `V6` vira múltiplos `INSERT` de uma linha só.

`V7__schema_auditoria.sql`:
```sql
CREATE TABLE tentativa_saida_negada (
    id NUMBER(19) GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    produto_codigo VARCHAR2(40) NOT NULL,
    funcionario_email VARCHAR2(150) NOT NULL,
    quantidade_solicitada NUMBER(10) NOT NULL,
    quantidade_disponivel NUMBER(10) NOT NULL,
    registrada_em TIMESTAMP NOT NULL
);
```

- [ ] **Step 4: Rodar a suíte completa (perfil `test`/H2 continua sendo o único usado pelos testes automatizados — nada muda aqui)**

Run: `mvn clean verify`
Expected: BUILD SUCCESS, 105 testes, 0 falhas — Oracle não é tocado pela suíte automatizada.

- [ ] **Step 5: Verificação manual — subir o Oracle real via Docker**

Run: `docker compose up --build` (primeira subida do Oracle XE demora alguns minutos — a imagem inicializa o banco internamente)
Expected: log do `oracle` mostrando `DATABASE IS READY TO USE!`, seguido do `app` conectando, aplicando as 7 migrations Oracle via Flyway, e subindo sem erro de `ddl-auto: validate`.

```bash
curl -X POST http://localhost:8080/controle-estoque/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@estoque.com","senha":"admin123"}'
```

Expected: `200`, token JWT — prova que o schema Oracle bate exatamente com as entidades JPA.

- [ ] **Step 6: Commit**

```bash
git add docker-compose.yml src/main/resources/app-oracle.properties src/main/resources/db/migration/oracle
git commit -m "feat: adicionar perfil Oracle (Docker, migrations, dialect), substituindo Postgres"
```

---

## Task 12: `Dockerfile` para WAR + Tomcat

**Files:**
- Modify: `Dockerfile`

**Interfaces:**
- Consumes: WAR gerado por `mvn clean package`.
- Produces: imagem Docker rodando o WAR num Tomcat 10.1, usada pelo serviço `app` do `docker-compose.yml` (Task 11).

- [ ] **Step 1: Reescrever o `Dockerfile`**

```dockerfile
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q dependency:go-offline
COPY src ./src
RUN mvn -q clean package -DskipTests

FROM tomcat:10.1-jre17
RUN rm -rf /usr/local/tomcat/webapps/ROOT
COPY --from=build /app/target/controle-estoque.war /usr/local/tomcat/webapps/controle-estoque.war
EXPOSE 8080
CMD ["catalina.sh", "run"]
```

- [ ] **Step 2: Rodar `docker compose up --build` de novo, confirmar que a imagem builda**

Run: `docker compose up --build`
Expected: mesmo resultado da Task 11 Step 5, agora usando a imagem Tomcat oficial em vez do jar Boot.

- [ ] **Step 3: Commit**

```bash
git add Dockerfile
git commit -m "build: trocar Dockerfile de jar Boot para WAR em Tomcat"
```

---

## Task 13: README — instruções novas, seção JSF, seção Oracle, experimento `@RequestScoped`

**Files:**
- Modify: `README.md`

**Interfaces:**
- Nenhuma (só documentação).

- [ ] **Step 1: Atualizar a seção "Como rodar localmente"**

Substituir o bloco `mvn spring-boot:run` (que não existe mais) por:

```markdown
## Como rodar localmente (H2, sem dependências externas)

\`\`\`bash
mvn clean package cargo:run
\`\`\`

A aplicação sobe num Tomcat 10.1 embarcado via Cargo, em \`http://localhost:8080/controle-estoque\`,
com banco H2 em memória e dados de seed já carregados.

- API REST: \`http://localhost:8080/controle-estoque/api/v1/**\`
- Telas JSF: \`http://localhost:8080/controle-estoque/faces/produtos.xhtml\`
\`\`\`
```

- [ ] **Step 2: Atualizar a seção "Como rodar com PostgreSQL" para Oracle**

Trocar o título e o conteúdo para refletir Oracle em vez de Postgres (`docker compose up --build`, perfil `oracle`, aviso sobre a demora da primeira subida do Oracle XE).

- [ ] **Step 3: Adicionar ao "Mapa de estudo — do guia técnico para o código" as linhas de JSF**

| Conceito | Onde ver | O que observar |
|---|---|---|
| Ciclo de vida do JSF | `jsf/ProdutoFormBean.java:criar()` + `produto-form.xhtml` | Submeter o form sem preencher `codigo` (campo `required="true"`) força o desvio da fase 3 (Process Validations) direto pra fase 6 (Render Response) — a action `criar()` nunca roda. |
| Escopos de managed bean | `jsf/ProdutoListBean.java` (`@ViewScoped`) | Paginar (Próxima/Anterior) sem perder a página atual — sobrevive ao postback. Ver Experimento novo abaixo pra ver o bug ao trocar pra `@RequestScoped`. |
| Spring beans dentro de managed beans JSF | `jsf/ProdutoListBean.java:iniciar()` | `SpringBeanAutowiringSupport.processInjectionBasedOnServletContext(this, servletContext)` — a ponte entre o container CDI (Weld) e o `ApplicationContext` do Spring, sem `SpringBeanFacesELResolver`. |
| Oracle real (não só teórico) | `db/migration/oracle/V4__schema_produto.sql` vs. `db/migration/h2/V4__schema_produto.sql` | Os dois arquivos lado a lado mostram o diff de sintaxe do guia (seção 11) acontecendo de verdade no mesmo projeto: `BIGINT`→`NUMBER(19)`, `VARCHAR`→`VARCHAR2`, `BOOLEAN`→`NUMBER(1,0)`. |

- [ ] **Step 4: Adicionar um novo experimento (`Experimento 10`) reproduzindo o bug de escopo**

```markdown
### Experimento 10 — `@ViewScoped` virando `@RequestScoped`

1. Em `jsf/ProdutoListBean.java`, trocar `import jakarta.faces.view.ViewScoped;` por
   `import jakarta.enterprise.context.RequestScoped;`, e a anotação `@ViewScoped` por `@RequestScoped`.
2. Rodar `mvn clean package cargo:run`, acessar `/faces/produtos.xhtml`, clicar em "Próxima".

**Esperado:** a tabela aparece vazia (ou volta pra página 0) a cada clique — o bean é recriado do zero
a cada requisição, perdendo `paginaAtual`. É exatamente a "pergunta clássica de gestor" do guia:
"a lista da tela some quando o usuário clica no botão de filtrar".

**Âncora que treina:** *Request morre na requisição · View sobrevive ao postback.*
```

- [ ] **Step 5: Adicionar à lista "O que este projeto não demonstra" a remoção das linhas de JSF e Oracle (agora demonstrados)**

Remover as duas linhas correspondentes (JSF/managed bean scopes e Oracle) da lista existente — o projeto passou a cobrir as duas.

- [ ] **Step 6: Commit**

```bash
git add README.md
git commit -m "docs: atualizar README com instrucoes de Tomcat/Oracle, mapa de estudo JSF e Experimento 10"
```

---

## Task 14: Verificação final

**Files:**
- Nenhum arquivo novo — apenas verificação.

- [ ] **Step 1: Suíte completa**

Run: `mvn clean verify`
Expected: BUILD SUCCESS, 105+ testes (105 originais + `ProdutoListBeanTest` + `ProdutoFormBeanTest`), 0 falhas.

- [ ] **Step 2: Fluxo REST completo via Tomcat local**

Run: `mvn clean package cargo:run`

Repetir o roteiro de smoke test original (login → criar produto → entrada → saída → 409 por saldo insuficiente → kardex → relatório) contra `http://localhost:8080/controle-estoque/api/v1/**`.

- [ ] **Step 3: Fluxo JSF completo no navegador**

Login em `/faces/login.xhtml` → listagem em `/faces/produtos.xhtml` → paginação → cadastro em `/faces/produto-form.xhtml` → volta pra listagem com o produto novo.

- [ ] **Step 4: Fluxo Oracle completo via Docker**

Run: `docker compose up --build`

Repetir o smoke test REST contra o container, confirmando que as migrations Oracle aplicaram e o schema bate com `ddl-auto: validate`.

- [ ] **Step 5: Commit final (se houver ajustes de última hora)**

```bash
git status
```

Se tudo já estiver commitado, nenhuma ação adicional — este passo existe para capturar qualquer correção de última hora encontrada na verificação manual.
