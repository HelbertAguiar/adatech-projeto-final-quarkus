# Aula 04 — MicroProfile, métricas, tolerância a falhas e GraalVM

> **Guia do professor com explicações, roteiro prático, perguntas com gabarito e laboratório Docker Desktop.**  
> Baseado no projeto **`catalogo-api`**, Java **21**, Quarkus **3.40.1**. Não é necessário criar mais um projeto Quarkus.

## 0. Objetivo, pré-requisitos e contexto

**Pergunta norteadora:** como evoluir a API de catálogo para operar como um microserviço independente, configurável, observável, resiliente a falhas de rede e com possibilidade de compilação nativa?

Ao final da aula, os alunos deverão: (1) explicar a arquitetura com dois processos; (2) aplicar MicroProfile Config e Health; (3) demonstrar `@Retry`/`@Fallback`; (4) diferenciar MicroProfile Metrics de Micrometer; (5) observar métricas em `/q/metrics`; (6) produzir e testar uma Native Image com Docker Desktop.

**Base das aulas anteriores:** `ProductResource` → `ProductService` → `EstoqueGateway` (REST Client) → `estoque-local` (`HttpServer` do JDK). Porta da API: **9091**; do estoque: **8081**.

```text
Cliente / curl / navegador
           |
           v
  catalogo-api (Quarkus :9091)
    ├── GET /info            (MicroProfile Config)
    ├── GET /products        (Resource → Service → REST Client)
    │                           |
    │                @Retry + @Fallback
    │                           |
    │                           +-- HTTP GET --> estoque-local (:8081)
    ├── /q/health/live      (MicroProfile Health / Liveness)
    ├── /q/health/ready     (MicroProfile Health / Readiness)
    └── /q/metrics          (Micrometer / Prometheus)

Mesmo código Quarkus → JVM (JAR) OU binário nativo (GraalVM/Mandrel).
```

**Mensagem didática:** executar dois processos distintos e comunicar-se por HTTP é o começo da discussão de microserviços, mas não significa automaticamente que já existem deploys independentes, service discovery, escala horizontal, autenticação, observabilidade distribuída e resiliência de produção.

### Sequência expositiva sugerida

| Bloco | Tempo indicativo | Demonstração |
|---|---:|---|
| 1. Arquitetura e configuração | 10–15 min | `/info`, troca de propriedade |
| 2. Health checks | 10 min | `/q/health/live`, `/ready` |
| 3. Tolerância a falhas | 15 min | `/products`, desligar estoque, 503 |
| 4. Métricas | 10 min | `MeterRegistry`, `/q/metrics` |
| 5. Native Image | 15 min + compilação | Docker Desktop, binário e imagem |
| 6. Laboratório/pesquisa | conforme tempo | atividade individual com gabarito abaixo |

---

## 1. O que foi instalado no `pom.xml` para a Aula 04?

O projeto importa o **BOM** `io.quarkus.platform:quarkus-bom:3.40.1` em `<dependencyManagement>`. O BOM centraliza as versões compatíveis das extensões Quarkus. Por isso, as dependências Quarkus abaixo não precisam de `<version>` explícito.

### 1.1 Dependências adicionadas

```xml
<!-- Health checks compatíveis com MicroProfile Health -->
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-smallrye-health</artifactId>
</dependency>

<!-- Resiliência compatível com MicroProfile Fault Tolerance -->
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-smallrye-fault-tolerance</artifactId>
</dependency>

<!-- Micrometer + exportador Prometheus -->
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-micrometer-registry-prometheus</artifactId>
</dependency>
```

| Extensão | O que oferece | Onde é usada |
|---|---|---|
| `quarkus-smallrye-health` | `HealthCheck`, `HealthCheckResponse`, `@Liveness`, `@Readiness`; rotas `/q/health/...` | `CatalogoLivenessCheck`, `CatalogoReadinessCheck` |
| `quarkus-smallrye-fault-tolerance` | Interceptadores CDI para `@Retry`, `@Fallback`, `@Timeout`, `@CircuitBreaker`, etc. | `ProductService` |
| `quarkus-micrometer-registry-prometheus` | Integração Micrometer + registro/exportação Prometheus, inclusive endpoint de métricas | `ProductResource` e `/q/metrics` |

**O que não foi adicionado:** não é preciso instalar uma extensão `quarkus-smallrye-config` específica para utilizar `@ConfigProperty` neste projeto. O suporte a configuração via SmallRye Config/MicroProfile Config integra a infraestrutura do Quarkus. Também **não** é necessário adicionar uma biblioteca GraalVM diretamente ao POM: o `quarkus-maven-plugin` já oferece o processo de build nativo, com GraalVM/Mandrel instalado localmente ou dentro do Docker.

### 1.2 Dependências que já existiam

- `quarkus-rest`, `quarkus-rest-jackson`: rotas Jakarta REST e JSON.
- `quarkus-arc`: CDI; cria e injeta componentes como `ProductService` e `MeterRegistry` via extensões.
- `quarkus-rest-client-jackson`: chamada HTTP ao estoque e serialização JSON (`@RegisterRestClient`).
- `quarkus-junit`, `rest-assured` (`scope=test`): teste de endpoints e inicialização do Quarkus nos testes.
- `lombok` (`scope=provided`): ferramenta de geração de código em compilação, não é exigida pela Aula 04.

### 1.3 Plugins e profile Maven `native`

- `quarkus-maven-plugin`: integra compilação, empacotamento, augmentation e construção nativa.
- `maven-compiler-plugin`: compila para Java 21 e declara o processador do Lombok.
- `maven-surefire-plugin`: testes unitários/`@QuarkusTest` (`mvn test`).
- `maven-failsafe-plugin`: testes de integração, incluindo execução contra aplicação empacotada.
- Profile Maven `native`: é ativado com **`-Dnative`** e ajusta propriedades para habilitar o build nativo (`quarkus.native.enabled=true`), desabilitar o JAR normal naquele build e permitir ITs, salvo se estes forem pulados por parâmetro.

**Não confundir:** profile **Maven** (`-Dnative`, altera como se faz o build) é diferente do profile de **configuração Quarkus** (`dev`, `test`, `prod`, `staging`, altera propriedades da aplicação).

**Pergunta e resposta:** por que o BOM é diferente de uma dependência comum? Porque `<dependencyManagement>` gerencia versões e escopos; não adiciona, por si, as bibliotecas à aplicação. É `<dependencies>` que declara o que o projeto utiliza.

---

## 2. Micrometer x MicroProfile Metrics

Ambos capturam **métricas de aplicação** e podem expor valores para monitoramento. A diferença não é “um registra métricas e o outro não”; são **APIs e modelos de integração diferentes**.

