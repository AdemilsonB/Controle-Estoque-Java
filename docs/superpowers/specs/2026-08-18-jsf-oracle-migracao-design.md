# Design — JSF + Oracle: migração de Spring Boot para Spring tradicional

Data: 2026-08-18

## 1. Contexto

O projeto hoje é uma API REST Spring Boot 3.3.4 (jar executável, Tomcat embarcado), com H2 em dev/test e PostgreSQL em produção, 105 testes passando (`mvn clean verify`). Essa arquitetura veio da migração original documentada em `docs/superpowers/specs/2026-08-13-spring-estoque-design.md` e do plano em `docs/superpowers/plans/2026-08-13-spring-estoque.md`.

O motivador desta mudança é um guia de estudo para entrevista técnica (seções de JSF, escopos de managed bean, Oracle e "Spring vs Spring Boot") que passou a ser incorporado ao próprio README do projeto como mapa de estudo + experimentos reproduzíveis. Duas seções desse guia não tinham nenhuma contrapartida no código: o ciclo de vida do JSF/escopos de managed bean, e Oracle. O objetivo desta mudança é fechar as duas, com o nível de fidelidade que o próprio guia recomenda para entrevistas — "projeto JSF legado quase nunca é Spring Boot; normalmente é Spring tradicional em WAR".

Durante o brainstorming, foi levantada a alternativa de isolar essa mudança num módulo separado (Maven multi-módulo, preservando a API REST atual intocada). Essa alternativa foi apresentada e **recusada explicitamente**: a decisão tomada é migrar o módulo único existente, removendo a autoconfiguração do Spring Boot, mesmo com o risco de regressão que isso implica sobre os 105 testes já verdes. Este documento assume essa decisão como definitiva.

## 2. Escopo

- Remover Spring Boot do projeto: `spring-boot-starter-parent`, autoconfiguração, `application.yml`, `@SpringBootApplication`. Substituir por Spring Framework "tradicional" com configuração Java explícita (`@Configuration`), gerenciado por um BOM de versões manual.
- Trocar o empacotamento de `jar` para `war`, deployável num Tomcat externo (não embarcado).
- Adicionar um módulo de telas JSF (2 páginas: listagem paginada de `Produto` e formulário de cadastro), reaproveitando `ProdutoService` sem alteração de contrato.
- Adicionar Oracle (via Docker, imagem `gvenzl/oracle-xe`) como o banco de todo o sistema em ambiente "prod"-equivalente, substituindo o PostgreSQL. H2 em memória continua sendo o banco de testes (Oracle não tem modo embarcado; ver seção 8).
- Duas cadeias de segurança (`SecurityFilterChain`) coexistindo: a JWT stateless já existente para `/api/v1/**`, e uma nova baseada em sessão/form login para `/faces/**`.

Fora de escopo: novas funcionalidades de negócio, CRUD completo em JSF para todas as entidades (fica só `Produto`, ver seção 7), CI/CD, deploy em nuvem.

## 3. Stack técnica (após a migração)

| Categoria | Antes (Spring Boot) | Depois (Spring tradicional) |
|---|---|---|
| Empacotamento | `jar` executável | `war`, deploy em Tomcat externo |
| Bootstrap | `@SpringBootApplication` + `SpringApplication.run` | `WebApplicationInitializer` registrando `DispatcherServlet` e `FacesServlet` |
| Configuração | `application.yml` + autoconfiguração | `@Configuration` Java explícita (DataSource, JPA, Security, Flyway) |
| Gerenciamento de versão | `spring-boot-starter-parent` | BOM `spring-framework-bom` + `spring-security-bom` no `<dependencyManagement>` |
| Web REST | `spring-boot-starter-web` | `spring-webmvc` |
| Persistência | `spring-boot-starter-data-jpa` | `spring-orm` + Hibernate + `spring-data-jpa` (sem starter) |
| Banco (dev/test) | H2 em memória | inalterado — H2 em memória |
| Banco (prod-equivalente) | PostgreSQL 16 | **Oracle XE 21c** (`gvenzl/oracle-xe`, Docker) |
| Migrations | Flyway, uma pasta | Flyway, pastas separadas por vendor (`db/migration/h2`, `db/migration/oracle`) |
| Segurança | `spring-boot-starter-security`, uma `SecurityFilterChain` stateless | `spring-security-web`/`-config`, duas `SecurityFilterChain` (`/api/v1/**` stateless, `/faces/**` com sessão) |
| UI nova | — | JSF (Mojarra) + `spring-faces` (bridge Spring↔JSF EL) |
| Documentação de API | springdoc-openapi | inalterado |
| Testes | `spring-boot-starter-test`, `@SpringBootTest` | `spring-test`, `@ContextConfiguration` apontando pras `@Configuration` novas — MockMvc continua igual (não é Boot-specific) |

