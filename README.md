# Controle de Estoque — API REST

API REST em Spring Boot para controle de estoque: produtos, categorias, coleções, fornecedores, funcionários e movimentações de entrada/saída, com autenticação JWT, controle de concorrência e documentação OpenAPI.

> Projeto desenvolvido durante a graduação, na disciplina de Desenvolvimento Orientado a Objetos.

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
mvn clean package cargo:run
```

A aplicação sobe num Tomcat 10.1 embarcado via Cargo, em `http://localhost:8080/controle-estoque`,
com banco H2 em memória e dados de seed já carregados.

- API REST: `http://localhost:8080/controle-estoque/api/v1/**`
- Telas JSF: `http://localhost:8080/controle-estoque/faces/produtos.xhtml`

## Como rodar com Oracle (Docker Compose)

```bash
docker compose up --build
```

Sobe Oracle XE + a aplicação no perfil `oracle`, executando migrations Flyway específicas para Oracle. A primeira subida do Oracle pode levar alguns minutos.

> **Nota:** O perfil Oracle foi implementado e testado estaticamente contra o schema JPA, mas não pôde ser testado com uma instância Oracle de fato neste ambiente (Docker não está disponível aqui). A configuração está pronta para uso em ambientes com Docker.

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

## Mapa de estudo — do guia técnico para o código

Cada linha aponta o arquivo e a linha onde o conceito está demonstrado. A ideia é ler a seção do guia e abrir o arquivo correspondente na sequência.

