# Design — Migração para Spring Boot: Sistema de Controle de Estoque

Data: 2026-08-13

## 1. Contexto

O projeto atual é uma aplicação Java puro (sem build tool, sem Maven/Gradle), estruturada em MVC de console (`src/models`, `src/views`, `src/controllers`), com dados mantidos em listas em memória — nada é persistido entre execuções. Não há testes, não há tratamento de concorrência, e o modelo de domínio tem bugs:

- `Saida` não estende `Fluxo` (diferente de `Entrada`), então não tem `produto`, `funcionario` nem `quantidade` — uma saída de estoque não referencia o que saiu.
- `Fornecedor.endere�o` está com encoding corrompido.
- `Funcionario.Setor` viola convenção de nomenclatura Java (campo com maiúscula inicial).
- Não existe validação de saldo: nada impede registrar uma saída maior que o estoque disponível.
- `Entrada`/`Saida` geram `id` com `new Random().nextInt(1000)` — colisões são possíveis.

O objetivo deste projeto é reescrever a aplicação como uma API REST Spring Boot, corrigindo esses problemas, adicionando persistência real, segurança, testes e documentação — com foco em qualidade de arquitetura para avaliação em processo seletivo.

## 2. Escopo

Reescrita completa. Todo o conteúdo atual de `src/`, `bin/`, `.classpath`, `.project`, `.metadata` será removido e substituído pela estrutura Maven padrão. Fora de escopo: frontend (SPA/Thymeleaf), deploy em nuvem, CI/CD (pode ser sugerido no README como próximo passo, mas não implementado).

## 3. Stack técnica

| Categoria | Escolha |
|---|---|
| Linguagem/Runtime | Java 17 (LTS) |
| Framework | Spring Boot 3.3.x |
| Build | Maven |
| Web | Spring Web (REST, `spring-boot-starter-web`) |
| Persistência | Spring Data JPA + Hibernate |
| Banco (dev/test) | H2 em memória |
| Banco (prod) | PostgreSQL 16 (via Docker Compose) |
| Migrations | Flyway |
| Segurança | Spring Security 6 + JWT (`jjwt`) |
| Documentação de API | springdoc-openapi (Swagger UI) |
| Validação | Bean Validation (Jakarta) |
| Mapeamento DTO↔Entity | MapStruct |
| Boilerplate | Lombok (apenas em entidades/DTOs — nunca em regra de negócio) |
| Testes | JUnit 5, Mockito, MockMvc, H2 |

## 4. Arquitetura em camadas

```
com.estoque
 ├─ config/          SecurityConfig, OpenApiConfig, JpaAuditingConfig
 ├─ controller/       @RestController — só orquestra request→service→response
 ├─ dto/
 │   ├─ request/      records com Bean Validation
 │   └─ response/     records
 ├─ entity/           @Entity — construtores, sem lógica de apresentação
 ├─ enums/            Role, TipoMovimento, MotivoSaida
 ├─ exception/        hierarquia de exceptions + GlobalExceptionHandler (ProblemDetail)
 ├─ mapper/           interfaces MapStruct
 ├─ repository/       interfaces Spring Data JPA
 ├─ security/         JwtAuthFilter, JwtService, UserDetailsServiceImpl
 ├─ service/
 │   ├─ *Service.java        (interface — contrato)
 │   └─ impl/*ServiceImpl.java
 └─ EstoqueApplication.java
```

Regra de dependência: `controller → service → repository`. Controllers nunca acessam repository diretamente. Services nunca conhecem `HttpServletRequest`/DTOs de request — recebem parâmetros de domínio ou DTOs de entrada já validados e devolvem entidades ou DTOs de saída (a conversão fica no mapper, chamada pelo controller ou por um facade fino no service, decisão tomada por camada durante a implementação, mantendo consistência).

## 5. Boas práticas aplicadas (atendendo ao pedido de atenção redobrada)

**Injeção de dependência:** exclusivamente via construtor (`private final` + `@RequiredArgsConstructor` do Lombok, ou construtor explícito onde não usar Lombok). Nunca `@Autowired` em campo — garante imutabilidade e testabilidade (mock via construtor nos testes unitários).

**Construtores de entidade:** entidades JPA têm construtor protegido/no-args exigido pelo Hibernate (`@NoArgsConstructor(access = AccessLevel.PROTECTED)`) e um construtor público explícito (ou builder `@Builder`) para criação válida no código — nunca dependem de setters soltos para montar um objeto em estado válido.