| Critério | MicroProfile Metrics (legado) | Micrometer (escolha desta aula) |
|---|---|---|
| Natureza | Especificação Eclipse MicroProfile | Biblioteca/API de instrumentação independente de framework |
| Registro central | `org.eclipse.microprofile.metrics.MetricRegistry` | `io.micrometer.core.instrument.MeterRegistry` |
| Formas de instrumentar | `@Counted`, `@Timed`, `@Gauge`, `MetricRegistry` | `Counter`, `Timer`, `Gauge`, `DistributionSummary`, `@Counted`/`@Timed` |
| Suporte a tags | Métricas dimensionais disponíveis na especificação | Tags são parte central da API e de suas integrações |
| Exportação | Depende da implementação MicroProfile | Registries para Prometheus e outros destinos |
| Quarkus contemporâneo | Integração antiga/descontinuada, não escolhida aqui | Caminho recomendado pela documentação do Quarkus |
| Spring Boot | Não é seu mecanismo padrão | É também usado pelo Spring Boot Actuator |

**Importante:** `@Counted` existe em ambos os ecossistemas, mas são classes de **pacotes diferentes**; não usar uma anotação e esperar comportamento da outra implementação. Micrometer **não é uma implementação da especificação MicroProfile Metrics**. O restante da aula pode usar MicroProfile Config, Health e Fault Tolerance, sem precisar usar MicroProfile Metrics.

**Fluxo:** código registra medições → `MeterRegistry` gerencia os instrumentos → registry Prometheus expõe `/q/metrics` → servidor Prometheus pode coletar periodicamente (se configurado) → Grafana pode visualizar. A extensão **não instala** Prometheus Server ou Grafana no computador.

### 2.1 O que é `MeterRegistry`?

É o **catálogo/registro de medidores** do Micrometer. O Quarkus injeta a instância configurada para o(s) backend(s) disponível(is); o código consulta ou registra medidores por **nome + tags**.

Código *real* do projeto, em `ProductResource`:

```java
@Inject
MeterRegistry meterRegistry;

@GET
public Response getProducts() {
    CatalogoResponse catalogo = CatalogoResponse.from(productService.getProducts());

    meterRegistry.counter("catalogo.consultas", "origem", catalogo.origem())
                 .increment();

    if ("FALLBACK".equals(catalogo.origem())) {
        return Response.status(503).entity(catalogo).build();
    }
    return Response.ok(catalogo).build();
}
```

- `@Inject`: CDI fornece a instância do registry.
- `counter("catalogo.consultas", "origem", catalogo.origem())`: **obtém ou registra** um contador identificado pelo nome e pela tag `origem`. Com estoque disponível a tag é `ESTOQUE`; no fallback é `FALLBACK`.
- `.increment()`: incrementa em 1 o contador. Também existe `increment(double amount)` para incremento positivo específico.
- O contador representa **consultas respondidas pelo endpoint**, e não o número de tentativas HTTP internas. Uma chamada que executa duas retries continua incrementando **uma vez** nesse ponto do código.
- O nome exportado para Prometheus normalmente aparece como `catalogo_consultas_total{origem="ESTOQUE",...}` ou com `origem="FALLBACK"`; nomes são normalizados segundo o registry. Antes da primeira consulta a série ainda pode não existir.

### 2.2 Principais métodos para um microserviço

| API | Mede | Exemplo | Uso típico |
|---|---|---|---|
| `registry.counter(nome, tags...).increment()` | Ocorrências acumuladas | consultas, pedidos, erros | Volume e taxa de eventos |
| `registry.timer(nome, tags...).record(...)` | Duração **e** quantidade | latência de um serviço | Tempo de resposta e frequência |
| `Timer.Sample.start(registry)` e `.stop(timer)` | Duração de bloco com início/fim explícitos | chamada externa | Medição com `try/finally` |
| `registry.gauge(nome, objeto, função)` | Valor instantâneo observado | itens na fila | Estado atual, e não total cumulativo |
| `registry.summary(nome, tags...).record(valor)` | Distribuição de quantidades, não tempo | tamanho de payload em bytes | Tamanhos e volumes |
| `registry.find(nome)` / `registry.get(nome)` | Localizar medidores existentes | inspecionar em testes | Verificação/consulta de instrumentos |

Exemplos **didáticos** — não adicionados ao código para manter a demo enxuta:

```java
// Timer: duração de uma operação; 'record' mede a execução.
registry.timer("catalogo.processamento", "operacao", "filtragem")
        .record(() -> executarFiltragem());

// Timer.Sample permite escolher as tags ao concluir.
Timer.Sample sample = Timer.start(registry);
try {
    executarChamadaExterna();
} finally {
    sample.stop(registry.timer("catalogo.estoque.latencia", "servico", "estoque"));
}

// DistributionSummary: tamanho de uma resposta, não duração.
registry.summary("catalogo.resposta.bytes").record(tamanhoDoJsonEmBytes);

// Gauge: referência longa (campo do bean) a uma variável observada.
// Exemplo de campo de um @ApplicationScoped bean:
private final AtomicInteger filaPendente = new AtomicInteger();
// No setup/inicialização:
registry.gauge("catalogo.fila.pendente", filaPendente, AtomicInteger::get);
```

*Os nomes `executarFiltragem`, `executarChamadaExterna` e `tamanhoDoJsonEmBytes` são ilustrativos, não métodos existentes no projeto.* Para um timer use `io.micrometer.core.instrument.Timer`; para o gauge use `java.util.concurrent.atomic.AtomicInteger`.

**Cuidados arquiteturais:** contadores crescem enquanto a instância vive e reiniciam ao reiniciar a aplicação (o monitoramento calcula taxas e agrega séries); `Gauge` observa um valor atual; timers contam chamadas e latências. Evite tags de alta cardinalidade (`userId`, `requestId`, payload, UUID), porque criam séries demais e elevam custo/memória. Para medir HTTP em geral, aproveite métricas automáticas da extensão antes de instrumentar manualmente todo endpoint.

**Métricas versus os outros sinais:** métrica = tendência numérica (ex.: 503/min); log = evento detalhado; trace = caminho de uma requisição pelos serviços; health = sinal operacional simplificado **UP/DOWN**. Um health UP não prova que não há erros de negócio.

### Demo Micrometer

1. Com o estoque ligado, execute `GET /products` duas vezes.
2. Pare o estoque e execute uma vez; observe HTTP 503.
3. Consulte `http://localhost:9091/q/metrics` e procure `catalogo_consultas`.
4. Mostre séries com `origem="ESTOQUE"` e `origem="FALLBACK"`. Os contadores são separados pelo valor da tag.

---

## 3. Tolerância a falhas x ExceptionHandler do Spring Boot

### 3.1 Quais problemas cada mecanismo resolve?