## 4. Arquitetura em camadas

```
com.estoque
 ├─ config/           AppConfig (DataSource/JPA/TransactionManager), SecurityConfig,
 │                     WebMvcConfig, FlywayConfig, OpenApiConfig
 ├─ controller/        @RestController — inalterado
 ├─ jsf/                managed beans (@Named, @ViewScoped) — camada nova
 │   ├─ ProdutoListBean.java
 │   └─ ProdutoFormBean.java
 ├─ dto/, entity/, enums/, exception/, mapper/, repository/, security/, service/
 │                     inalterados — nenhuma classe de domínio muda
 └─ WebAppInitializer.java   substitui EstoqueApplication
```

A camada `service/` não é tocada: é exatamente o ponto do diagrama original do guia — "a camada de serviço não sabe quem chamou". `ProdutoListBean`/`ProdutoFormBean` injetam `ProdutoService` pelo mesmo contrato que `ProdutoController` já usa.

## 5. Estratégia de migração (proteção dos 105 testes existentes)

Migração incremental, rodando `mvn clean verify` a cada passo — mesmo ritmo desta sessão. Ordem:

1. **`pom.xml`**: trocar `parent` por `<dependencyManagement>` com `spring-framework-bom` + `spring-security-bom`; `packaging` vira `war`; adicionar `spring-boot-maven-plugin` → remover, adicionar `maven-war-plugin`.
2. **Configuração de contexto**: criar `AppConfig` (`DataSource` via `HikariCP` explícito, `LocalContainerEntityManagerFactoryBean`, `JpaTransactionManager`), `FlywayConfig` (`FlywayMigrationInitializer` manual), replicando exatamente os valores que hoje vêm do `application.yml`.
3. **`WebAppInitializer`**: implementa `WebApplicationInitializer`, registra `AnnotationConfigWebApplicationContext` com as `@Configuration`, registra `DispatcherServlet` mapeado em `/api/v1/*`.
4. **Rodar a suíte** — nesse ponto a API REST deve continuar 100% funcional, só sem o Boot por baixo. Nenhum `@RestController`/`@Service`/`@Repository`/`@Entity` muda.
5. **`SecurityConfig`**: adaptar para não depender de autoconfiguração (`@EnableWebSecurity` continua igual, mas o registro do filtro na servlet precisa de `AbstractSecurityWebApplicationInitializer` em vez do auto-registro do Boot).
6. **Rodar a suíte de novo.**
7. Só then adicionar JSF (`FacesServlet`, managed beans, páginas `.xhtml`) e a segunda `SecurityFilterChain` — a API REST já está estável e testada sem Boot antes de introduzir a superfície nova.
8. Por último, trocar o banco de prod para Oracle (migrations + `docker-compose.yml`) — isolado do resto porque só afeta o ambiente `prod`-equivalente, não os testes (que continuam em H2).

Cada passo é um commit próprio, seguindo o padrão já usado nesta sessão.

## 6. Empacotamento e deploy

- `packaging` no `pom.xml` vira `war`.
- Tomcat externo para desenvolvimento local: documentar no README como baixar/rodar um Tomcat 10.1 (compatível com Jakarta EE 10, mesmo namespace `jakarta.*` já usado no projeto) e fazer deploy do `.war` gerado em `target/`.
- `mvn spring-boot:run` deixa de existir; o fluxo de dev vira `mvn clean package cargo:run` usando o `cargo-maven3-plugin` configurado contra um Tomcat 10.1 baixado localmente (`containerId=tomcat10x`, `type=installed`) — automatiza start do Tomcat + deploy do `.war` num único comando, sem exigir `$CATALINA_HOME` configurado manualmente pelo desenvolvedor.

## 7. Telas JSF

Escopo mínimo, apenas `Produto`:

- **`produtos.xhtml` + `ProdutoListBean` (`@ViewScoped`)**: tabela paginada (`<h:dataTable>`), reaproveitando `ProdutoService.listar(Pageable)`. Demonstra `@ViewScoped` sobrevivendo ao postback de paginação — e o README ganha um experimento novo trocando para `@RequestScoped` de propósito, pra ver a lista sumir a cada clique (mesmo padrão dos 9 experimentos já existentes).
- **`produto-form.xhtml` + `ProdutoFormBean` (`@ViewScoped`)**: formulário de cadastro com `<h:message>`/`<h:messages>` visíveis, reaproveitando `ProdutoService.criar(ProdutoRequest)`. Validação de preço negativo força o desvio da fase 3 (Process Validations) direto pra fase 6 (Render Response) sem chamar a `action` — o "bug clássico" que o guia descreve ("clica no botão e não acontece nada"), agora reproduzível de verdade.

