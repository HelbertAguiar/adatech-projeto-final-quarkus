# Quarkus vs. Spring Boot

Este guia compara os dois frameworks de forma prática. Não existe um vencedor
universal: a escolha depende do tipo de sistema, da equipa e das restrições de
operação. Métricas como tempo de arranque, memória e throughput devem sempre
ser medidas com a aplicação e a infraestrutura reais.

## Visão geral

- **Quarkus** é orientado a aplicações cloud-native, Kubernetes e execução
  nativa. Move parte relevante do processamento para o *build time*, reduzindo
  trabalho no arranque. Usa padrões como Jakarta EE, CDI e MicroProfile.
- **Spring Boot** é a forma mais difundida de criar aplicações Spring. Oferece
  auto-configuração, um ecossistema muito amplo e integração madura com
  bibliotecas e produtos Spring.

## Onde o Quarkus se destaca

| Aspeto | Vantagem do Quarkus | Por que importa |
| --- | --- | --- |
| Arranque e memória | Foi desenhado para iniciar rapidamente e usar pouca memória, especialmente como executável nativo com GraalVM/Mandrel. | Ajuda em funções, serviços que escalam para zero, muitos pods e ambientes com recursos limitados. |
| Cloud e Kubernetes | Tem suporte de primeira classe para imagens nativas, configuração, health checks, métricas e integração com plataformas cloud. | Simplifica a entrega de microsserviços em containers. |
| Processamento no build | Resolução de DI e análise de classes ocorrem em grande parte na compilação. | Erros podem aparecer mais cedo e há menos reflexão/trabalho em runtime. |
| Dev mode | `mvn quarkus:dev` fornece live reload e Dev UI. | Ciclo de desenvolvimento rápido para APIs e serviços. |
| Padrões abertos | É baseado em Jakarta EE, CDI e MicroProfile. | Útil quando a equipa quer evitar acoplamento forte a APIs proprietárias de um único ecossistema. |

### Pontos de atenção ao escolher Quarkus

- A imagem nativa pode exigir adaptações para reflexão, recursos e bibliotecas
  dinâmicas; o build costuma ser mais lento e mais exigente que o JVM tradicional.
- A compatibilidade com anotações Spring é útil para migração, mas não executa
  um `ApplicationContext` nem toda a infraestrutura Spring. Não trate-a como
  compatibilidade total de qualquer biblioteca Spring.
- O ecossistema é grande, mas o de Spring ainda é mais amplo em exemplos,
  integrações de terceiros e experiência disponível no mercado.

## Onde o Spring Boot se destaca

| Aspeto | Vantagem do Spring Boot | Por que importa |
| --- | --- | --- |
| Ecossistema | Spring Data, Security, Batch, Cloud, Integration e muitas integrações de terceiros são amplamente usados e documentados. | Reduz risco quando a aplicação precisa de capacidades empresariais ou conectores específicos. |
| Mercado e aprendizagem | Há mais profissionais, cursos, exemplos e respostas para problemas comuns. | Facilita contratação, manutenção e onboarding. |
| Convenções e produtividade | Auto-configuração, starters e APIs consistentes resolvem muitos casos de uso com pouca configuração. | Excelente para equipas já habituadas ao ecossistema Spring. |
| Sistemas existentes | A migração incremental dentro de uma organização Spring é normalmente mais simples. | Permite reutilizar bibliotecas internas, práticas e conhecimento já consolidados. |
| Flexibilidade em runtime | O modelo baseado no `ApplicationContext` e no amplo conjunto de extensões Spring atende cenários altamente dinâmicos. | Pode ser preferível quando a aplicação depende profundamente de mecanismos Spring em runtime. |

### Pontos de atenção ao escolher Spring Boot

- Uma aplicação JVM convencional pode consumir mais memória e demorar mais a
  arrancar do que uma alternativa nativa otimizada; isso varia com dependências
  e configuração.
- Spring Boot também suporta imagens nativas (AOT/GraalVM), mas deve ser
  avaliado e testado no projeto — não é uma troca automática sem impacto.
- Em workloads com muitos serviços pequenos e efémeros, custos de recursos e
  tempo de escala merecem medição cuidadosa.

## Quando escolher cada um

### Escolha Quarkus quando

- o principal objetivo é minimizar memória e tempo de arranque em containers;
- o serviço será distribuído em Kubernetes, serverless ou terá escalamento
  frequente;
- executáveis nativos são uma vantagem operacional relevante;
- a equipa aceita trabalhar com CDI/Jakarta/MicroProfile e validar limites de
  bibliotecas no modo nativo;
- está a criar novos microsserviços e quer um modelo cloud-native desde o início.

### Escolha Spring Boot quando

- a equipa, as bibliotecas internas ou a plataforma da empresa já são Spring;
- é necessário usar intensamente módulos Spring ou integrações cuja alternativa
  em Quarkus não atende o requisito;
- velocidade de entrega depende de talento disponível, documentação e exemplos
  conhecidos pela equipa;
- a aplicação é um monólito ou serviço JVM de longa duração em que o arranque e
  a memória não são a principal restrição;
- a migração teria alto risco e não há ganho operacional mensurável que a justifique.

## Aplicação deste repositório

Esta API já usa o estilo nativo de Quarkus: `ProductResource` expõe `GET
/products` com JAX-RS (`@Path`, `@GET`) e injeta `ProductService` com CDI
(`@Inject`). Para este tipo de API REST, Quarkus é uma escolha natural quando a
prioridade for execução em containers e eficiência operacional.

Se fosse implementada em Spring Boot, o equivalente geralmente usaria
`@RestController`, `@GetMapping` e injeção por construtor. A decisão não deve
ser tomada apenas pela diferença de anotações: avalie dependências necessárias,
perfil de execução, requisitos de suporte e um benchmark representativo.

## Checklist de decisão

1. Quais integrações obrigatórias já existem e em qual ecossistema são maduras?
2. Arranque, memória e densidade de containers têm metas mensuráveis?
3. A equipa domina CDI/Jakarta ou Spring, e qual custo de capacitação é aceitável?
4. O modo nativo foi testado com as dependências, observabilidade e segurança reais?
5. O ganho esperado compensa o custo de adoção ou migração?

## Referências

- [Quarkus: visão geral](https://quarkus.io/about/)
- [Quarkus: performance](https://quarkus.io/performance/)
- [Quarkus: Kubernetes native](https://quarkus.io/kubernetes-native/)
- [Quarkus: extensão para Spring DI](https://quarkus.io/guides/spring-di/)
- [Quarkus: construir executável nativo](https://quarkus.io/guides/building-native-image/)
- [Spring Boot: documentação](https://docs.spring.io/spring-boot/index.html)
- [Spring Boot: imagens nativas](https://docs.spring.io/spring-boot/how-to/native-image/index.html)