| Questão | Fault Tolerance (`@Retry`, `@Fallback`, `@CircuitBreaker`) | Tratamento de exceções (`@ControllerAdvice` / `@ExceptionHandler`) |
|---|---|---|
| Propósito | **Resiliência operacional** durante uma chamada | **Tradução centralizada** de erros para uma resposta HTTP |
| Atua quando | Antes de desistir/na falha da operação protegida | Quando uma exceção se propaga até a camada web |
| Pode repetir a operação? | Sim, com `@Retry` | Não por padrão |
| Pode devolver uma alternativa? | Sim, com `@Fallback` | Pode montar resposta, mas não substitui a política de resiliência |
| Pode abrir circuito? | Sim, com `@CircuitBreaker` | Não |
| Equivalente no outro ecossistema | Em Spring: Resilience4j/Spring Retry (ou similares) | Em Jakarta REST/Quarkus: `ExceptionMapper<T>` + `@Provider` |

Essas abordagens são **complementares**. Mesmo usando Fault Tolerance, ainda se pode criar um `ExceptionMapper` para transformar exceções não recuperadas em JSON/status padronizados.

### 3.2 Como o código atual funciona?

```java
@ApplicationScoped
public class ProductService {
    @Retry(maxRetries = 2, delay = 200)
    @Fallback(fallbackMethod = "fallbackProducts")
    public CatalogoResultado getProducts() { ... }

    public CatalogoResultado fallbackProducts() {
        return new CatalogoResultado(List.of(), "FALLBACK");
    }
}
```

- `@ApplicationScoped`: bean CDI gerenciado pelo container, instância compartilhada no escopo da aplicação. O objeto não deve guardar estado mutável por requisição sem proteção.
- `@Retry(maxRetries = 2, delay = 200)`: **1 execução inicial + no máximo 2 repetições**, total máximo de 3 invocações quando há falhas que acionam retry. `delay` é em milissegundos, entre tentativas; não é timeout.
- `@Fallback(fallbackMethod = "fallbackProducts")`: quando a execução protegida não obtém sucesso, chama o método de fallback compatível com a assinatura do método principal.
- `@RestClient` (no construtor): injeta implementação HTTP gerada a partir de `EstoqueGateway`.
- `fallbackProducts()`: retorna `CatalogoResultado` com `origem="FALLBACK"` e produtos vazios; o boundary calcula `total=0` ao converter o resultado para `CatalogoResponse`. **Não se deve interpretar esse array como estoque realmente vazio**.
- `ProductResource.getProducts()`: ao identificar `FALLBACK`, transforma o resultado em **HTTP 503 Service Unavailable**; se a consulta funciona, devolve 200.

```text
GET /products
  → ProductService.getProducts()
      → tentativa #1 → falhou
      → aguarda 200 ms
      → tentativa #2 → falhou
      → aguarda 200 ms
      → tentativa #3 → falhou
      → fallbackProducts() → CatalogoResultado(..., origem="FALLBACK")
  → ProductResource converte para CatalogoResponse(total=0)
  → ProductResource → HTTP 503
```

Quando o estoque está ativo, a primeira tentativa pode terminar em sucesso, sem retry. Nos testes, a indisponibilidade é simulada com resposta HTTP 503; na demo manual, parar o servidor provoca falha de conexão.

**Cuidados:** não repetir indiscriminadamente POST que cobra, transfere ou cria dados; analisar idempotência, limites de tentativas, timeout, backoff, tratamento seletivo de erros (por exemplo, não repetir 400 por default) e risco de efeito cascata. `@Retry` não substitui limites de conexão nem `@Timeout`; os dois timeouts do REST Client no `application.properties` ajudam a limitar chamadas de rede. `@CircuitBreaker` é tópico de extensão útil para reduzir chamadas a dependência que falha persistentemente.

**Exemplo conceitual de ExceptionMapper em Quarkus (não é necessário adicionar ao projeto):**

```java
@Provider
public class ErroMapper implements ExceptionMapper<IllegalArgumentException> {
    @Override
    public Response toResponse(IllegalArgumentException e) {
        return Response.status(400).entity(Map.of("erro", e.getMessage())).build();
    }
}
```

`@Provider` permite que Jakarta REST descubra o mapper; `toResponse` traduz exceção em resposta. Ele **não** tenta novamente consultar estoque. Imports ilustrativos: `jakarta.ws.rs.ext.Provider`, `jakarta.ws.rs.ext.ExceptionMapper`, `jakarta.ws.rs.core.Response`, `java.util.Map`.

**Pergunta para discussão:** faria sentido responder 200 com uma lista vazia quando o estoque caiu? **Gabarito:** neste domínio, não: o consumidor confundiria indisponibilidade com ausência real de produtos. Alternativas como cache de última resposta são possíveis, mas precisariam explicitar que os dados podem estar desatualizados.

---

## 4. Explicação classe a classe e anotações

### 4.1 `CatalogoLivenessCheck.java`

```java
@Liveness
@ApplicationScoped
public class CatalogoLivenessCheck implements HealthCheck {
    @Override
    public HealthCheckResponse call() {
        return HealthCheckResponse.up("catalogo-vivo");
    }
}
```

- `implements HealthCheck`: contrato com método `call()` que o mecanismo de health executa ao consultar as probes.
- `@Liveness`: associa a verificação à rota **`/q/health/live`**; em Kubernetes, uma falha persistente da *liveness probe* pode provocar reinício do container.
- `@ApplicationScoped`: componente CDI da aplicação.
- `@Override`: implementação do método definido na interface; não é anotação de Quarkus.
- `HealthCheckResponse.up("catalogo-vivo")`: devolve verificação com nome `catalogo-vivo` e status **UP**; existe opção DOWN e builder para detalhes.

**O que esta implementação verifica de fato?** Apenas que uma chamada chegou à classe e retornou UP; é um exemplo intencionalmente simples. Não detecta qualquer bloqueio lógico, nem garante processamento útil. Se o processo morrer, a rota deixa de responder; o orquestrador detecta por conexão/timeout.

**Caso de uso:** indicar que a instância pode continuar viva. **Não** atrelar a liveness à disponibilidade do estoque remoto: uma queda do estoque poderia provocar reinícios inúteis de todas as instâncias do catálogo.

### 4.2 `CatalogoReadinessCheck.java`

```java
@Readiness
@ApplicationScoped
public class CatalogoReadinessCheck implements HealthCheck {
    @ConfigProperty(name = "catalogo.nome")
    String nome;

    @Override
    public HealthCheckResponse call() {
        return HealthCheckResponse.named("catalogo-configurado")
                .status(nome != null && !nome.isBlank())
                .build();
    }
}
```

- `@Readiness`: registra o check em **`/q/health/ready`**. A *readiness probe* sinaliza se a instância deve **receber novas requisições** pelo balanceador de um orquestrador.
- `@ConfigProperty(name = "catalogo.nome")`: injeta configuração que pode vir de arquivo, profile, variável de ambiente ou `-D`.
- `HealthCheckResponse.named(...).status(condição).build()`: compõe a resposta. Se o valor injetado for não vazio → UP; se o check receber um valor inválido/branco → DOWN, e o endpoint agregado pode devolver HTTP 503. **Atenção:** caso a propriedade obrigatória não exista ou seja tratada como vazia pela resolução do MicroProfile Config, o Quarkus também pode **falhar na inicialização por injeção**, antes que esta probe seja consultada. Para exercitar DOWN explicitamente, escolha um valor inválido mas presente ou injete `Optional<String>` e trate a ausência.
- `call()`: executado nas consultas à probe. Sem acesso remoto nesta implementação.