| Conceito | Onde ver | O que observar |
|---|---|---|
| Camadas: entrada → regra → dados | `controller/ProdutoController.java:32` → `service/impl/ProdutoServiceImpl.java:41` → `repository/ProdutoRepository.java:18` | O service não sabe quem o chamou. É por isso que uma segunda porta de entrada (uma tela, por exemplo) reaproveitaria a mesma regra sem reescrita. |
| Injeção por construtor | `service/impl/ProdutoServiceImpl.java:32-38` | Sete dependências, todas `private final`, um único construtor (gerado por `@RequiredArgsConstructor`) — logo, sem `@Autowired`. Não existe injeção em campo em nenhum ponto do projeto. |
| `@Value` lendo configuração | `security/JwtService.java:20` | Segredo e expiração vêm do `application.yml`, não hardcoded. |
| `@Transactional` na escrita | `service/impl/ProdutoServiceImpl.java:61` | Anotado no método público do service que representa a operação inteira — nunca no controller, nunca no repository. |
| `@Transactional(readOnly = true)` | `service/impl/ProdutoServiceImpl.java:41` | Toda consulta do projeto usa. Desliga o dirty checking. |
| Regra de negócio na entidade | `entity/Produto.java:111` e `:122` | `registrarEntrada`/`registrarSaida` — o service orquestra, a entidade decide. |
| N+1 e a solução declarativa | `repository/ProdutoRepository.java:18` e `:21` | `@EntityGraph` é o equivalente Spring Data do `JOIN FETCH`: uma consulta em vez de 1+N. Mesmo padrão em `EntradaRepository`, `SaidaRepository` e `MovimentacaoEstoqueRepository`. |
| `FetchType.LAZY` explícito | `entity/Produto.java:49`, `:53`, `:57` | Todo `@ManyToOne` é LAZY aqui, contrariando o padrão EAGER da JPA — por isso o `@EntityGraph` acima é necessário. |
| `LazyInitializationException` (terreno) | `application.yml:12` (`open-in-view: false`) | Com a sessão fechando no fim do service, tocar um LAZY no controller estoura. Ver Experimento 2. |
| `@Enumerated(STRING)` | `entity/Funcionario.java:67`, `entity/Saida.java:22` | Grava `VENDA`, não `0`. Ver Experimento 5 para o que acontece com ORDINAL. |
| Herança `JOINED` | `entity/MovimentacaoEstoque.java:25` | Entrada e Saída em uma consulta polimórfica só (o kardex). |
| Lock otimista e o `409` | `entity/Produto.java:75` (`@Version`) + `service/impl/SaidaServiceImpl.java:46` | O bloco de comentário nas linhas 38-44 explica o *lost update* que o `@Version` impede. Prova executável em `SaidaConcorrenciaIT.java:37`. |
| REST: verbos e `201` + `Location` | `controller/ProdutoController.java:47-52` | `ResponseEntity.created(URI.create(...))` — os sete controllers seguem o mesmo padrão. |
| **401 vs 403** | `security/RestAuthenticationEntryPoint.java:23` (401) e `security/RestAccessDeniedHandler.java:23` (403) | Duas classes separadas, plugadas em `config/SecurityConfig.java:69-71`. É a distinção que o guia marca como a mais errada em entrevista. Ver Experimento 6. |
| `409 Conflict` | `exception/GlobalExceptionHandler.java:37` e `:81` | Estoque insuficiente e conflito de lock otimista. |
| `@RestControllerAdvice` centralizando erro | `exception/GlobalExceptionHandler.java:27` | Nenhum `try/catch` de apresentação espalhado pelos controllers. O comentário na linha 49 registra uma armadilha real de `@ExceptionHandler` ambíguo. |
| DTO em vez de `@Entity` no endpoint | `dto/request/ProdutoRequest.java` | O record não tem `id`, `quantidadeEstoque` nem `custoMedio`: o cliente não consegue forjar saldo. Saldo só muda por entrada/saída. |
| `@Valid` disparando Bean Validation | `controller/ProdutoController.java:49` | Erros viram `400` com os campos, em `GlobalExceptionHandler.java:56`. |
| `BigDecimal` com escala explícita | `entity/Produto.java:117` | `setScale(2, RoundingMode.HALF_UP)` no custo médio ponderado. |
| `equals`/`hashCode` pela chave de negócio | `entity/Produto.java:32` e `:40` (mesmo padrão em `Categoria`, `Colecao`, `Fornecedor`, `Funcionario`) | `@EqualsAndHashCode(onlyExplicitlyIncluded = true)` do Lombok, incluindo só o campo de negócio — nunca todos os campos, nunca o `id`. Testado em `entity/ProdutoTest.java`; ver Experimento 7 para o efeito de remover. |
| Ciclo de vida do JSF | `jsf/ProdutoFormBean.java:criar()` + `produto-form.xhtml` | Submeter o form sem preencher `codigo` (campo `required="true"`) força o desvio da fase 3 (Process Validations) direto pra fase 6 (Render Response) — a action `criar()` nunca roda. |
| Escopos de managed bean | `jsf/ProdutoListBean.java` (`@ViewScoped`) | Paginar (Próxima/Anterior) sem perder a página atual — sobrevive ao postback. Ver Experimento novo abaixo pra ver o bug ao trocar pra `@RequestScoped`. |
| Spring beans dentro de managed beans JSF | `jsf/ProdutoListBean.java:iniciar()` | `SpringBeanAutowiringSupport.processInjectionBasedOnServletContext(this, servletContext)` — a ponte entre o container CDI (Weld) e o `ApplicationContext` do Spring, sem `SpringBeanFacesELResolver`. |
| Oracle real (não só teórico) | `db/migration/oracle/V4__schema_produto.sql` vs. `db/migration/h2/V4__schema_produto.sql` | Os dois arquivos lado a lado mostram o diff de sintaxe do guia (seção 11) acontecendo de verdade no mesmo projeto: `BIGINT`→`NUMBER(19)`, `VARCHAR`→`VARCHAR2`, `BOOLEAN`→`NUMBER(1,0)`. |

### O que este projeto **não** demonstra

Estas seções do guia não têm contrapartida no código — estude-as pela teoria, sem procurar no repositório:

