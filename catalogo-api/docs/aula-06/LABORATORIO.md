# Laboratório — Aula 06: validação, regra de negócio e tratamento de erros

**Duração:** 30–50 minutos | **Formato:** individual ou em dupla | **Entrega:** código + evidências HTTP | **Sem gabarito nesta versão**.

## Contexto

Na Aula 05, o catálogo passou a persistir produtos. Na Aula 06, adicionamos uma operação de retirada de estoque: `POST /produtos/{id}/retiradas`. Agora, sua missão é implementar **a operação inversa**, para repor unidades no estoque sem aceitar uma requisição inválida.

## Objetivo do desafio

Criar:

```http
POST /produtos/{id}/reposicoes
Content-Type: application/json

{"quantidade":3}
```

Se o produto atual tiver 2 unidades, após a operação ele deverá ter **5**. A resposta de sucesso deverá ser HTTP 200 com a representação atualizada de produto.

## Etapas

### 1. Entenda a implementação existente (5 minutos)

Localize os arquivos:

- `produtos/boundary/RetiradaEstoqueRequestDto.java`
- `produtos/boundary/ProdutoResource.java`
- `produtos/domain/ProdutoService.java`
- `platform/error/*Mapper.java`

Identifique **onde a validação acontece**, **onde a regra de negócio é aplicada** e **quem transforma a exceção em HTTP**.

### 2. Defina o contrato (5–10 minutos)

Escolha reutilizar o DTO da retirada ou criar um DTO dedicado. É obrigatório que a quantidade seja informada e seja maior que zero. Utilize Bean Validation; evite validação repetida com `if` no Resource.

### 3. Implemente o serviço e o endpoint (10–15 minutos)

No serviço de produtos, crie uma operação de reposição com `@Transactional`:

- Carregar o produto por ID;
- Se não existir, aproveitar a exceção de domínio e o mapper de 404;
- Somar a quantidade recebida ao saldo;
- Devolver o produto atualizado.

No Resource, crie `POST /produtos/{id}/reposicoes` e aplique `@Valid`.

**Desafio de integridade:** evite overflow do tipo `int` ao somar o estoque. Se a soma ultrapassar o limite, lance uma exceção de negócio e defina um status HTTP coerente; é permitido justificar sua escolha no README.

### 4. Teste no Postman (10 minutos)

| Cenário | Corpo | Resultado esperado |
|---|---|---|
| Reposição válida | `{"quantidade":3}` | 200, saldo aumentado |
| Quantidade zero | `{"quantidade":0}` | 400 + `codigo=DADOS_INVALIDOS` |
| Quantidade ausente | `{}` | 400 + detalhe `quantidade` |
| Produto inexistente | `{"quantidade":1}` | 404 + `codigo=PRODUTO_NAO_ENCONTRADO` |

### 5. Evidência e reflexão (5 minutos)

Registre no README ou num arquivo Markdown os exemplos de 200, 400 e 404 e responda:

1. Por que `@Positive` não substitui `@NotNull`?
2. Por que o Resource não deveria tratar toda exceção com `try/catch`?
3. Qual é a vantagem de usar um contrato JSON de erro padronizado?

## Critérios de conclusão

- [ ] Endpoint HTTP implementado com rota REST coerente.
- [ ] Corpo validado com anotações Jakarta Bean Validation.
- [ ] Regra de reposição implementada no `ProdutoService`.
- [ ] Escrita dentro de transação.
- [ ] HTTP 400, 404 e 200 reproduzidos.
- [ ] Retornos de erro seguem o envelope já usado pela API.

## Bônus (para quem concluir antes dos 50 minutos)

Explique como `@Version` (optimistic locking) poderia melhorar a consistência em acessos concorrentes. Não é obrigatório implementar.

> Dica: você **não precisa criar um ExceptionMapper novo para o 404** se reutilizar a exceção de domínio já existente. Esse é um benefício do tratamento centralizado.