**Liveness vs Readiness vs Startup:** *liveness* pergunta “devo reiniciar este container?”, *readiness* pergunta “devo encaminhar tráfego para ele?”, *startup* ajuda aplicações que demoram a iniciar (o Quarkus também pode ter `/q/health/started` com checks específicos, se configurados). `Readiness` não deve virar um teste de integração pesado executado a cada probe.

**Pergunta para discussão:** se o estoque falhar, devemos marcar a API como DOWN? **Gabarito:** depende de a dependência ser indispensável para **todas** as operações e da estratégia de degradação. Neste projeto, `/info` e as probes continuam respondendo, enquanto `/products` devolve 503. Marcar todas as instâncias como not ready ao mesmo tempo pode derrubar a disponibilidade do serviço por completo.

### 4.3 `CatalogoInfoResource.java`

```java
@Path("/info")
@Produces(MediaType.APPLICATION_JSON)
public class CatalogoInfoResource {
    @ConfigProperty(name = "catalogo.nome")
    String nome;

    @GET
    public Informacoes info() {
        return new Informacoes(nome, "microservico");
    }

    public record Informacoes(String nome, String tipo) {}
}
```

- `@Path("/info")`: rota base de Jakarta REST, acessível por **GET `/info`** com o método `@GET`.
- `@Produces(MediaType.APPLICATION_JSON)`: seleciona a representação JSON; Quarkus REST Jackson serializa o record.
- `@ConfigProperty`: injeta `catalogo.nome` resolvido pelo sistema de configuração.
- `@GET`: liga o método à operação HTTP GET.
- `info()`: devolve o record imutável `Informacoes` com `nome` lido da configuração e `tipo` fixo.
- `record`: declaração Java 21 concisa de DTO, com componentes `nome`, `tipo`, construtor, acessores e métodos de valor gerados.

**Casos de uso:** expor identificação não sensível para troubleshooting, ensino de configuração e inspeção do ambiente. Em produção, não retornar segredos, endpoints internos, tokens, credenciais nem *todas* as propriedades em `/info`.

**Contraste com Spring:** `@Path`/`@GET` fazem o papel próximo de `@RestController`/`@GetMapping`; `@ConfigProperty` tem uso semelhante a `@Value("${...}")` (com diferenças de infraestrutura e ciclo de vida).

---

## 5. `application.properties` explicado propriedade por propriedade

Arquivo atual: `src/main/resources/application.properties`.

```properties
# Porta que o servidor HTTP do Quarkus usa em execução normal.
quarkus.http.port=9091

# Porta aleatória para testes HTTP Quarkus (0 = porta livre atribuída pelo SO).
quarkus.http.test-port=0

# Propriedade da nossa aplicação; o Quarkus resolve CATALOGO_NOME como override.
catalogo.nome=Catálogo API

# URL base do REST Client com configKey "estoque-local".
# Usa a variável ESTOQUE_LOCAL_URL, se existir; senão localhost:8081.
quarkus.rest-client.estoque-local.url=${ESTOQUE_LOCAL_URL:http://127.0.0.1:8081}

# Máximo para estabelecer conexão TCP (ms).
quarkus.rest-client.estoque-local.connect-timeout=1000

# Máximo de espera por leitura de resposta do cliente (ms).
quarkus.rest-client.estoque-local.read-timeout=1000
```

| Chave | Quem consome? | Alteração visível |
|---|---|---|
| `quarkus.http.port` | Servidor HTTP Quarkus | API sobe em 9091, não 8080 |
| `quarkus.http.test-port` | Infraestrutura de teste do Quarkus | Evita colisão com portas fixas na suíte |
| `catalogo.nome` | `CatalogoInfoResource` e `CatalogoReadinessCheck` | Muda JSON `/info` e critério da readiness |
| `quarkus.rest-client.estoque-local.url` | `EstoqueGateway` (`@RegisterRestClient(configKey="estoque-local")`) | URL base para GET `/products` remoto |
| `...connect-timeout` | HTTP client do estoque | Limita tempo para conseguir conectar |
| `...read-timeout` | HTTP client do estoque | Limita espera por dados/resposta após conectar |

**Entender `${ESTOQUE_LOCAL_URL:http://127.0.0.1:8081}`:** `${...}` faz expansão de expressões; `ESTOQUE_LOCAL_URL` é procurada nas fontes de configuração; após `:` está o valor padrão. Isso é diferente do mapeamento automático `catalogo.nome` → variável `CATALOGO_NOME`, embora ambos permitam configuração externa.

**Ordem de precedência (simplificada, da maior para a menor):** `-Dchave=valor` (system property), variável de ambiente, arquivo `.env`, arquivo externo `config/application.properties`, arquivo de aplicação empacotado. Há detalhes adicionais para profiles e fontes customizadas. Nenhum segredo deve ser commitado em arquivos de profile; usar secrets/variáveis gerenciadas pelo ambiente.

### 5.1 Como definir ambientes no Quarkus

O Quarkus tem três **profiles automáticos**:

- `dev`: com `mvn quarkus:dev`.
- `test`: durante os testes automatizados.
- `prod`: execução normal do artefato empacotado, salvo ativação de outro profile.

Um profile **nomeado** também pode ser criado, por exemplo `staging`, `homolog`, `qa` ou `prod-eu`.

**Abordagem A — blocos no mesmo `application.properties`:**

```properties
catalogo.nome=Catálogo API
%dev.catalogo.nome=Catálogo API (DEV)
%test.catalogo.nome=Catálogo API (TEST)
%prod.catalogo.nome=Catálogo API (PROD)
%staging.catalogo.nome=Catálogo API (STAGING)
```

> Esse bloco é **um exemplo alternativo**; não foi incluído no arquivo real para não alterar o resultado do teste e a demo existente.

**Abordagem B — arquivos separados por profile**, junto ao `application.properties`:

```text
src/main/resources/
  ├── application.properties            # config compartilhada (existente)
  ├── application-staging.properties    # EXEMPLO REAL incluido neste ZIP
  ├── application-dev.properties        # opcional
  ├── application-test.properties       # opcional
  └── application-prod.properties       # opcional
```

Conteúdo **real** de `application-staging.properties`:

```properties
catalogo.nome=Catálogo API - STAGING
```

A chave `catalogo.nome` passa a ser específica de staging; todas as outras propriedades continuam herdadas de `application.properties`. No arquivo `application-staging.properties`, as chaves **não precisam** de prefixo `%staging.`. **Não defina `quarkus.profile` dentro de arquivos profile-aware**; o profile precisa estar selecionado antes de esses arquivos serem carregados.