- **Spring tradicional em WAR**: aqui é Spring Boot com jar executável e Tomcat embarcado.
- **Lock pessimista** (`SELECT ... FOR UPDATE`): o projeto resolve concorrência com lock **otimista**. Vale saber justificar a escolha — está em "Decisões de design".
- **`REQUIRES_NEW` e `rollbackFor`**: não há caso de uso no projeto. O Experimento 4 força o cenário de rollback para você ver o commit indevido acontecer.

## Experimentos — quebre de propósito

O conceito gruda quando você vê o bug acontecer. Todos são reversíveis com `git checkout -- <arquivo>`; nenhum altera o banco de forma permanente (H2 é em memória).

Antes de começar, ligue o log de SQL — vários experimentos dependem de **contar consultas**:

```yaml
# application.yml
logging:
  level:
    org.hibernate.SQL: DEBUG
```

> **Reconstrua com `mvn clean package` (ou `mvn clean spring-boot:run`), não apenas `package`/`spring-boot:run`.** Build incremental neste projeto (MapStruct + Lombok gerando código a cada compilação) pode deixar classes de mapper desatualizadas e produzir um erro do Hibernate sem nenhuma relação com o experimento que você está rodando — se aparecer algo estranho, primeiro tente `mvn clean package` antes de desconfiar do próprio código.

### Experimento 1 — fazer o N+1 aparecer

1. Em `repository/ProdutoRepository.java:18`, comente a linha `@EntityGraph(...)` do método paginado.
2. Cadastre produtos apontando para **categorias diferentes** — esse detalhe importa (ver "cuidado" abaixo).
3. Suba a aplicação e chame `GET /api/v1/produtos?size=20`.
4. Conte quantas linhas `org.hibernate.SQL` aparecem no log entre a chamada e a resposta.

**Esperado:** o total sobe de 2 consultas (uma de contagem, uma da página) para 2 + o número de categorias/coleções **distintas** referenciadas pelos produtos da página — uma consulta lazy por associação ainda não carregada na sessão. Numa medição feita durante o preparo deste guia: 8 produtos com 9 categorias distintas e sem coleção/fornecedor geraram **12 consultas** contra as 2 esperadas com `@EntityGraph`.

**Cuidado com o resultado enganoso:** se todos os produtos de teste apontarem para a *mesma* categoria (por exemplo, o seed padrão), o N+1 quase desaparece — o Hibernate reaproveita a entidade já carregada na sessão (cache de primeiro nível) e você vê só 4 consultas em vez de dezenas. O bug é real, mas só fica visível com dado variado — o que é justamente o retrato de produção depois de algumas semanas de uso, e o motivo de passar despercebido em ambiente de teste com poucos registros.

**Âncora que treina:** *LAZY na listagem vira consulta por linha · JOIN FETCH resolve.*

### Experimento 2 — provocar a `LazyInitializationException`

1. Em `service/impl/ProdutoServiceImpl.java:47`, troque o retorno de `buscarPorId` para devolver a entidade `Produto` crua em vez do DTO (e ajuste o controller para chamar `produto.getCategoria().getNome()`).
2. Chame `GET /api/v1/produtos/1`.

**Esperado:** `LazyInitializationException`. A transação termina no fim do método do service e, com `open-in-view: false` (`application.yml:12`), a sessão fecha junto — o acesso ao LAZY acontece tarde demais. Ligue `open-in-view: true` e veja o erro sumir: é exatamente por isso que muito projeto legado mantém essa configuração ligada, mascarando N+1 na camada de tela.

**Âncora que treina:** *Acessou o LAZY fora da transação e a sessão já fechou.*

### Experimento 3 — a anotação `@Transactional` ignorada

1. Em `service/impl/ProdutoServiceImpl.java`, adicione:

```java
@Transactional(propagation = Propagation.REQUIRES_NEW)
public void metodoInterno(Produto p) {
    System.out.println("tx ativa: " +
        org.springframework.transaction.support.TransactionSynchronizationManager
            .isActualTransactionActive());
    System.out.println("nome da tx: " +
        org.springframework.transaction.support.TransactionSynchronizationManager
            .getCurrentTransactionName());
}
```

