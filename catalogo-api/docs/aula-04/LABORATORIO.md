# Aula 04 — Laboratório individual: Configuração, Health Checks e Resiliência

**Duração:** 40 minutos (versão essencial: 30 minutos; extensões: até 50 minutos)  
**Formato:** prática individual, com validação no Postman ou `curl.exe`  
**Projeto de partida:** `catalogo-api` da Aula 04, com Java 21 e Quarkus 3.40.1

## 1. Contexto do problema

A equipe mantém dois serviços independentes:

- **Catálogo API (Quarkus):** disponibiliza produtos ao cliente, na porta `9091`.
- **Estoque Local (Java HttpServer):** fornece os produtos e suas quantidades, na porta `8081`.

O catálogo já consulta o estoque por REST Client. Ele também possui configurações externas, Health Checks, `@Retry`, `@Fallback` e métricas com Micrometer. Agora precisamos **evoluir a aplicação para torná-la mais configurável e observável**, sem criar outro projeto.

**Sua missão:** implementar mudanças pequenas no projeto existente e comprovar o resultado por requisições HTTP. Não é necessário implementar CRUD, banco de dados, Kubernetes ou uma Native Image para concluir a atividade.

## 2. Objetivos de aprendizagem

Ao final, você deverá conseguir:

1. Criar uma propriedade de configuração e injetá-la com `@ConfigProperty`.
2. Diferenciar `liveness` de `readiness` por meio de um teste real.
3. Registrar uma métrica de negócio com `MeterRegistry`.
4. Analisar como `@Retry` e `@Fallback` afetam uma chamada entre serviços.
5. Explicar por que a falha de uma dependência externa **não obriga** o processo do catálogo a ser reiniciado.

## 3. O que já existe e o que você vai implementar

| Já está implementado | Você irá fazer |
| --- | --- |
| `GET /info` retorna `nome` e `tipo` | Adicionar o campo `ambiente` |
| `CatalogoReadinessCheck` valida o nome | Adicionar uma configuração que habilita/desabilita a prontidão |
| `CatalogoLivenessCheck` informa o estado de vida | Observar seu comportamento sem modificá-lo |
| `ProductResource` registra `catalogo.consultas` | Criar uma métrica exclusiva para fallbacks |
| `ProductService` usa `@Retry` e `@Fallback` | Simular falhas e explicar as tentativas |

### Arquivos relevantes

```text
catalogo-api/
├── pom.xml
├── estoque-local/src/EstoqueLocalServer.java
└── src/main/
    ├── java/studio/catalogo/
    │   ├── platform/info/CatalogoInfoResource.java
    │   ├── platform/health/CatalogoLivenessCheck.java
    │   ├── platform/health/CatalogoReadinessCheck.java
    │   ├── catalogoremoto/boundary/ProductResource.java
    │   └── catalogoremoto/domain/ProductService.java
    └── resources/
        ├── application.properties
        └── application-staging.properties
```

**Regra:** utilize as extensões que já estão no `pom.xml`. Não adicione dependências sem necessidade e não altere o contrato de `GET /products`.

---

## 4. Preparação — 5 minutos

Abra a pasta **`catalogo-api`**, que contém o `pom.xml`. Utilize dois terminais.

**Terminal 1 — Estoque Local:**

```powershell
java --class-path estoque-local/src estoque-local/src/EstoqueLocalServer.java
```

**Terminal 2 — API Quarkus:**

```powershell
mvn quarkus:dev
```

No Postman (ou usando `curl.exe`), confirme os endpoints:

| Requisição | Resultado inicial esperado |
| --- | --- |
| `GET http://localhost:8081/products` | HTTP 200; produtos do estoque |
| `GET http://localhost:9091/info` | HTTP 200; `nome` e `tipo` |
| `GET http://localhost:9091/products` | HTTP 200; `origem: ESTOQUE`, `total: 2` |
| `GET http://localhost:9091/q/health/live` | HTTP 200; `status: UP` |
| `GET http://localhost:9091/q/health/ready` | HTTP 200; `status: UP` |

> Se as portas estiverem ocupadas, identifique o processo em execução antes de continuar. Os exemplos desta atividade consideram `8081` e `9091`.

## 5. Atividade A — Configuração por ambiente (7 minutos)

**Requisito de negócio:** o endpoint `/info` deve informar o ambiente no qual o catálogo está executando.

### Implemente

1. Em `src/main/resources/application.properties`, defina a propriedade `catalogo.ambiente` com o valor `local`.
2. Em `CatalogoInfoResource`, injete essa propriedade usando `@ConfigProperty`, seguindo o padrão do campo `nome`.
3. Atualize o `record Informacoes` e a resposta do método `info()` para incluir o novo campo `ambiente`, preservando `nome` e `tipo`.
4. Faça `GET /info` novamente.

**Contrato esperado:**

```json
{
  "nome": "Catálogo API",
  "tipo": "microservico",
  "ambiente": "local"
}
```

**Teste de configuração externa:** pare apenas a API Quarkus (`Ctrl+C`) e execute:

```powershell
mvn quarkus:dev "-Dcatalogo.ambiente=turma04"
```

O valor de `ambiente` deve mudar para `turma04` **sem editar o código Java**. Reinicie a API normalmente antes da próxima atividade.

**Pista:** procure a anotação já utilizada para `catalogo.nome`. Não confunda o goal `quarkus:dev` (modo de execução) com o profile `dev` (configuração).

### Critério de conclusão

- [ ] `/info` contém os três campos.
- [ ] O argumento `-Dcatalogo.ambiente=turma04` altera somente o valor configurado.

## 6. Atividade B — Readiness controlada por configuração (10 minutos)

**Requisito operacional:** a equipe precisa colocar uma instância do catálogo em estado **não pronta para tráfego**, sem desligar o processo.

### Implemente

1. Adicione `catalogo.pronto=true` em `application.properties`.
2. Abra `CatalogoReadinessCheck` e injete `catalogo.pronto` como `boolean`, usando `@ConfigProperty`.
3. Ajuste `call()` para que o resultado seja `UP` **somente quando**:
   - `catalogo.nome` estiver preenchido; **e**
   - `catalogo.pronto` for `true`.
4. Não modifique `CatalogoLivenessCheck`.

**Teste 1 — aplicação pronta:** com a configuração `true`, consulte `/q/health/ready` e `/q/health/live`. Ambos devem retornar `UP`.

**Teste 2 — aplicação não pronta:** encerre o Quarkus e execute:

```powershell
mvn quarkus:dev "-Dcatalogo.pronto=false"
```

Depois consulte:

```powershell
curl.exe -i http://localhost:9091/q/health/ready
curl.exe -i http://localhost:9091/q/health/live
curl.exe -i http://localhost:9091/info
```

| Endpoint | Resultado esperado com `catalogo.pronto=false` |
| --- | --- |
| `/q/health/ready` | HTTP 503 e `status: DOWN` |
| `/q/health/live` | HTTP 200 e `status: UP` |
| `/info` | Continua respondendo HTTP 200 |

**Pergunta de raciocínio:** por que `/info` ainda funciona quando a readiness está `DOWN`? O que mudaria se um Kubernetes Service estivesse configurado para usar essa probe?

**Importante:** o Quarkus não bloqueia automaticamente suas próprias rotas quando a readiness está `DOWN`. Essa informação só afeta o roteamento/reinício quando um orquestrador está configurado para interpretá-la.

Reinicie a API com `mvn quarkus:dev` antes de continuar.

### Critério de conclusão

- [ ] A readiness passa de `UP` para `DOWN` pela configuração.
- [ ] A liveness permanece `UP`.
- [ ] Nenhuma chamada ao Estoque Local foi adicionada às probes.

## 7. Atividade C — Contador exclusivo de fallbacks (8 minutos)

**Requisito de observabilidade:** a equipe quer saber quantas respostas do catálogo foram produzidas pelo fallback.

### Implemente

1. Abra `ProductResource`.
2. Localize a condição que identifica `origem` como `FALLBACK`.
3. Nesse caminho, registre **uma ocorrência por requisição HTTP** em um contador Micrometer chamado `catalogo.fallbacks`.
4. Mantenha a métrica já existente `catalogo.consultas`, que separa as chamadas por `origem`.

**Pista:** o projeto já injeta `MeterRegistry`. Observe como `counter(...).increment()` é utilizado. Uma chamada que passou por duas repetições do `@Retry` ainda deve gerar **uma ocorrência de fallback**, não três.

### Teste

1. Com catálogo e estoque ligados, faça `GET /products`: resultado `200`, `origem: ESTOQUE`.
2. Pare **somente o Estoque Local** com `Ctrl+C` no Terminal 1.
3. Faça `GET /products` duas vezes: ambas devem retornar `503` e `origem: FALLBACK`.
4. Consulte `GET http://localhost:9091/q/metrics`.
5. Procure a métrica `catalogo_fallbacks_total`, que deve ter aumentado em **2** nesse experimento (se o contador começou do zero). Caso o serviço já tenha registrado fallbacks, compare os valores antes e depois.

**Atenção:** os contadores existem na saída Prometheus depois de serem registrados/utilizados; não espere encontrar um contador de fallback antes da primeira falha.

### Critério de conclusão

- [ ] Duas requisições com fallback aumentam o contador em 2.
- [ ] A métrica anterior `catalogo_consultas_total` continua disponível.
- [ ] O endpoint `/products` preserva HTTP 503 quando o estoque está fora.

## 8. Atividade D — Interpretar Retry, Fallback e Health (10 minutos)

**Esta atividade não exige nova classe.** Use o comportamento que já existe em `ProductService` e observe o resultado da aplicação.

Com o Estoque Local ainda desligado, consulte:

```powershell
curl.exe -i http://localhost:9091/products
curl.exe -i http://localhost:9091/q/health/live
curl.exe -i http://localhost:9091/q/health/ready
```

Preencha a tabela com sua observação:

| Pergunta | Sua resposta |
| --- | --- |
| Qual status HTTP foi retornado em `/products`? | |
| Qual foi o valor de `origem`? | |
| Qual é o valor de `total`? | |
| Qual é o máximo de tentativas do método protegido com `@Retry(maxRetries = 2)`? | |
| Qual foi o status de liveness? E de readiness? | |
| O `@Fallback` resolveu a falha ou apenas permitiu uma resposta controlada? | |

Agora reinicie o Estoque Local e faça novamente `GET /products`. Confirme o retorno ao HTTP 200.

### Critério de conclusão

- [ ] Você demonstrou sucesso, indisponibilidade e recuperação.
- [ ] Conseguiu explicar a diferença entre **falha na dependência**, **falha na própria aplicação** e **não prontidão para receber tráfego**.

---

## 9. Extensões opcionais — até 10 minutos adicionais

**Escolha somente uma** se concluir as atividades anteriores antes do tempo.

### Opção 1 — Medir duração com `Timer` (código)

Adicione em `ProductResource` uma métrica Micrometer `catalogo.tempo.consulta` para medir a duração de `productService.getProducts()`, incluindo as tentativas de retry e o fallback quando houver. Uma possibilidade é estudar `Timer.Sample` (`start` e `stop`) ou `MeterRegistry.timer(...).record(...)`.

**Validação:** após chamadas a `/products`, procure em `/q/metrics` as séries correspondentes a `catalogo_tempo_consulta_seconds_count` e `catalogo_tempo_consulta_seconds_sum`. Compare a métrica com `catalogo.consultas`: qual mede quantidade e qual mede duração?

### Opção 2 — Profiles no Quarkus (configuração)

No arquivo existente `application-staging.properties`, acrescente `catalogo.ambiente=staging` e execute:

```powershell
mvn quarkus:dev "-Dquarkus.profile=staging"
```

Confirme que `/info` exibe o ambiente `staging`. Depois teste se uma variável de ambiente `CATALOGO_AMBIENTE` consegue sobrescrever esse valor (reinicie o processo ao defini-la).

### Opção 3 — GraalVM Native Image (pesquisa curta)

Sem fazer uma compilação nativa durante este laboratório, investigue e responda:

- Native Image e imagem Docker são a mesma coisa?
- Por que o build de uma Native Image costuma ser mais demorado que `mvn package` em modo JVM?
- Qual é a vantagem de levar a aplicação pronta para iniciar em um contêiner?

Use como referência a [documentação de Native Image do Quarkus](https://quarkus.io/guides/building-native-image).

## 10. Pesquisa individual — consolidação (se houver tempo)

Responda **com suas palavras**, associando cada conceito a um arquivo do projeto:

1. Qual é a diferença entre **MicroProfile Health** e **Micrometer**? Qual extensão Maven permite usar cada um?
2. Qual é a diferença entre `Counter`, `Timer` e `Gauge`? Que métrica você usaria para medir a quantidade atual de pedidos numa fila?
3. Por que `@Retry` e `@Fallback` não substituem um `ExceptionMapper` no Quarkus ou um `@ExceptionHandler` no Spring Boot?
4. O que pode acontecer se uma readiness depender de um serviço externo que ficou temporariamente indisponível para todas as réplicas?

Fontes de consulta:

- [Quarkus — Configuração](https://quarkus.io/guides/config-reference)
- [Quarkus — Health](https://quarkus.io/guides/smallrye-health)
- [Quarkus — Fault Tolerance](https://quarkus.io/guides/smallrye-fault-tolerance)
- [Quarkus — Micrometer](https://quarkus.io/guides/telemetry-micrometer)

## 11. Entrega e critérios de avaliação

**Entregue** os arquivos Java e `application.properties` modificados (ou um commit com suas alterações) e um registro breve dos testes realizados. Pode ser um `README.md` curto com os comandos e os resultados, ou capturas do Postman.

| Critério | Evidência mínima |
| --- | --- |
| Configuração externa | `/info` mostra `ambiente`, inclusive com override `-D` |
| Health | Readiness `DOWN` por configuração e liveness `UP` |
| Métricas | `catalogo_fallbacks_total` aumenta durante a falha |
| Resiliência | `/products` retorna `503/FALLBACK` e depois `200/ESTOQUE` |
| Clareza | Explicou por que `@Retry` não é o mesmo que tratamento de exceções HTTP |

Se sobrar tempo, execute `mvn test` com a configuração padrão e registre o resultado. **Não entregue somente a saída dos testes:** é preciso demonstrar as mudanças realizadas.

### Checklist final

- [ ] Mudei pelo menos uma configuração sem alterar o Java.
- [ ] Demonstrei readiness `UP` e `DOWN`.
- [ ] Criei uma métrica de negócio com Micrometer.
- [ ] Testei sucesso, fallback e recuperação do estoque.
- [ ] Consigo explicar as decisões tomadas.

**Observação:** não inclua URLs privadas, tokens ou senhas nos arquivos de configuração ou na entrega.