**Ativar profile no terminal Windows PowerShell:**

```powershell
# Terminal da raiz catalogo-api (pom.xml).
mvn quarkus:dev "-Dquarkus.profile=staging"
# Em outro terminal:
curl.exe http://localhost:9091/info
# Esperado: nome "Catálogo API - STAGING".
```

**Alternativa por variável de ambiente:**

```powershell
$env:QUARKUS_PROFILE = "staging"
mvn quarkus:dev
# Para limpar o override após a demonstração:
Remove-Item Env:QUARKUS_PROFILE
```

**Em Linux/macOS:** `QUARKUS_PROFILE=staging mvn quarkus:dev`.

**Override de propriedade isolada:**

```powershell
$env:CATALOGO_NOME = "Minha turma"
mvn quarkus:dev
# GET /info deve mostrar "Minha turma" (variável tem precedência).
Remove-Item Env:CATALOGO_NOME
```

**Regra de arquitetura:** configuração de runtime (nome, URL remota, timeout, credenciais externas) pode variar entre instâncias do mesmo artefato; **parte das propriedades de Quarkus é fixada em build-time** (por exemplo, decisões de empacotamento/extensões). Ao compilar nativo, valide sempre quais propriedades podem mudar após o build. Escolher profile diferente entre build e runtime pode causar efeitos inesperados se nele houver propriedades de build-time.

**Comparação com Spring Boot:** `%dev.alguma.propriedade` e `application-dev.properties` são análogos, conceitualmente, ao `application-dev.yml`/`application-dev.properties` com `spring.profiles.active=dev`. As regras específicas de resolução e de build-time no Quarkus são diferentes.

---

## 6. Demonstrações guiadas em sequência

### Demo A — executar os dois serviços e reconfigurar

**Terminal 1 (raiz do projeto):**

```powershell
java --class-path estoque-local/src estoque-local/src/EstoqueLocalServer.java
```

**Terminal 2:**

```powershell
mvn quarkus:dev
```

**Terminal 3:**

```powershell
curl.exe -i http://localhost:9091/info
curl.exe -i http://localhost:9091/products
```

Com estoque ativo, o catálogo filtra `stock > 0` e responde com dois produtos. Alterar `CATALOGO_NOME` em novo processo demonstra externalização: **mesmo código**, configuração diferente. Não altere a variável de ambiente **depois** que o processo Quarkus tiver iniciado esperando que ela seja automaticamente relida.

### Demo B — liveness/readiness

```powershell
curl.exe -i http://localhost:9091/q/health/live
curl.exe -i http://localhost:9091/q/health/ready
curl.exe -i http://localhost:9091/q/health
```

Pare o estoque (`Ctrl+C` no terminal 1): as probes demonstrativas permanecem UP. Isso foi intencional; a falha do estoque afeta a operação `/products`, não significa que o processo catálogo parou.

**Kubernetes (ilustração de parte de um container spec, não um manifesto completo):**

```yaml
livenessProbe:
  httpGet:
    path: /q/health/live
    port: 9091
readinessProbe:
  httpGet:
    path: /q/health/ready
    port: 9091
```

Kubernetes pode reiniciar o container com falha persistente de liveness; remover temporariamente um pod dos endpoints de Service com falha de readiness. As configurações reais também exigem parâmetros de frequência, delay, timeout e thresholds apropriados ao cenário.

### Demo C — retry, fallback e tratamento HTTP

1. Ligue o estoque e consulte `GET /products` → **200**, `origem=ESTOQUE`.
2. Pare o estoque e repita → a chamada falha, `@Retry` pode executar até 3 invocações e `@Fallback` é acionado → **503**, `origem=FALLBACK`.
3. Religue o estoque e repita → **200** sem reiniciar a API Quarkus.
4. Discuta por que um 503 honesto é melhor do que transformar a falha em “nenhum produtoBo cadastrado”.

### Demo D — inspeção de métricas

```powershell
curl.exe -s http://localhost:9091/q/metrics | Select-String "catalogo_consultas"
```

É necessário chamar `/products` antes. Perguntar: a métrica conta o número de clientes HTTP internos ou de requisições do endpoint? **Gabarito: requisições atendidas por `ProductResource`**, uma contagem após o retorno de `ProductService`.

---

## 7. GraalVM Native Image: teoria e trade-offs

**JVM tradicional:** compila `.java` → bytecode `.class`; a JVM executa o programa, combinando interpretação, JIT (*Just-in-Time*) e otimizações de runtime (como profiling, inlining e recompilação). Normalmente é boa para serviços de longa duração, nos quais o JIT aquece e otimiza caminhos quentes.

**Native Image:** usa compilação **AOT** (*Ahead-of-Time*) e análise de alcance para produzir um **executável específico de sistema/arquitetura**, incluindo runtime reduzido necessário. Quarkus faz trabalho antecipado no processo de build para favorecer startup e uso de memória em diversos cenários.

| Critério | JVM/JIT | Native Image/AOT |
|---|---|---|
| Inicialização | Em geral maior, depende da aplicação | Frequentemente menor |
| Memória em runtime | JVM/JIT/heap + overhead | Frequentemente menor, mas depende de workload |
| Throughput em longa execução | Pode ganhar com otimizações JIT | Pode ser inferior, equivalente ou superior conforme o caso |
| Compilação | `mvn package` normalmente mais rápida | Build pode exigir mais tempo, CPU, memória e espaço |
| Reflexão/proxies/recursos dinâmicos | Dinâmicos na JVM | Devem ser compatíveis com a análise e, em certos casos, registrados |
| Portabilidade do artefato | Bytecode roda em JVMs compatíveis | Binário específico de SO, arquitetura e ABI |
| Testabilidade | Testes padrão + integração | Executar testes/smoke tests do artefato nativo é importante |

**Pergunta frequente: compilar nativo é sempre mais rápido?** Não. Melhor startup ≠ throughput maior. Comparações honestas devem medir startup, latência, RSS/memória, CPU e throughput **na mesma carga, hardware e limites de memória**. Diferenciar otimização para funções/serverless, autoescalabilidade, serviço de tráfego constante e tempo de desenvolvimento.

**GraalVM x Mandrel:** Mandrel é uma distribuição de ferramentas Native Image otimizada para o ecossistema Quarkus e workloads Linux; com `-Dquarkus.native.container-build=true` o Quarkus usa, por padrão, o builder configurado para construir o executável Linux dentro de um contêiner. **O Docker Desktop evita instalar manualmente GraalVM no Windows para esta atividade.** O compilador nativo executa no contêiner de **build**; a imagem final de runtime não precisa levar JDK nem compilador.

---

## 8. Laboratório com Docker Desktop — Windows + PowerShell (passo a passo)

