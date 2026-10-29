# Quarkus vs. Spring Boot — Como interpretar o benchmark oficial

**Material de apoio para aula de Java e Quarkus**  
**Consulta:** 21 de outubro de 2026  
**Execução identificada no gráfico:** 2 de junho de 2026  
**Fonte:** projeto de benchmarks mantido pela comunidade Quarkus.

> **Importante:** o endereço contém `latest` e pode passar a apresentar outros resultados no futuro. Os números deste documento são um retrato da execução identificada acima, extraídos da versão PNG oficial correspondente à imagem SVG fornecida.

## 1. Gráfico analisado

![Comparação oficial entre Quarkus e Spring Boot — tempo até a primeira resposta, throughput e memória](https://quarkus.io/benchmarks/tuned/spring-quarkus-perf-comparison-latest-tuned-composite-for-main-comparison-light.png)

- [Abrir o SVG original](https://quarkus.io/benchmarks/tuned/spring-quarkus-perf-comparison-latest-tuned-composite-for-main-comparison-light.svg)
- [Abrir a versão PNG equivalente](https://quarkus.io/benchmarks/tuned/spring-quarkus-perf-comparison-latest-tuned-composite-for-main-comparison-light.png)

O gráfico apresenta a mesma aplicação de referência implementada nos dois frameworks, comparando **modo JVM (JIT)** e **modo nativo (GraalVM)**. As barras azuis representam **Quarkus** e as verdes, **Spring Boot**.

## 2. Resultados: valores lidos diretamente do gráfico

| Métrica | Quarkus JVM | Spring Boot JVM | Quarkus Native | Spring Boot Native |
|---|---:|---:|---:|---:|
| **Boot + First Response Time** (menor é melhor) | **4.352 ms** | 10.582 ms | **574 ms** | 2.169 ms |
| **Throughput** (maior é melhor) | **14.917 TPS** | 7.524 TPS | **5.437 TPS** | 2.150 TPS |
| **Memória RSS após a primeira requisição** (menor é melhor) | **271 MiB** | 477 MiB | **98 MiB** | 303 MiB |

**Comparando frameworks com o mesmo tipo de execução:**

| Comparação | Em JVM | Em Native |
|---|---:|---:|
| Tempo até a primeira resposta: vantagem do Quarkus | **2,43× mais rápido** | **3,78× mais rápido** |
| Throughput: vantagem do Quarkus | **1,98× maior** | **2,53× maior** |
| Consumo de memória RSS: redução com Quarkus | **43,2% menor** | **67,7% menor** |

As expressões “2,43× mais rápido” e “1,98× maior” são **razões entre os valores observados neste experimento**: não constituem uma previsão de ganho para qualquer aplicação real.

## 3. O que significa cada métrica?

### 3.1. Boot + First Response Time (TTFR)

**TTFR (*Time to First Request*)** é o tempo transcorrido entre lançar o processo e ele conseguir responder com sucesso a uma requisição HTTP. Inclui inicialização do runtime, inicialização do framework e preparação para atender à primeira requisição.

- **JVM:** Quarkus chega à primeira resposta em **4,352 s**; Spring Boot, em **10,582 s**.
- **Native:** Quarkus chega à primeira resposta em **0,574 s**; Spring Boot, em **2,169 s**.

**Por que importa?** É relevante para inicialização de pods, autoscaling, aplicações *scale-to-zero* e ambientes nos quais novas instâncias precisam ficar prontas rapidamente.

**Cuidado:** TTFR **não** é a latência média de uma API em operação normal, nem é simplesmente o número `Started in ...` exibido nos logs.

### 3.2. Throughput (TPS)

Representa quantas **transações/requisições completas da carga de teste** foram processadas por segundo. No experimento, a aplicação atende a uma carga REST/CRUD com persistência; **TPS não deve ser entendido como “transações financeiras” ou como uma capacidade universal de qualquer endpoint**.

- **JVM:** Quarkus **14.917 TPS** vs. Spring Boot **7.524 TPS**: aproximadamente **1,98×** o throughput.
- **Native:** Quarkus **5.437 TPS** vs. Spring Boot **2.150 TPS**: aproximadamente **2,53×** o throughput.

**Por que importa?** Para serviços com alto volume, maior throughput no mesmo envelope de recursos pode significar mais capacidade por instância.

**Cuidado:** throughput alto **não garante** menor latência nos percentis p95/p99, especialmente sob carga diferente. Para avaliar a experiência do utilizador, analise throughput **junto com** latência e taxa de erro.

### 3.3. Memory RSS (*Resident Set Size*)

RSS é a **memória física residente ocupada pelo processo** no instante da medição. No contexto de Java, abrange mais do que o *heap*: inclui, entre outros componentes, memória nativa da JVM, stacks e código compilado. Aqui, o valor foi medido **após a primeira requisição**.

- **JVM:** Quarkus **271 MiB** vs. Spring Boot **477 MiB**: **43,2% menos memória**.
- **Native:** Quarkus **98 MiB** vs. Spring Boot **303 MiB**: **67,7% menos memória**.

**Por que importa?** Em Kubernetes, o consumo de memória por processo influencia o dimensionamento dos pods, a densidade de implantação e o risco de ultrapassar os limites de memória.

**Cuidado:** RSS após a primeira requisição **não representa obrigatoriamente o pico de memória**, nem o consumo sob carga prolongada. Também não deve ser confundido com `-Xmx`, que controla um limite do heap da JVM.

## 4. JVM (JIT) versus Native (GraalVM): o trade-off

Este é um dos pontos didáticos mais importantes do gráfico: **não basta escolher o menor consumo de memória; é preciso considerar o throughput necessário**.

Dentro do **próprio Quarkus**, ao sair de JVM para Native:

| Métrica | Quarkus JVM | Quarkus Native | Interpretação |
|---|---:|---:|---|
| Primeira resposta | 4.352 ms | 574 ms | Native alcança a primeira resposta cerca de **7,58× mais rápido**. |
| Throughput | 14.917 TPS | 5.437 TPS | Native apresenta **63,6% menos throughput** nesta carga. |
| RSS | 271 MiB | 98 MiB | Native utiliza **63,8% menos memória** neste momento da medição. |

O modo **JVM/JIT** pode beneficiar-se de otimizações de execução feitas pelo compilador JIT para caminhos de código frequentemente utilizados. O modo **Native** compila a aplicação antecipadamente para um executável e costuma favorecer inicialização e uso de memória, mas pode ter menor throughput máximo, como ocorreu aqui.

**Leitura arquitetural:**

- **Microserviço de longa duração com tráfego intenso:** comece por avaliar a versão **JVM**, considerando throughput, latência, CPU e custo total.
- **Serviço de baixa utilização, inicialização muito frequente ou memória restrita:** avalie **Native**, contabilizando também tempo de build, compatibilidade de bibliotecas e observabilidade.
- **Ambientes com Kubernetes:** tempo de readiness, memória mínima por pod e escalabilidade importam; porém, **não é possível concluir o número ideal de réplicas só com este gráfico**.

> **Native não é sinónimo de “mais rápido em tudo”.** Ele pode iniciar mais depressa e ocupar menos memória, mas entregar menos requisições por segundo na mesma carga.

## 5. Por que Quarkus pode alcançar esses resultados?

Uma diferença de arquitetura importante é o princípio de **otimização em build time** do Quarkus. Parte do trabalho que outros sistemas realizam ao iniciar é antecipada para o momento do build: análise de anotações, preparação de metadados, geração de código e resolução de componentes. O framework também utiliza uma base de execução HTTP assíncrona, apoiada em Vert.x/Netty, que pode reduzir custos de processamento em determinadas cargas.

O Spring Boot também possui otimizações de inicialização, AOT e suporte a Native. Não é correto afirmar que “Spring é sempre lento” ou atribuir toda a diferença do gráfico a um único mecanismo. Os resultados observados dependem do conjunto de bibliotecas, do workload, do tipo de concorrência e da configuração dos runtimes.

## 6. Como o benchmark foi construído?

**Informações visíveis no rodapé da imagem analisada:**

| Item | Valor |
|---|---|
| Quarkus | 3.35.4 |
| Spring Boot | 4.0.6 |
| OpenJDK/JVM | 25.0.3-tem |
| GraalVM | 25.0.2-graalce |
| CPUs atribuídas à aplicação | 4 |
| Heap configurado para a execução JVM | `-Xms512m -Xmx512m` |
| Flags JVM apresentadas no gráfico | `-XX:+UseParallelGC -XX:+UseNUMA` |
| Cenário | **tuned** (ajustes de configuração permitidos) |
| Commit identificado | `38345ca` |
| Data da execução | 2026-06-02 |

**Sobre o cenário `tuned`:** ele permite ajustes razoáveis de configuração, por exemplo *thread pools* e *connection pools*, procurando comparar aplicações equivalentes sem alterar a arquitetura de uma delas para obter vantagem. Isso é diferente de um teste puramente *out of the box*.

A documentação oficial informa que o projeto de comparação procura equivalência do modelo e das tecnologias de persistência (incluindo Hibernate/JPA) e utiliza uma carga REST/CRUD com PostgreSQL. A medição de throughput usa **Hyperfoil**; a metodologia inclui aquecimento (*warmup*) e uma etapa de medição sob carga. O projeto evolui ao longo do tempo, pelo que **não se deve presumir que todos os parâmetros atuais dos scripts sejam idênticos aos da execução de junho mostrada no gráfico**.

## 7. Limitações e leitura crítica

1. **É um benchmark de uma aplicação específica**, não um resultado garantido para todos os sistemas Spring Boot e Quarkus.
2. **O gráfico é publicado pelo ecossistema Quarkus**, ainda que haja código e metodologia públicos para reprodução. Convém validar com medições independentes no workload da própria equipa.
3. **A carga e o ambiente influenciam muito:** quantidade de CPU, pool de conexões, queries, caching, GC, versão de Java, JIT, rede e limitação de recursos podem mudar o resultado.
4. **Faltam métricas para uma decisão completa:** percentis de latência, utilização de CPU, taxa de erros, desempenho sob carga sustentada, custo de build Native e custo operacional não estão todos representados no gráfico.
5. **O histórico do benchmark importa:** a equipa publicou posteriormente ajustes de metodologia para evitar comparações injustas causadas pelo aquecimento incompleto do compilador JIT. Ao reproduzir resultados, confirme a revisão e a metodologia empregadas.
6. **“Menos memória” não significa necessariamente “menor custo”:** o custo real também depende da quantidade de trabalho processada, das réplicas, da infraestrutura e da disponibilidade exigida.

## 8. Resumo para apresentar aos alunos (30–45 segundos)

> “Este gráfico compara o desempenho de duas aplicações equivalentes, uma com Quarkus e outra com Spring Boot. Ele mede quanto tempo cada uma demora até responder à primeira requisição, quantas transações atende por segundo e quanta memória ocupa. Neste teste, o Quarkus foi mais eficiente nas três métricas quando comparamos os mesmos modos de execução. Entretanto, ao mudar o Quarkus de JVM para Native, a aplicação iniciou muito mais rapidamente e consumiu menos memória, mas perdeu bastante throughput. Portanto, a melhor escolha depende do tipo de aplicação e não apenas de qual barra parece melhor.”

### Perguntas para discussão

1. Em um serviço com tráfego constante e elevado, é melhor privilegiar **throughput** ou **tempo de inicialização**? Por quê?
2. Em Kubernetes, o que precisamos medir além de RSS para decidir quantas réplicas criar?
3. Se uma API passa a depender de chamadas lentas a serviços externos, os resultados deste benchmark provavelmente continuam na mesma proporção?

## 9. Fontes oficiais

- [Gráfico SVG original — Quarkus vs. Spring Boot](https://quarkus.io/benchmarks/tuned/spring-quarkus-perf-comparison-latest-tuned-composite-for-main-comparison-light.svg)
- [Gráfico PNG equivalente](https://quarkus.io/benchmarks/tuned/spring-quarkus-perf-comparison-latest-tuned-composite-for-main-comparison-light.png)
- [Repositório do benchmark: código e critérios de equivalência](https://github.com/quarkusio/spring-quarkus-perf-comparison)
- [Repositório dos resultados e dos gráficos](https://github.com/quarkusio/benchmarks)
- [Guia oficial: como o Quarkus mede performance](https://quarkus.io/guides/performance-measure)
- [Artigo: novos benchmarks e interpretação dos resultados](https://quarkus.io/blog/new-benchmarks/)
- [Artigo: melhorias de justiça metodológica no benchmark](https://quarkus.io/blog/fairness-in-benchmarking/)

---

**Conclusão:** para a carga específica testada, Quarkus mostrou vantagens em tempo até a primeira resposta, throughput e memória frente a Spring Boot quando comparados em modos equivalentes. Para decidir uma arquitetura real, complemente esses dados com um benchmark do **seu próprio serviço** e com métricas de latência, CPU, taxa de erro, memória e custo operacional.