Sem CRUD de mais nenhuma entidade — é demonstração, não segunda UI completa (decisão já tomada no brainstorming).

## 8. Banco de dados Oracle

- `docker-compose.yml` ganha um serviço `oracle` (`gvenzl/oracle-xe:21-slim`, porta `1521`, sem necessidade de conta Oracle — imagem gratuita da comunidade). Substitui o serviço `postgres` existente; o serviço `app` passa a apontar pra ele.
- Dependência nova: `com.oracle.database.jdbc:ojdbc11`.
- Migrations do Flyway passam a ter duas pastas por vendor: `db/migration/h2` (dev/test, migrations atuais adaptadas) e `db/migration/oracle` (sintaxe própria: `NUMBER` em vez de `BIGINT`/`INT`, `VARCHAR2` em vez de `VARCHAR`, `NUMBER(1)` para boolean, `GENERATED BY DEFAULT AS IDENTITY` tem suporte nativo no Oracle 12c+ mas com particularidades de sintaxe). `FlywayConfig` escolhe a pasta pela property de perfil ativo.
- **H2 continua sendo o banco de todos os testes** — Oracle não tem modo embarcado/em memória, e o projeto mantém a restrição original "sem Testcontainers". Isso é uma lacuna real e documentada: a suíte não pega bug específico de dialeto Oracle. O README ganha uma nota explícita sobre isso — é, coincidentemente, a pegadinha exata que a seção 11 do guia descreve.
- Hibernate precisa do dialect certo por perfil (`OracleDialect` vs `H2Dialect`) — resolvido via property, não autoconfiguração.

## 9. Segurança

Duas `SecurityFilterChain` com `@Order` explícito:

1. **Existente, inalterada em comportamento**: `/api/v1/**`, stateless, `JwtAuthFilter`, `ROTAS_PUBLICAS` como já está.
2. **Nova**: `/faces/**`, `SessionCreationPolicy.IF_REQUIRED` (JSF precisa de sessão pro view state), form login tradicional contra o mesmo `UserDetailsServiceImpl`/`Funcionario` (mesmas credenciais, incluindo o usuário seed `admin@estoque.com`/`admin123`), página de login própria em JSF.

Nenhuma mudança nas credenciais ou no hash BCrypt existente — é o mesmo `Funcionario`, dois mecanismos de autenticação.

## 10. Testes

- MockMvc continua funcionando (é Spring MVC puro). Troca: `@SpringBootTest` → `@ContextConfiguration(classes = {AppConfig.class, SecurityConfig.class, ...})` + `@WebAppConfiguration` onde precisar de servlet context.
- `@DataJpaTest`/`@ActiveProfiles("test")` não existem fora do Boot — substituídos por `@ContextConfiguration` com um perfil de teste próprio (`test`) resolvido nas `@Configuration` novas via `@Profile("test")`.
- Testes de JSF: cobertura via `ProdutoListBean`/`ProdutoFormBean` como POJOs testáveis por Mockito (chamando `ProdutoService` mockado), sem precisar subir o `FacesContext` completo — o ciclo de vida JSF de 6 fases em si não é testável por unit test de forma prática; a demonstração das fases é o experimento manual no navegador, documentado no README, não um teste automatizado.
- Meta: manter os 105 testes existentes verdes ao longo de toda a migração (seção 5), mais os testes novos dos managed beans.

## 11. Riscos e mitigação

| Risco | Mitigação |
|---|---|
| Regressão na API REST durante a remoção do Boot | Migração incremental com `mvn clean verify` a cada passo (seção 5); qualquer quebra é isolada a um commit específico e reversível |
| Configuração manual de `DataSource`/JPA divergir do comportamento do Boot em algum detalhe (ex.: `open-in-view`, pool sizing) | Replicar explicitamente os valores hoje default do Boot que o projeto depende (`open-in-view: false` já é setado manualmente no `application.yml` atual — outros defaults do Boot precisam ser auditados e replicados um a um) |
| Migrations Oracle divergirem do schema H2/Postgres (tipos, tamanhos) | `ddl-auto: validate` (mantido) força o Hibernate a acusar qualquer divergência de schema na subida — mesma rede de segurança que já existe hoje |
| Ambiente de desenvolvimento ficar mais pesado (Tomcat externo + Oracle em Docker) | Documentar claramente no README os dois setups (H2 rápido para dia a dia de código; Oracle/Tomcat para validar o cenário "prod"-equivalente) |

## 12. Fora de escopo

CRUD JSF de outras entidades além de `Produto`; WildFly (fica só Tomcat, citado no guia como alternativa); CI/CD; deploy em nuvem; Testcontainers para Oracle nos testes automatizados.