2. Chame `this.metodoInterno(produto)` de dentro de `criar` (linha 61) e crie um produto.
3. Depois, mova `metodoInterno` para um `@Service` novo, injete-o por construtor e chame de novo.

**Esperado:** na chamada interna, o nome da transação continua sendo o de `criar` — o `REQUIRES_NEW` foi silenciosamente ignorado, porque a chamada não passou pelo proxy. Movido para outro bean, o nome muda: transação nova de verdade.

**Âncora que treina:** *Chamada de fora passa pelo proxy · Chamada interna não · Solução: mover pra outro bean.*

### Experimento 4 — o commit que não deveria ter acontecido

1. Em `service/impl/SaidaServiceImpl.java`, depois do `produtoRepository.save(produto)` da linha 58, lance uma exceção **checada**: `if (true) throw new java.io.IOException("falha simulada");` (declare `throws IOException` no método).
2. Registre uma saída e depois consulte `GET /api/v1/produtos/{id}`.

**Esperado:** o estoque foi decrementado e **nenhuma `Saida` foi gravada** — o rollback não aconteceu, porque por padrão o Spring só reverte em `RuntimeException`. Agora troque para `@Transactional(rollbackFor = Exception.class)` e repita: o estoque volta ao valor original.

Variante que vale rodar logo em seguida: capture a exceção dentro do método com um `try/catch` vazio. Sem relançar, não existe rollback nenhum — o commit sai com dado pela metade, e o log fica limpo.

**Âncora que treina:** *Rollback só em unchecked · Capturou e não relançou, não tem rollback.*

### Experimento 5 — `@Enumerated` ORDINAL

1. Em `entity/Saida.java:22`, troque `EnumType.STRING` por `EnumType.ORDINAL`.
2. Suba a aplicação.

**Esperado:** a aplicação **nem sobe**. Com `ddl-auto: validate`, o Hibernate compara o mapeamento com o schema real e encontra `motivo VARCHAR(20)` (`V5__schema_movimentacao.sql:18`) onde ORDINAL exigiria um inteiro. É o melhor cenário possível: falha na subida, não em produção. Em um projeto sem `validate`, o mesmo erro passaria batido até alguém inserir um valor no meio do enum e corromper o histórico inteiro.

**Âncora que treina:** *Sempre STRING, nunca ORDINAL.*

### Experimento 6 — 401 contra 403

Sem alterar código nenhum:

```bash
# 401 — não sei quem você é
curl -i http://localhost:8080/api/v1/produtos

# login como ADMIN e criação de um OPERADOR
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@estoque.com","senha":"admin123"}' | jq -r .token)

curl -s -X POST http://localhost:8080/api/v1/funcionarios \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"nome":"Op","sobrenome":"Teste","cpf":"99988877766","email":"op@estoque.com",
       "senha":"operador123","matricula":"F002","dataAdmissao":"2026-01-10",
       "setor":"Almoxarifado","role":"OPERADOR",
       "endereco":{"logradouro":"Rua B","numero":"2","bairro":"Centro",
                    "cidade":"Curitiba","estado":"PR","cep":"80000-000"}}'

# 403 — sei quem você é, e você não pode criar produto
OP=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"op@estoque.com","senha":"operador123"}' | jq -r .token)

curl -i -X POST http://localhost:8080/api/v1/produtos \
  -H "Authorization: Bearer $OP" -H "Content-Type: application/json" \
  -d '{"codigo":"X-1","nome":"Teste","categoriaId":1,"precoVenda":10.00,"estoqueMinimo":1}'
```

**Esperado:** `401` no primeiro (sem token, `RestAuthenticationEntryPoint`), `403` no último (token válido, papel insuficiente, `RestAccessDeniedHandler`) — os dois com corpo `ProblemDetail`. O mesmo OPERADOR consegue registrar saídas: `POST /api/v1/saidas` responde `201`.

**Âncora que treina:** *401 não sei quem você é · 403 sei, e você não pode.*

### Experimento 7 — o `Set` que aceita duplicado (e como a correção quebra de novo)

