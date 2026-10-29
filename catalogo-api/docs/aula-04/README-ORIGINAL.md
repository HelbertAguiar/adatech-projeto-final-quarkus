> **Arquivo histórico:** caminhos e comandos podem corresponder à versão anterior dos pacotes ou do Docker Compose. Para executar a versão reorganizada, veja [README raiz](../../README.md), [arquitetura](../ARQUITETURA.md) e [produção local](../producao/README.md).

# Catálogo API — Aula 04 | MicroProfile, Microserviços e GraalVM

Continuação do `catalogo-api.zip` da Aula 03, **sem criar outro projeto Quarkus**. Mantidos o pacote `studio`, Quarkus **3.40.1**, Java **21**, REST Client, Lombok, a porta **9091** e o servidor `estoque-local` em **8081**.

Esta versão foca **apenas a Aula 04**: configuração externa, health checks, tolerância a falhas, métricas e build nativo. O `laboratorio.md` anterior continua no ZIP para consulta, mas os endpoints de CRUD descritos nele não foram adicionados nesta evolução.

## Arquitetura da demonstração

```text
  Browser / curl
        |
        v
  catalogo-api (Quarkus, porta 9091)
    |-- GET /products ------- REST Client ----> estoque-local (JDK HttpServer, porta 8081)
    |-- GET /info                         
    |-- /q/health/live e /q/health/ready
    |-- /q/metrics
    |-- @Retry + @Fallback
    +-- GraalVM Native Image (opcional no build)
```

O `estoque-local` usa somente o JDK e **não precisa de Maven**. Cada aplicação executa em processo independente, com endereço configurável. Ter dois processos não implica, por si só, um ambiente distribuído com escalabilidade automática; a arquitetura permite discutir isso.

## 1. Pré-requisitos

- JDK 21 (`java -version`)
- Maven 3.9+ (`mvn -version`)
- Para compilar executável nativo: GraalVM/Mandrel **ou** Docker/Podman

## 2. Executar a aplicação (dois terminais)

Abra a **raiz `catalogo-api/`** no IntelliJ IDEA.

**Terminal 1 — serviço externo de estoque (porta 8081):**

```bash
java --class-path estoque-local/src estoque-local/src/EstoqueLocalServer.java
```

**Terminal 2 — API Quarkus (porta 9091):**

```bash
mvn quarkus:dev
```

Ao usar o terminal integrado ao IntelliJ, confirme que o diretório atual é o que contém `pom.xml`.

## 3. Roteiro rápido de chamadas

```bash
# 1. MicroProfile Config: propriedade catalogo.nome
curl http://localhost:9091/info

# 2. Integração REST e tratamento de estoque sem disponibilidade
curl -i http://localhost:9091/products

# 3. MicroProfile Health
curl http://localhost:9091/q/health/live
curl http://localhost:9091/q/health/ready

# 4. Micrometer + Prometheus
curl http://localhost:9091/q/metrics
```

**Resposta de `GET /products` com o estoque ativo:**

```json
{
  "products": [
    { "id": 1, "name": "Caderno", "stock": 5 },
    { "id": 2, "name": "Caneta", "stock": 1 }
  ],
  "total": 2,
  "origem": "ESTOQUE"
}
```

O produtoBo `Pasta` (`stock: 0`) é filtrado e o `total` é calculado depois do filtro. A API não mantém listas mutáveis de produtos entre consultas, eliminando o bug de duplicação presente no ZIP original.

## 4. Demonstrar MicroProfile Config

A propriedade `catalogo.nome` está em `src/main/resources/application.properties` e é injetada com `@ConfigProperty`. Uma variável de ambiente tem precedência sobre o arquivo.

**Windows PowerShell** (executar antes de iniciar o Quarkus):

```powershell
$env:CATALOGO_NOME = "Catálogo da turma"
mvn quarkus:dev
```

**Linux/macOS**:

```bash
CATALOGO_NOME="Catálogo da turma" mvn quarkus:dev
```

Verifique o resultado com `curl http://localhost:9091/info`. Para apontar o REST Client para outro endereço, use `ESTOQUE_LOCAL_URL` (ex.: `http://127.0.0.1:8082`).

## 5. Health Checks: liveness vs. readiness

- `GET /q/health/live`: `@Liveness` (`CatalogoLivenessCheck`) verifica que o processo está vivo.
- `GET /q/health/ready`: `@Readiness` (`CatalogoReadinessCheck`) verifica se o nome do serviço está configurado.
- `GET /q/health`: visão geral dos checks.

**Nota de arquitetura:** os checks demonstrativos não consultam o estoque. Uma falha comum é vincular todas as probes à mesma dependência remota e retirar todas as réplicas do serviço de operação durante uma falha transitória. Discuta quando readiness deve considerar dependências críticas, sem colocar chamadas remotas na liveness.

## 6. Fault Tolerance: retry e fallback

1. Faça um `GET /products` com o estoque ativo: **200** e `origem: ESTOQUE`.
2. **Pare apenas o serviço `estoque-local`** no Terminal 1 (`Ctrl+C`).
3. Faça outro `GET /products`: o `ProductService` tenta novamente duas vezes com `@Retry`; depois executa o `@Fallback`.
4. A API devolve **503 Service Unavailable**, `origem: FALLBACK`, `products: []` e `total: 0`.
5. Reinicie o estoque; a resposta volta a **200**.

Neste exemplo, o fallback não cria dados fictícios nem finge que o estoque está vazio: o status HTTP 503 comunica a indisponibilidade ao cliente. Em produção, avalie timeouts, idempotência, backoff e circuit breaker conforme o caso; evite retries em operações com efeitos colaterais sem proteção contra repetição.