> Objetivo: construir um executável nativo **Linux** com Docker Desktop, empacotá-lo em uma imagem, executá-lo em uma rede privada com o servidor de estoque e validar saúde, configuração, métricas e falhas. Os comandos abaixo assumem PowerShell no Windows e terminal aberto **na pasta `catalogo-api/`**.

### 8.1 Conferir pré-requisitos

1. Abra **Docker Desktop**; aguarde o engine ficar operacional. Use **Linux containers** (não Windows containers). Em Windows, Docker Desktop geralmente usa WSL 2.
2. Verifique o Docker e o Maven/JDK, com acesso ao Docker pelo terminal onde executará o Maven:

```powershell
docker version
docker info --format '{{.OSType}}'   # esperado: linux
java -version                       # Java 21
mvn -version                        # Java usado pelo Maven: 21
```

3. Recomenda-se reservar vários GB de memória livre para compilar Native Image e espaço para baixar a imagem de builder; o build pode consumir bastante recurso.
4. Garanta que a raiz do projeto contém `pom.xml`, `src/` e `estoque-local/`. As portas **9091** no Windows devem estar livres, especialmente se `mvn quarkus:dev` estiver rodando; finalize a execução dev antes de iniciar o contêiner com `-p 9091:9091`.
5. Se o Docker não conseguir montar a pasta do projeto no Windows, verifique as permissões/integração WSL e compartilhamento de arquivos do Docker Desktop.

### 8.2 Executar testes rápidos na JVM antes do build nativo

```powershell
mvn clean test
```

A suíte usa o `EstoqueTestResource` para criar um `HttpServer` simulado em porta dinâmica e testar sucesso/503/config/health/métricas. **Não precisa** iniciar o estoque manualmente para rodar essa suíte.

### 8.3 Gerar o executável nativo Linux usando Docker como builder

```powershell
mvn clean package "-Dnative" "-Dquarkus.native.container-build=true" "-Dquarkus.native.container-runtime=docker" "-DskipTests" "-DskipITs"
```

Significado dos argumentos:

- `clean`: remove artefatos anteriores de `target/`.
- `package`: cria o artefato do projeto.
- `-Dnative`: aciona o profile Maven nativo deste POM.
- `-Dquarkus.native.container-build=true`: faz o processo de compilação nativa **dentro de um contêiner**, sem instalar GraalVM diretamente no Windows.
- `-Dquarkus.native.container-runtime=docker`: seleciona Docker explicitamente.
- `-DskipTests` e `-DskipITs`: evitam executar testes nessa compilação inicial; **não substituem** o `mvn test` anterior nem a validação HTTP posterior. No Windows, é especialmente conveniente evitar tentar executar localmente binário Linux nos testes de integração.

**Artefato esperado:** `target/catalogo-api-1.0.0-runner` (sem `.exe`, pois é executável **Linux**). Confira:

```powershell
Get-ChildItem .\target\*runner*
```

Não use `./target/...-runner` no PowerShell: o binário Linux **não executa diretamente no Windows**. Diferencie **o binário Native Image** do **Docker image** que vamos gerar no passo seguinte.

### 8.4 Gerar uma imagem Docker que contém o catálogo nativo

O arquivo `src/main/docker/Dockerfile.native` usa base Linux **UBI 9**, compatível com o builder nativo da linha Quarkus 3.40, copia `target/*-runner`, define `EXPOSE 9091` e inicia `./application`.

```powershell
docker build -f src/main/docker/Dockerfile.native -t catalogo-api:aula04-native .
docker image ls catalogo-api
```

**Por que não há Java no Dockerfile final?** O executável gerado já contém o necessário para iniciar a aplicação; a imagem final possui bibliotecas de sistema/base mínima, mas não precisa de uma JVM completa.

### 8.5 Preparar o estoque como contêiner na mesma rede

Esta versão do ZIP acrescenta `estoque-local/Dockerfile` e suporte opcional `--host` ao servidor Java original:

- Execução local, sem argumento: continua vinculada a `127.0.0.1:8081`.
- Execução isolada em Docker: inicia com `--host 0.0.0.0` **dentro do contêiner**, acessível por outro contêiner na rede Docker. **Não** publicamos a porta 8081 para o host com `-p`.

Crie a rede Docker e a imagem do estoque:

```powershell
docker network create catalogo-aula04
docker build -f estoque-local/Dockerfile -t catalogo-estoque:aula04 ./estoque-local
```

Execute o estoque:

```powershell
docker run -d --name estoque-local --network catalogo-aula04 catalogo-estoque:aula04
docker logs estoque-local
```

O nome `estoque-local` é resolvido pelo DNS da rede Docker. **Não é** o mesmo `127.0.0.1` usado quando ambos os serviços estão no Windows; `localhost` dentro de um contêiner significa **o próprio contêiner**.

### 8.6 Executar o catálogo nativo e passar configurações de runtime

```powershell
docker run -d --name catalogo-native --network catalogo-aula04 -p 9091:9091 -e CATALOGO_NOME="Catálogo Aula 04 - Docker" -e ESTOQUE_LOCAL_URL="http://estoque-local:8081" catalogo-api:aula04-native
```

- `-d`: execução em segundo plano.
- `--name`: nome amigável para logs e operações.
- `--network`: liga o catálogo à rede privada com o estoque.
- `-p 9091:9091`: expõe **somente** a API no Windows (`host:container`).
- `-e CATALOGO_NOME=...`: sobrescreve `catalogo.nome` da aplicação nativa sem recompilar.
- `-e ESTOQUE_LOCAL_URL=...`: aponta o REST Client para o hostname DNS `estoque-local` na rede privada.

Confira status/logs:

```powershell
docker ps
docker logs catalogo-native
```

### 8.7 Smoke tests: config, health e produtos

```powershell
# HTTP 200 e nome configurado em -e CATALOGO_NOME
curl.exe -i http://localhost:9091/info

# Liveness / readiness UP
curl.exe -i http://localhost:9091/q/health/live
curl.exe -i http://localhost:9091/q/health/ready

# HTTP 200, dois produtos com estoque > 0, origem ESTOQUE
curl.exe -i http://localhost:9091/products
```

**Resultado esperado no `/info` (exemplo):** `{"nome":"Catálogo Aula 04 - Docker","tipo":"microservico"}`. A rota `/products` deve mostrar dois produtos; `Pasta`, com `stock=0`, não deve aparecer.

### 8.8 Testar tolerância a falhas na imagem nativa

**Desligue somente o contêiner do estoque:**

```powershell
docker stop estoque-local
curl.exe -i http://localhost:9091/products
```

**Esperado:** HTTP **503**, com corpo similar a `{"products":[],"total":0,"origem":"FALLBACK"}`. O `@Retry` tenta novamente e o fallback produz um resultado explícito. `/q/health/live` e `/q/health/ready` continuam UP porque, no exemplo, não dependem do estoque.

**Religue o estoque e valide recuperação:**

