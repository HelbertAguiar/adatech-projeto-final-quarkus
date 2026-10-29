# Refatoração da Aula 05 — Feature + Boundary / Domain / Repository

Esta evolução **não é um exercício de CRUD novo**: altera a estrutura interna dos pacotes e o mapeamento entre camadas, preservando as URLs e as respostas HTTP das Aulas 03, 04 e 05.

## Quais classes procurar?

| Assunto | Classe | Caminho relativo a `src/main/java/studio/catalogo/` |
| --- | --- | --- |
| Entrada REST /produtos | `ProdutoResource` | `produtos/boundary/` |
| JSON de POST/PUT | `ProdutoRequestDto` | `produtos/boundary/` |
| JSON da resposta | `ProdutoResponseDto` | `produtos/boundary/` |
| Casos de uso/CRUD | `ProdutoService` | `produtos/domain/` |
| Objeto simples de domínio | `ProdutoBo` | `produtos/domain/` |
| Tabela e anotações JPA | `ProdutoEntity` | `produtos/repository/` |
| Acesso ao banco (Panache) | `ProdutoRepository` | `produtos/repository/` |
| Criteria API | `ProdutoCriteriaQueryExemplo` | `produtos/repository/` |
| GET /products | `ProductResource` | `catalogoremoto/boundary/` |
| Retry e Fallback | `ProductService` | `catalogoremoto/domain/` |
| Resultado interno do catálogo | `CatalogoResultado` | `catalogoremoto/domain/` |
| Cliente HTTP de estoque | `EstoqueGateway` | `catalogoremoto/integration/estoque/` |
| Liveness/Readiness | `Catalogo*Check` | `platform/health/` |

## Demonstração de arquitetura em 5 minutos

1. Abra `ProdutoResource` e observe `@Path`, `@Valid`, `Response` e a conversão `ProdutoResponse.from(...)` — tudo na fronteira HTTP.
2. Abra `ProdutoService` e observe `@Transactional`, chamadas ao `ProdutoRepository` e o retorno de `ProdutoBo`, sem importar `ProdutoRequestDto` nem `ProdutoResponseDto`.
3. Abra `ProdutoRepository` e compare `findById`, `find` e `persist` com Hibernate ORM tradicional.
4. Faça uma requisição no Postman (`GET /produtos`) para comprovar que a API HTTP não mudou.
5. Mostre `OrganizacaoPacotesTest`: ele testa a organização esperada dos pacotes e os tipos retornados pelo domínio.

## Trade-off didático

O domínio depende diretamente do repositório concreto Panache para evitar uma interface por operação CRUD. Em projetos maiores, podemos definir uma interface no domínio, implementar o acesso aos dados em um adaptador e transformar a arquitetura em Ports & Adapters.

**Não mova o exemplo `docs/aula-05/exemplos/ProdutoActiveRecordExemplo.java` para o código-fonte durante a aula regular**: ele é um exercício separado, com anotações da entidade comentadas. Ativar Active Record exige refatorar as referências atuais e observar a estratégia de IDs.
