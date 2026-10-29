# Laboratório individual — Aula 05: consultando dados com Panache

**Duração:** 40 minutos (adaptável para 30–50).  
**Pré-requisitos:** Java 21, Maven e projeto `catalogo-api` da Aula 05 aberto no IntelliJ/VS Code.  
**Entrega:** alterações no código + evidências dos testes HTTP.  
**Atenção:** execute os passos; **não copie uma solução pronta**. Este arquivo apresenta as tarefas e os resultados esperados.

## 1. Contexto

O Catálogo evoluiu: além de consultar o estoque remoto (`GET /products`), agora consegue cadastrar produtos no banco H2 usando `GET/POST/PUT/DELETE /produtos`.

A área comercial precisa identificar produtos com poucas unidades disponíveis para planejar reposição. Sua tarefa será **acrescentar uma consulta ao CRUD existente**, utilizando Hibernate ORM/Panache, sem alterar o comportamento dos endpoints anteriores.

## 2. Resultado esperado

Ao final, a API deverá fornecer:

```http
GET http://localhost:9091/produtos/baixo-estoque?limite=5
```

Deve devolver uma lista JSON de produtos com `quantidade` **menor ou igual a 5**, em ordem crescente de quantidade. Não altera o banco.

**Exemplo:** usando os dados iniciais do `import.sql`, o retorno inclui Pasta (0) e Caderno (5), mas não inclui Caneta (12).

## 3. Preparação — 5 minutos

1. Abra a pasta `catalogo-api` (onde está o `pom.xml`).
2. Execute `mvn quarkus:dev`.
3. No Postman ou browser, consulte `http://localhost:9091/produtos` e confirme que aparecem três produtos.
4. Observe a diferença entre `/produtos` (Hibernate/H2) e `/products` (Estoque Local REST Client). **Não precisa subir Estoque Local** para este laboratório.

## 4. Tarefa A — Entender o mapeamento (5 minutos)

Abra `src/main/java/studio/catalogo/produtos/repository/ProdutoEntity.java` e localize:

- Qual atributo Java será a chave primária SQL?
- Que anotação define a tabela?
- Como o banco sabe que `nome` não pode ser nulo?
- Qual a finalidade de `@GeneratedValue(strategy = IDENTITY)`?

Anote as respostas ou explique ao colega. **Não modifique essa entidade nessa tarefa.**

## 5. Tarefa B — Criar a consulta no repository (10 minutos)

Arquivo: `src/main/java/studio/catalogo/produtos/repository/ProdutoRepository.java`.

1. Crie um método público chamado `buscarComEstoqueAte(int limite)`.
2. O retorno deve ser `List<ProdutoEntity>`.
3. Utilize **Panache `find(...).list()`**, filtrando registros cuja quantidade seja `<= limite`.
4. Ordene pela quantidade de forma crescente; em caso de empate, use o ID.
5. Não execute uma consulta para cada produtoBo e não traga todos os registros para filtrar na memória.

**Dica:** procure no código atual o método `buscarPorNome` para entender como usar `find(...)`. Uma consulta Panache/HQL pode receber parâmetros com `?1`.

## 6. Tarefa C — Expor o endpoint (10 minutos)

Arquivos:

- `src/main/java/studio/catalogo/produtos/domain/ProdutoService.java`
- `src/main/java/studio/catalogo/produtos/boundary/ProdutoResource.java`

1. No `ProdutoService`, crie `listarBaixoEstoque(int limite)` para buscar os dados no repository e devolvê-los como `List<Produto>` (modelo do domínio). Use o método de conversão já existente no serviço.
2. No `ProdutoResource`, crie `GET /produtos/baixo-estoque`. Nesse boundary, transforme cada `ProdutoBo` em `ProdutoResponseDto`, como já acontece no método `listar`.
3. Leia o parâmetro `limite` da URL; se não for informado, adote o valor **5**.
4. Se `limite` for negativo, responda com HTTP **400** (requisição inválida).
5. Para `limite` válido, devolva HTTP **200** e um array JSON, mesmo quando vazio.

**Dicas de JAX-RS:** `@GET`, `@Path`, `@QueryParam` e `@DefaultValue` ajudam a implementar o endpoint. Lembre-se de declarar o caminho literal `baixo-estoque` e testá-lo separadamente da rota dinâmica `/{id}`.

## 7. Tarefa D — Testar no Postman (10 minutos)

Execute os cenários abaixo e registre status + quantidade retornada:

| Teste | Chamada | Esperado no H2 inicial |
|---|---|---|
| 1 | `GET /produtos/baixo-estoque` | `200`, 2 produtos: Pasta e Caderno |
| 2 | `GET /produtos/baixo-estoque?limite=0` | `200`, apenas Pasta |
| 3 | `GET /produtos/baixo-estoque?limite=20` | `200`, 3 produtos |
| 4 | `GET /produtos/baixo-estoque?limite=-1` | `400` |
| 5 | `GET /produtos` | `200`, endpoint antigo ainda funciona |

Confira a ordenação pela coluna `quantidade`, não pela ordem de criação. Os dados vêm de `src/main/resources/import.sql`.

## 8. Conclusão e critérios de aceitação

- [ ] O projeto compila e o Quarkus inicia.
- [ ] A entidade continua mapeando a tabela `produtos`.
- [ ] A consulta Panache filtra diretamente no banco e ordena.
- [ ] O endpoint de baixo estoque funciona com limite padrão 5.
- [ ] O limite negativo devolve HTTP 400.
- [ ] Os endpoints CRUD existentes permanecem funcionais.

## 9. Desafios opcionais (+10 minutos)

**Extra A:** crie um teste com `@QuarkusTest` e REST Assured para a consulta de baixo estoque. Verifique o HTTP 200 e os dois produtos de exemplo.

**Extra B:** crie um produtoBo com `POST /produtos` e quantidade 2; consulte novamente o baixo estoque para observar o resultado. Em seguida, remova com `DELETE /produtos/{id}`.

**Extra C — Pesquisa individual:** Panache usa HQL ou SQL no `find` desse exemplo? Qual a diferença entre filtrar no banco e chamar `stream().filter(...)` numa lista de todos os produtos?

> **Orientação:** se usar o módulo em PostgreSQL via Docker Compose, o banco começa sem dados de exemplo. Cadastre produtos antes de executar os testes da tabela. O laboratório foi pensado para **H2 dev**.


## Pesquisa complementar (opcional)

Compare a implementação `ProdutoRepository` ativa com o arquivo `docs/aula-05/exemplos/ProdutoActiveRecordExemplo.java`, que **não deve ser ativado durante o laboratório**. Identifique a diferença entre `repository.persist(entity)` e `entity.persist()` e explique por que a entidade `PanacheEntity` exige cuidado com a estratégia de geração do ID existente no projeto.