```powershell
docker start estoque-local
curl.exe -i http://localhost:9091/products
```

**Esperado:** HTTP **200**, `origem="ESTOQUE"`.

### 8.9 Testar métricas e consumo de recursos

```powershell
curl.exe -s http://localhost:9091/q/metrics | Select-String "catalogo_consultas"
docker stats catalogo-native --no-stream
docker image ls catalogo-api
```

Procure as séries por `origem` normal e fallback. `docker stats` fornece uma **amostra** de CPU e memória do contêiner; não prova sozinho que o binário nativo consome sempre menos memória. Para comparar com JVM, rode builds equivalentes em contêineres com configurações/carga iguais, faça aquecimento quando adequado e repita a medição. `docker image ls` mede **tamanho da imagem OCI**, não RSS ou memória da aplicação.

### 8.10 Limpar os recursos da demonstração

```powershell
docker rm -f catalogo-native estoque-local
docker network rm catalogo-aula04
# Opcional: remover imagens criadas nesta aula:
docker image rm catalogo-api:aula04-native catalogo-estoque:aula04
```

### 8.11 Problemas frequentes

| Sintoma | Verificação / solução |
|---|---|
| `docker` indisponível | Iniciar Docker Desktop e garantir terminal com acesso ao engine Linux |
| `mvn` usa Java errado | Verificar `mvn -version` e `JAVA_HOME` para JDK 21 |
| Build nativo sem memória | Ajustar recursos disponíveis no Docker/WSL; fechar outros processos; repetir |
| `port is already allocated` em 9091 | Parar o `quarkus:dev` ou outro container/processo que utiliza 9091 |
| `GET /products` devolve 503 com estoque “ativo” | Inspecionar `docker ps`, `docker logs estoque-local`, `docker logs catalogo-native`, nome da rede e `ESTOQUE_LOCAL_URL` |
| Usar `localhost:8081` dentro do catálogo nativo | Trocar para `http://estoque-local:8081` na mesma Docker network |
| `docker run` não reconhece nome do estoque | Verificar se **ambos** usam `--network catalogo-aula04` e se o container `estoque-local` está iniciado |
| Tentativa de executar `*-runner` no PowerShell | É binário Linux; executar dentro de imagem Docker Linux |
| `Dockerfile.native` não encontra `target/*-runner` | Verificar `mvn ... -Dnative` finalizado com sucesso antes do `docker build` |
| Build nativo com daemon Docker remoto | Consultar `quarkus.native.remote-container-build=true` na documentação oficial |

**Observação de segurança:** só use `--host 0.0.0.0` para o servidor de estoque quando necessário e com rede/firewall apropriados. Neste roteiro, ele fica acessível aos contêineres da rede privada e **a porta não é publicada no Windows**.

---

## 9. Laboratório individual (30–40 min) + gabarito do professor

**Enunciado para os alunos:** implemente pequenas evoluções a partir do projeto existente; não copie a implementação completa do professor. Registre os comandos usados e evidências (HTTP/status ou saída Prometheus).

### Atividade 1 — propriedade e ambiente

**Tarefa:** adicionar `catalogo.ambiente=local`, expor `ambiente` no JSON de `/info` e testar override com `CATALOGO_AMBIENTE`.

**Pistas:** seguir padrão do `@ConfigProperty(name="catalogo.nome")` e atualizar o `record Informacoes`.

**Gabarito possível:**

```java
@ConfigProperty(name = "catalogo.ambiente", defaultValue = "local")
String ambiente;

// Dentro de info():
return new Informacoes(nome, "microservico", ambiente);

// Atualizar o record:
public record Informacoes(String nome, String tipo, String ambiente) {}
```

No PowerShell: `$env:CATALOGO_AMBIENTE = "dev"` antes de `mvn quarkus:dev` e depois `curl.exe http://localhost:9091/info`. Aceitar soluções usando `application-dev.properties`, desde que expliquem precedência. O `defaultValue` evita erro de ausência da propriedade para este campo.

### Atividade 2 — readiness adicional

**Tarefa:** criar `ConfiguracaoObrigatoriaReadinessCheck` que valide uma propriedade necessária (por exemplo, `catalogo.ambiente`) e observar a agregação em `/q/health/ready`.

**Gabarito conceitual:** novo bean CDI com `@Readiness` e `implements HealthCheck`, `call()` retornando `HealthCheckResponse.named("ambiente-configurado").status(...).build()`. Injeção com `@ConfigProperty`/`Optional` ou default conforme expectativa. Demonstrar status DOWN sem impedir a inicialização da aplicação se o exercício exigir config ausente. Não consultar estoque na liveness.

### Atividade 3 — contador próprio de fallback

**Tarefa:** contar somente respostas em fallback. **Gabarito mínimo:** dentro do `if ("FALLBACK".equals(...))` em `ProductResource`, executar `meterRegistry.counter("catalogo.fallbacks").increment();`. Exportação típica: `catalogo_fallbacks_total`. Justificar por que não incluir `userId` como tag.

### Atividade 4 — retry e observação de falhas

**Tarefa:** fazer uma chamada bem-sucedida, parar o estoque e repetir. Informar status, body e número **máximo** de execuções do método protegido.

**Gabarito:** sucesso 200/`ESTOQUE`; falha 503/`FALLBACK`; `maxRetries=2` implica 1 tentativa original + até 2 repetições = **3 tentativas**. Duas pausas configuradas de 200 ms entre tentativas, sem contar tempo de rede. Se houver falha que não se enquadre na política de retry, comportamento depende da configuração/interceptação.

**Critérios de aceitação:** `/info` com novo atributo, readiness com novo check, métrica de fallback identificável em `/q/metrics`, explicação correta das tentativas e HTTP status.

---

## 10. Pesquisa individual — perguntas COM respostas esperadas e fontes

> **Sugestão didática:** entregar aos alunos somente as perguntas; o gabarito abaixo destina-se ao professor. Peça exemplos e citação de documentação oficial, não apenas uma definição curta.

### P1. Qual é a diferença entre MicroProfile Metrics e Micrometer?

