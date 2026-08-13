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

> **⚠️ Importante:** O valor de `JWT_SECRET` definido no `docker-compose.yml` é uma chave de demonstração/desenvolvimento. Antes de qualquer deploy em produção, substitua esse valor por uma chave segura gerada aleatoriamente.

## Usuário de seed

| Email | Senha | Papel |
|---|---|---|
| `admin@estoque.com` | `admin123` | `ADMIN` |

> **⚠️ Importante:** essa conta e senha são exclusivamente para desenvolvimento local. Não as utilize como estão se o `docker compose`/perfil `prod` for apontado para um ambiente real acessível pela internet — troque a senha (e idealmente o e-mail) antes de qualquer deploy.

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

### Paginação

Os endpoints de listagem (GET) retornam um `PagedModel` com a seguinte estrutura:

```json
{
  "content": [
    { "id": 1, "nome": "Produto A", ... },
    { "id": 2, "nome": "Produto B", ... }
  ],
  "page": {
    "size": 20,
    "number": 0,
    "totalElements": 150,
    "totalPages": 8
  }
}
```

Use os query parameters `page` (0-indexed) e `size` para controlar a paginação: `GET /api/v1/produtos?page=1&size=20`.

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
