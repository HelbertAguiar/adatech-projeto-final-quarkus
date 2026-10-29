# Arquitetura de pacotes — Catálogo API (Aulas 03–05)

Organização adotada: **Package by Feature + Boundary / Domain / Repository**, padrão híbrido familiar a equipas Java/Spring Boot. A organização principal é por capacidade de negócio; dentro dela, as classes ficam separadas por responsabilidade. **Não é uma implementação rigorosa de Clean Architecture/Hexagonal**: para manter a aula simples, o domínio ainda usa o repositório Panache e o client REST concreto.

```text
studio.catalogo/
├── produtos/                        # Aula 05 — CRUD local, H2 e PostgreSQL
│   ├── boundary/                    # HTTP: ProdutoResource, ProdutoRequest, ProdutoResponse
│   ├── domain/                      # Produto (modelo simples) e ProdutoService
│   └── repository/                  # ProdutoEntity, ProdutoRepository, Criteria API
│
├── catalogoremoto/                  # Aulas 03/04 — dados do Estoque Local
│   ├── boundary/                    # GET /products, /products-native, DTOs HTTP
│   ├── domain/                      # ProductService, CatalogoResultado (retorno de domínio)
│   └── integration/estoque/         # EstoqueGateway e modelos recebidos do REST Client
│
└── platform/                        # Infraestrutura compartilhada
    ├── health/                      # CatalogoLivenessCheck, CatalogoReadinessCheck
    └── info/                        # GET /info, MicroProfile Config
```

Os testes HTTP espelham os pacotes dos *boundaries* em `src/test/java/studio/catalogo/`.

## Responsabilidades e direção das dependências

| Pacote | Papel | Pode usar | Evita acessar |
| --- | --- | --- | --- |
| `*/boundary` | Endpoints, status HTTP, validação, JSON de entrada/saída | `*/domain` | `ProdutoRepository`, `EntityManager` |
| `*/domain` | Regras, orquestração, transações, resultado de negócio | `*/repository` ou `*/integration` (opção pragmática da aula) | DTOs e anotações de HTTP do boundary |
| `produtos/repository` | JPA, queries Panache, transações gerenciadas no service | ORM e banco | Objetos REST |
| `catalogoremoto/integration` | Adaptador REST e contrato do serviço externo | MicroProfile REST Client | Endpoints próprios |
| `platform` | Liveness, readiness, informações operacionais | Configuração do Quarkus | Lógica específica de cadastro |

Fluxo de persistência:

```text
HTTP /produtos
     ↓
produtos.boundary.ProdutoResource   [ProdutoRequest/ProdutoResponse]
     ↓ mapeia os campos / resultado
produtos.domain.ProdutoService     [Produto record de domínio]
     ↓
produtos.repository.ProdutoRepository / ProdutoEntity
     ↓
Hibernate ORM → H2 (dev/test) ou PostgreSQL (prod-like Docker)
```

Fluxo remoto:

```text
HTTP /products
     ↓
catalogoremoto.boundary.ProductResource [CatalogoResponse/ProductDto]
     ↓ mapeia resultado
catalogoremoto.domain.ProductService    [CatalogoResultado]
     ↓
catalogoremoto.integration.estoque.EstoqueGateway
     ↓
Estoque Local (servidor HTTP do JDK)
```

### Por que existem `ProdutoBo` e `ProdutoEntity`?

`ProdutoEntity` é a representação persistente, com anotações JPA. `ProdutoBo` é um record simples do domínio, sem conhecer HTTP ou Hibernate. A fronteira REST também mantém `ProdutoRequestDto` e `ProdutoResponseDto`. **Para um projeto real com CRUD trivial, quatro representações podem ser desnecessárias**: aqui são intencionais para comparar responsabilidades arquiteturais. O domínio depende do Panache pelo repositório; para uma Hexagonal estrita, teríamos de introduzir uma porta (interface de repositório) no núcleo e mover a implementação concreta para a infraestrutura.

### O que foi preservado?

- `GET /products`, `GET /products-native` — resposta com `products`, `total`, `origem`; fallback HTTP 503.
- `GET /info`, probes `/q/health/*`, métricas `/q/metrics`.
- CRUD `/produtos`: GET, POST, PUT, DELETE; payloads com `nome`, `preco`, `quantidade`.
- `application.properties`, H2 dev/test, PostgreSQL no Docker Compose e exemplos de Active Record/Criteria API.

**Observação:** `/products` consulta o serviço remoto. `/produtos` opera sobre o banco local. São fontes independentes.

## Mapa da migração de pacotes (versão anterior → versão atual)

| Antes | Agora |
| --- | --- |
| `catalogoremoto.api` | `catalogoremoto.boundary` |
| `catalogoremoto.application` | `catalogoremoto.domain` |
| `integracao.estoque.client`, `integracao.estoque.model` | `catalogoremoto.integration.estoque` |
| `produtos.api` | `produtos.boundary` |
| `produtos.application` | `produtos.domain` |
| `produtos.persistence` | `produtos.repository` |

É necessário atualizar imports ao integrar alterações de branches anteriores. **URLs HTTP e nomes de propriedades não mudaram.**

## Quando evoluir além desta estrutura

No desenvolvimento corporativo, considere portas/adapters, Flyway/Liquibase, testes de arquitetura (ArchUnit), contratos por módulo e separação de domínios apenas quando a complexidade justificar. O código atual evita interfaces artificiais para os conceitos didáticos da Aula 05.


## Extensão da Aula 06 — Tratamento de erros

`produtos/boundary` contém DTOs validados e endpoints REST. `produtos/domain` lança exceções de negócio
sem depender de HTTP. `platform/error` concentra os `ExceptionMapper` e o contrato JSON de erros.
Consulte [Aula 06](aula-06/README.md).