**Anotações Spring/JPA usadas com intenção:**
- `@RestController` + `@RequestMapping("/api/v1/...")` por recurso.
- `@Valid` nos `@RequestBody` de entrada; `@Validated` em controllers que validam `@RequestParam`/`@PathVariable`.
- `@Transactional` nos métodos de service que escrevem; `@Transactional(readOnly = true)` nas consultas (permite otimizações do Hibernate e deixa a intenção explícita).
- `@PreAuthorize("hasRole('ADMIN')")` nos endpoints administrativos; `@EnableMethodSecurity` na config.
- `@ManyToOne(fetch = FetchType.LAZY)` em todas as associações — evita N+1 silencioso e carregamento involuntário de grafo inteiro.
- `@Embeddable`/`@Embedded` para `Endereco` (value object, não é uma entidade com identidade própria).
- `@EnumType.STRING` em todos os enums persistidos (nunca ORDINAL — evita corrupção de dados se a ordem do enum mudar).
- `@CreatedDate`/`@LastModifiedDate` + `@EntityListeners(AuditingEntityListener.class)` + `@EnableJpaAuditing` para auditoria automática de `criadoEm`/`atualizadoEm`.
- `@Version` em `Produto` para lock otimista (ver seção de concorrência).
- `@UniqueConstraint` nas colunas de negócio únicas (CNPJ, CPF, email, código de produto).

## 6. Tratamento de exceções

Hierarquia própria em `exception/`:
- `BusinessException` (abstrata) → `RecursoNaoEncontradoException` (404), `EstoqueInsuficienteException` (409), `RegistroDuplicadoException` (409), `CredenciaisInvalidasException` (401).

`GlobalExceptionHandler` com `@RestControllerAdvice`, usando **`ProblemDetail`** (RFC 7807, nativo do Spring 6/Boot 3 — abordagem moderna, substitui corpo de erro ad-hoc) para padronizar toda resposta de erro: `type`, `title`, `status`, `detail`, `instance`, mais propriedades extras (`errors: [{campo, mensagem}]` em erros de validação, `timestamp`).

Mapeamento:
| Exceção | Status |
|---|---|
| `RecursoNaoEncontradoException` | 404 |
| `EstoqueInsuficienteException` | 409 |
| `RegistroDuplicadoException` / `DataIntegrityViolationException` | 409 |
| `MethodArgumentNotValidException` | 400 (com lista de campos inválidos) |
| `ObjectOptimisticLockingFailureException` | 409 |
| `AuthenticationException` | 401 |
| `AccessDeniedException` | 403 |
| Exceção não mapeada | 500 (sem vazar stacktrace) |

## 7. Concorrência

Cenário real: dois operadores registram saída do mesmo produto ao mesmo tempo — sem controle, o estoque pode ficar negativo ou inconsistente (lost update).

- `Produto.quantidadeEstoque` protegido por **lock otimista** (`@Version` em campo `long`). Ao atualizar concorrentemente, o segundo commit lança `ObjectOptimisticLockingFailureException`, tratada como 409 — o cliente reenvia a operação.
- Toda a operação de "ler saldo → validar → decrementar/incrementar → salvar" ocorre dentro de um único método `@Transactional` no service (nunca dividida entre controller e service), garantindo atomicidade da transação de banco.
- Validação de saldo (`EstoqueInsuficienteException`) acontece **dentro** da transação, imediatamente antes de persistir a saída, não numa checagem separada anterior — evita race condition do tipo check-then-act.

## 8. Organização da API REST

Prefixo de versão: `/api/v1`. Recursos como substantivos no plural, verbos HTTP com semântica correta, status codes corretos (`201 Created` + header `Location` em criação, `204 No Content` em delete, `200 OK` em leitura/atualização, `400/401/403/404/409/422` conforme a falha).

```
POST   /api/v1/auth/login                        → login, retorna JWT

GET    /api/v1/produtos            (paginado, filtros: nome, categoriaId, colecaoId, estoqueBaixo)
GET    /api/v1/produtos/{id}
POST   /api/v1/produtos                           [ADMIN]
PUT    /api/v1/produtos/{id}                       [ADMIN]
DELETE /api/v1/produtos/{id}        (soft delete)  [ADMIN]
GET    /api/v1/produtos/{id}/movimentacoes         (kardex paginado)
GET    /api/v1/produtos/estoque-baixo

GET    /api/v1/categorias | POST | PUT /{id} | DELETE /{id}     [ADMIN em escrita]
GET    /api/v1/colecoes   | POST | PUT /{id} | DELETE /{id}     [ADMIN em escrita]

GET    /api/v1/fornecedores | GET /{id} | POST | PUT /{id} | DELETE /{id}   [ADMIN em escrita]

GET    /api/v1/funcionarios | GET /{id} | POST | PUT /{id} | DELETE /{id}   [ADMIN]
GET    /api/v1/funcionarios/me                     (perfil do usuário autenticado)

POST   /api/v1/entradas            [ADMIN, OPERADOR]
GET    /api/v1/entradas            (paginado, filtro produtoId/data)

POST   /api/v1/saidas              [ADMIN, OPERADOR]
GET    /api/v1/saidas              (paginado, filtro produtoId/data)

GET    /api/v1/relatorios/valor-estoque
GET    /api/v1/relatorios/estoque-baixo
```