**Resposta esperada:** ambos permitem registrar métricas, mas MicroProfile Metrics é uma especificação MicroProfile e Micrometer é uma API/biblioteca de instrumentação amplamente adotada. Quarkus atual recomenda Micrometer e a extensão Prometheus; não há exigência de adotar MicroProfile Metrics para continuar usando Config/Health/Fault Tolerance. [Fonte: Quarkus Micrometer](https://quarkus.io/guides/telemetry-micrometer/).

### P2. O que faz o `MeterRegistry`? Qual a diferença entre Counter, Timer e Gauge?

**Resposta esperada:** cria/reutiliza instrumentos identificados por nome e tags e os conecta à exportação configurada. Counter registra ocorrências crescentes; Timer mede duração e quantidade de eventos; Gauge observa valor instantâneo. Para tamanho de payload, `DistributionSummary`. Tags controladas são importantes para agregar séries; tags altamente variáveis aumentam cardinalidade. [Fonte: Micrometer — Meters](https://docs.micrometer.io/micrometer/reference/concepts/meters.html).

### P3. Retry/Fallback substitui ExceptionHandler? Como seria no Spring Boot?

**Resposta esperada:** não. Retry/fallback são padrões de resiliência. `@ExceptionHandler`/`@ControllerAdvice` ou `ExceptionMapper` padronizam a representação do erro. Spring Boot pode usar Resilience4j/Spring Retry para resiliência; Jakarta REST oferece `ExceptionMapper`. [Fonte: Quarkus Fault Tolerance](https://quarkus.io/guides/smallrye-fault-tolerance/).

### P4. Qual a diferença entre liveness e readiness? E se o estoque cair?

**Resposta esperada:** liveness ajuda a decidir reinício; readiness indica elegibilidade para receber tráfego. Neste projeto, ambas continuam UP porque avaliam o processo/uma configuração local, não a dependência remota. `/products` pode retornar 503 sem que a instância deva ser reiniciada. [Fonte: Quarkus Health](https://quarkus.io/guides/smallrye-health/).

### P5. Como Quarkus seleciona `dev`, `test`, `prod` e `staging`?

**Resposta esperada:** `dev` para `quarkus:dev`, `test` em testes, `prod` em execução empacotada; profile nomeado pode ser ativado com `quarkus.profile`/`QUARKUS_PROFILE`. É possível usar `%profile.chave=...` ou `application-profile.properties`; uma variável de ambiente pode sobrescrever valores. Algumas configurações são build-time e não devem ser assumidas alteráveis em runtime. [Fonte: Quarkus Config Reference](https://quarkus.io/guides/config-reference/).

### P6. Por que Native Image costuma iniciar mais rápido? Quais são os trade-offs?

**Resposta esperada:** compilação AOT e trabalho antecipado reduzem necessidade de inicialização/otimização em runtime em muitos casos. Trade-offs: compilação demorada e intensa, restrições para alguns usos dinâmicos/reflexão, dependência de sistema/arquitetura, throughput dependente do workload e demanda por testes do binário final. **Não se pode prometer ganho universal de performance.** [Fonte: Quarkus Native Image](https://quarkus.io/guides/building-native-image/).

### P7. O que diferencia o builder GraalVM/Mandrel, o executável Linux e a imagem Docker?

**Resposta esperada:** builder é o ambiente/ferramenta de compilação; executável é o resultado Native Image ligado ao SO/arquitetura; imagem Docker é o pacote de filesystem/configuração que carrega o executável para executar em contêiner. Uma imagem OCI não é sinônimo de compilação nativa: também pode executar Java/JAR sobre JVM. [Fonte: Quarkus Native Image](https://quarkus.io/guides/building-native-image/).

### P8. Se uma aplicação inicia mais rápido, ela necessariamente atende mais requisições por segundo?

**Resposta esperada:** não. Startup é métrica de inicialização; throughput e latências sob carga são métricas diferentes. JIT pode otimizar longas execuções; AOT tem diferentes trade-offs. É preciso benchmark reproduzível sob cargas e configurações comparáveis.

### P9. Por que é importante que os serviços tenham deploys e configurações independentes?

**Resposta esperada:** possibilita alterar/implantar/escalar partes separadamente, reduzir acoplamento operacional, adaptar limites de recursos e isolar responsabilidades. Custos: rede, observabilidade, consistência de dados, tolerância a falhas, contratos entre serviços e automação de deploy. **Dois processos não garantem maturidade de microserviços.**

### P10. O que devemos monitorar para garantir confiabilidade real?

**Resposta esperada:** combinar disponibilidade, taxa de erros (incluindo fallback), latência (p50/p95/p99), tráfego, saturação, memória/CPU, logs, traces e probes. Uma métrica de negócio (consultas/fallbacks) complementa as métricas técnicas.

### Avaliação sugerida das pesquisas

Pedir para cada aluno: (a) citar ao menos uma fonte oficial; (b) responder com palavras próprias; (c) associar à classe/endereço correspondente no `catalogo-api`; (d) apontar pelo menos um trade-off ou risco operacional. A resposta não deve depender de números fixos não medidos pela turma.

---

## 11. Perguntas rápidas para revisão oral (com gabarito)

1. **Quantas chamadas máximas do método?** Até 3 (1 + 2 retries).
2. **`delay=200` é timeout?** Não. É intervalo configurado entre tentativas.
3. **O que significa 503 no `/products`?** Indisponibilidade da consulta remota tratada via fallback, não produtoBo esgotado.
4. **Por que não usar retry para qualquer POST?** Risco de repetir efeitos colaterais sem idempotência.
5. **`HealthCheckResponse.up` reinicia algo?** Não. Apenas responde UP; o orquestrador interpreta a probe.
6. **O que a readiness atual valida?** `catalogo.nome` não está branco; não testa estoque.
7. **Por que `CATALOGO_NOME` funciona?** Mapeamento de nome de propriedade em fonte de ambiente; maior precedência que o arquivo.
8. **É possível usar `application-staging.properties`?** Sim; selecionar o profile staging antes do startup.
9. **`MeterRegistry.counter` cria um contador a cada chamada?** Registra ou reutiliza o medidor identificado por nome + tags no registry.
10. **Uma chamada a `/products` com 2 retries incrementa `catalogo.consultas` quantas vezes?** Uma vez no `ProductResource`, se ele chegar a executar essa linha.
11. **O binário Linux criado com Docker executa no Windows diretamente?** Não; executá-lo em Linux/contêiner Linux.
12. **Por que `127.0.0.1` não liga os dois contêineres?** Cada contêiner tem seu loopback; usar DNS da rede Docker.

---

## 12. Fontes oficiais para aprofundamento

- Quarkus — [SmallRye Config / Configuration Reference](https://quarkus.io/guides/config-reference/)
- Quarkus — [SmallRye Health](https://quarkus.io/guides/smallrye-health/)
- Quarkus — [SmallRye Fault Tolerance](https://quarkus.io/guides/smallrye-fault-tolerance/)
- Quarkus — [Micrometer Metrics](https://quarkus.io/guides/telemetry-micrometer/)
- Micrometer — [Meter concepts](https://docs.micrometer.io/micrometer/reference/concepts/meters.html)
- Quarkus — [Building a Native Executable](https://quarkus.io/guides/building-native-image/)
- Quarkus — [Configuring Applications](https://quarkus.io/guides/config/)
- Docker — [Networking containers](https://docs.docker.com/engine/network/)

**Nota de validação:** o servidor `estoque-local` e a sintaxe Java foram exercitados com Java 21 nesta revisão. O build completo Maven e o build Docker/GraalVM devem ser executados no computador do professor/aluno com Docker Desktop ativo; os resultados medidos em sala podem variar por sistema, recursos e versões locais.