## 7. Métricas: abordagem atual no Quarkus

`ProductResource` usa `MeterRegistry` (Micrometer) para registrar a contagem de consultas com a tag `origem` (`ESTOQUE`/`FALLBACK`). Faça chamadas ao endpoint e procure no `/q/metrics` por `catalogo_consultas_total`. O Quarkus também publica métricas HTTP.

**Importante:** Micrometer **não é** a API MicroProfile Metrics. O legado SmallRye Metrics foi descontinuado nas versões mais recentes do Quarkus; usamos Micrometer porque é a recomendação atual. Já `@ConfigProperty`, `@Liveness`, `@Readiness`, `@Retry` e `@Fallback` são APIs MicroProfile.

## 8. Compilar com GraalVM Native Image

**Alternativa A — GraalVM/Mandrel local (instalado e configurado como JDK):**

```bash
mvn clean package -Dnative -DskipITs
```

**Alternativa B — Docker/Podman (constrói executável Linux, inclusive a partir do Windows):**

```bash
mvn clean package -Dnative "-Dquarkus.native.container-build=true" -DskipITs
```

No PowerShell, as aspas em `"-Dquarkus.native.container-build=true"` são necessárias neste ambiente para que o Maven receba a propriedade inteira. Sem elas, ele pode interpretar `.native.container-build=true` como uma fase inválida. Em Bash/Linux/macOS, as aspas são opcionais.

O artefato nativo gerado normalmente fica em `target/catalogo-api-1.0.0-runner` (Linux) ou com a extensão adequada à plataforma na compilação local. Para Linux/macOS, execute:

```bash
./target/catalogo-api-1.0.0-runner
```

**No Windows:** um executável Linux gerado no Docker não roda diretamente no PowerShell. Execute o binário em um contêiner Linux ou em uma máquina Linux. Para gerar executável Windows localmente, use GraalVM para Windows e a toolchain C/C++ requerida.

**Imagem de contêiner com o executável nativo** (apenas se o binário foi gerado para Linux):

```bash
docker build -f src/main/docker/Dockerfile.native -t catalogo-api:aula04-native .
docker run --rm -p 9091:9091 catalogo-api:aula04-native
```

No contêiner, `GET /info` e as probes funcionam independentemente do servidor de estoque. Para `GET /products`, a URL do estoque deve ser uma URL realmente acessível **de dentro do contêiner**; `127.0.0.1` dentro dele não é o host. A imagem OCI e a *Native Image* do GraalVM são artefatos distintos.

## 9. Testes e materiais da aula

```bash
mvn test
```

O teste sobe um servidor HTTP temporário para simular o estoque (porta dinâmica); não é necessário iniciar o `estoque-local` manualmente para executar os testes. A API de testes usa uma porta aleatória, evitando conflitos com a porta 8081 do estoque. São exercitados `GET /products`, falha com fallback, config, health e métricas.

- `AULA-04-ROTEIRO.md`: sequência de demonstrações e desafios para os alunos.
- `laboratorio.md`: material anterior preservado, relativo à evolução de CRUD de Aula 03.
- `estoque-local/`: serviço externo já existente.

Documentação oficial: [MicroProfile Config](https://quarkus.io/guides/config-reference), [Health](https://quarkus.io/guides/smallrye-health), [Fault Tolerance](https://quarkus.io/guides/smallrye-fault-tolerance), [Micrometer](https://quarkus.io/guides/telemetry-micrometer) e [Native Image](https://quarkus.io/guides/building-native-image).

## 10. Guia aprofundado e laboratório completo com Docker Desktop

O arquivo **`AULA-04-ROTEIRO.md`** agora é um **guia do professor com gabarito**, incluindo:

- Diferenças entre MicroProfile Metrics e Micrometer, `MeterRegistry` e exemplos de `Counter`, `Timer`, `Gauge` e `DistributionSummary`.
- Explicação de todas as extensões da Aula 04 no POM, do profile Maven `native` e da diferença para profiles Quarkus.
- Comparação entre Fault Tolerance (`@Retry`/`@Fallback`) e tratamento de exceções (`@ExceptionHandler` no Spring e `ExceptionMapper` no Jakarta REST).
- Anotações, métodos, exemplos e limitações de `CatalogoLivenessCheck`, `CatalogoReadinessCheck` e `CatalogoInfoResource`.
- Propriedades `application.properties`, overrides via ambiente, profiles `%dev`/`%test`/`%prod`, e exemplo funcional em `application-staging.properties`.
- Tutorial **Windows PowerShell + Docker Desktop**, com build Native Image, contêiner nativo do catálogo, contêiner do estoque, rede privada e smoke tests.
- Respostas esperadas às pesquisas individuais e critérios para o laboratório.

**Para a demo Docker do roteiro**, foi adicionado `estoque-local/Dockerfile`. O servidor do estoque aceita agora `--host` opcional; sem argumentos ele **continua escutando somente em `127.0.0.1:8081`**, como na Aula 03. Dentro da rede Docker, o Dockerfile configura `--host 0.0.0.0` para permitir conexão pela rede interna. Não é necessário publicar a porta 8081 no host.

**Observação:** siga o passo a passo da seção 8 de `AULA-04-ROTEIRO.md` para testar a integração entre dois contêineres. O comando simples `docker run -p 9091:9091 catalogo-api:aula04-native` desta introdução testa `/info` e health, mas não configura uma conexão ao servidor de estoque.