`Categoria`, `Colecao`, `Fornecedor`, `Funcionario` e `Produto` implementam `equals`/`hashCode` pela chave de negócio (`nome`, `cnpj`, `cpf`, `codigo`) via `@EqualsAndHashCode(onlyExplicitlyIncluded = true)` do Lombok — veja `entity/Produto.java:32` e `entity/Produto.java:40`, com o teste correspondente em `test/.../entity/ProdutoTest.java`.

1. Rode `mvn -Dtest=ProdutoTest test`: passa. Dois `Produto` com o mesmo `codigo` (nomes e preços diferentes) são `equals`, têm o mesmo `hashCode`, e um `HashSet` com os dois tem tamanho `1`.
2. Agora comente a linha `@EqualsAndHashCode(onlyExplicitlyIncluded = true)` em `entity/Produto.java:32`.
3. Rode `mvn clean -Dtest=ProdutoTest test` de novo (o `clean` é necessário — ver o aviso no topo desta seção).

**Esperado:** o teste falha. Sem a anotação, `equals` volta a ser identidade de referência (`Object.equals`) e o `HashSet` aceita os dois objetos como distintos — o mesmo produto de negócio duplicado. É a mesma lacuna que o guia descreve para entidades que não implementam os dois métodos.

**Âncora que treina:** *HashSet e HashMap dependem de equals e hashCode.*

### Experimento 8 — `BigDecimal`, as duas pegadinhas

```java
0.1 + 0.2                                          // 0.30000000000000004
new java.math.BigDecimal(0.1)                      // 0.1000000000000000055511151231257827…
new java.math.BigDecimal("0.1")                    // 0.1
new java.math.BigDecimal("10.0").equals(new java.math.BigDecimal("10.00"))     // false
new java.math.BigDecimal("10.0").compareTo(new java.math.BigDecimal("10.00"))  // 0
```

**Esperado:** o `equals` dá `false` porque compara a escala junto; `compareTo` dá `0`. Em conciliação financeira, usar `equals` é como o valor "não bater" por causa de um zero à direita.

**Âncora que treina:** *Dinheiro nunca é double · Construa com String · Compare com compareTo.*

### Experimento 9 — desligar o lock otimista e ver o overselling

1. Comente `@Version` em `entity/Produto.java:75` (o campo pode ficar; só a anotação sai).
2. Rode `mvn -Dtest=SaidaConcorrenciaIT test`.

**Esperado:** o teste **falha**. Ele dispara 5 threads pedindo 3 unidades cada sobre um estoque de 10 e afirma que o total vendido nunca ultrapassa o disponível (`SaidaConcorrenciaIT.java:84`). Sem `@Version`, as threads leem o mesmo saldo e sobrescrevem umas às outras — *lost update* clássico, e o estoque fica negativo. A coluna `version` tem `DEFAULT 0` na migration, então nada quebra no banco: o bug é puramente de concorrência.

**Âncora que treina:** *Lock otimista: a segunda gravação falha em vez de sobrescrever.*

### Experimento 10 — `@ViewScoped` virando `@RequestScoped`

1. Em `jsf/ProdutoListBean.java`, trocar `import jakarta.faces.view.ViewScoped;` por `import jakarta.enterprise.context.RequestScoped;`, e a anotação `@ViewScoped` por `@RequestScoped`.
2. Rodar `mvn clean package cargo:run`, acessar `/faces/produtos.xhtml`, clicar em "Próxima".

**Esperado:** a tabela aparece vazia (ou volta pra página 0) a cada clique — o bean é recriado do zero a cada requisição, perdendo `paginaAtual`. É exatamente a "pergunta clássica de gestor" do guia: "a lista da tela some quando o usuário clica no botão de filtrar".

**Âncora que treina:** *Request morre na requisição · View sobrevive ao postback.*

## Roadmap

- Pipeline de CI (build + testes a cada push)
- Deploy automatizado (ex: Railway/Render)
- Cache de leitura para relatórios