Todas as listagens retornam `Page<T>` do Spring Data (`content`, `totalElements`, `totalPages`, `number`, `size`). Todos os corpos de request de escrita são `record`s com Bean Validation; respostas nunca expõem a entidade JPA diretamente (sempre DTO de resposta), evitando vazamento de lazy proxies e acoplamento da API ao modelo de persistência.

## 9. Modelo de domínio

- **Funcionario** — id, nome, sobrenome, cpf (único), email (único, login), senha (hash BCrypt), matricula, dataAdmissao, setor, cargo (`Role`: ADMIN/OPERADOR), endereco (embedded), ativo, criadoEm, atualizadoEm. Implementa `UserDetails` (ou tem um adapter dedicado — decisão de implementação).
- **Fornecedor** — id, cnpj (único, validado), razaoSocial, telefone, email, endereco (embedded), ativo, criadoEm, atualizadoEm.
- **Categoria** — id, nome (único), descricao.
- **Colecao** — id, nome (único), descricao.
- **Produto** — id, codigo (único), nome, descricao, categoria (ManyToOne), colecao (ManyToOne), fornecedor (ManyToOne), precoVenda, custoMedio, quantidadeEstoque, estoqueMinimo, version (lock otimista), ativo, criadoEm, atualizadoEm.
- **MovimentacaoEstoque** (`@MappedSuperclass` ou herança `JOINED` — decisão de implementação, provavelmente `JOINED` para permitir queries polimórficas no kardex) — id, produto (ManyToOne), funcionario (ManyToOne, quem registrou), quantidade, dataMovimentacao, observacao.
  - **Entrada** — custoUnitario, fornecedor (ManyToOne).
  - **Saida** — motivo (`MotivoSaida`: VENDA/PERDA/AJUSTE/DEVOLUCAO).
- **Endereco** (`@Embeddable`) — logradouro, numero, bairro, cidade, estado (UF), cep.

## 10. Segurança

`POST /api/v1/auth/login` valida email+senha (BCrypt), retorna JWT (expiração configurável). `JwtAuthFilter` (OncePerRequestFilter) valida o token em cada request e popula o `SecurityContext`. Rotas de escrita administrativa exigem `ADMIN`; registrar entrada/saída exige `ADMIN` ou `OPERADOR`; leitura exige apenas autenticação. `SecurityConfig` desabilita CSRF (API stateless), define sessão `STATELESS`, e expõe `/swagger-ui/**`, `/v3/api-docs/**` e `/api/v1/auth/login` publicamente.

## 11. Dados de seed (Flyway)

`V1__schema.sql` cria todas as tabelas. `V2__seed.sql` insere: um funcionário ADMIN (`admin@estoque.com` / senha documentada no README), 3 categorias e 2 coleções de exemplo — permite testar a API imediatamente após subir a aplicação, sem cadastro manual prévio.

## 12. Testes

- **Unitários (service, Mockito):** regra de estoque insuficiente, cálculo de saldo após entrada/saída, validação de duplicidade (CNPJ/CPF/email/código), autorização por role.
- **Integração (controller, MockMvc + H2, perfil `test`):** fluxo HTTP completo de cada recurso — criação (201+Location), leitura paginada, atualização, delete (204), e os principais casos de erro (400 validação, 401 sem token, 403 role errada, 404 não encontrado, 409 estoque insuficiente/conflito de versão).
- Sem Testcontainers — mantém `mvn test` executável em qualquer máquina sem Docker.

## 13. Entregáveis de documentação

README.md reescrito: badges (Java, Spring Boot, licença), visão geral, stack, arquitetura (diagrama de camadas em texto/mermaid), como rodar localmente (Maven, H2) e via Docker Compose (Postgres), credenciais de seed, exemplos de request/response, link para Swagger UI, estrutura de pastas, decisões de design (por que ProblemDetail, por que lock otimista, etc.), roadmap de próximos passos (CI, deploy).

## 14. Fora de escopo / não incluído

Frontend, CI/CD, deploy em nuvem, cache (Redis), mensageria, multi-tenancy, internacionalização de mensagens de erro.
