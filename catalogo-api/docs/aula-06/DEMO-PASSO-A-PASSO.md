# Demo passo a passo — Aula 06

## Pré-requisitos

Java 21, Maven 3.9+, Postman (opcional). **O estoque-local não é necessário** para esta demo.

```powershell
mvn clean test
mvn quarkus:dev
```

O Quarkus usa H2 em desenvolvimento e expõe a API em `http://localhost:9091`.

## 1. Teste o Bean Validation (400)

```http
POST http://localhost:9091/produtos
Content-Type: application/json

{"nome":"","preco":-2,"quantidade":-1}
```

Resposta esperada: HTTP **400**, código `DADOS_INVALIDOS` e detalhes dos campos `nome`, `preco` e `quantidade`. Nenhum produto é persistido.

## 2. Teste o ExceptionMapper de recurso ausente (404)

```http
GET http://localhost:9091/produtos/99999999
```

Resposta esperada: HTTP **404**, código `PRODUTO_NAO_ENCONTRADO`.

## 3. Crie um produto de estoque pequeno (201)

```http
POST http://localhost:9091/produtos
Content-Type: application/json

{"nome":"Mouse Aula 06","preco":49.90,"quantidade":2}
```

Guarde o `id` da resposta (use nos próximos exemplos em vez de `{id}`).

## 4. Teste o Bean Validation específico da retirada (400)

```http
POST http://localhost:9091/produtos/{id}/retiradas
Content-Type: application/json

{"quantidade":0}
```

Resposta esperada: HTTP **400**, código `DADOS_INVALIDOS` e detalhe `quantidade`.

## 5. Teste o erro de domínio — estoque insuficiente (409)

```http
POST http://localhost:9091/produtos/{id}/retiradas
Content-Type: application/json

{"quantidade":5}
```

Resposta esperada: HTTP **409**, código `ESTOQUE_INSUFICIENTE`. O estoque permanece em **2**.

## 6. Teste o caso de sucesso (200)

```http
POST http://localhost:9091/produtos/{id}/retiradas
Content-Type: application/json

{"quantidade":1}
```

Resposta esperada: HTTP **200** com quantidade igual a **1**. Confirme com `GET /produtos/{id}`.

## 7. Compare o envelope de erro

O exemplo utiliza o mesmo contrato JSON nos mappers de 400, 404 e 409:

```json
{
  "codigo": "ESTOQUE_INSUFICIENTE",
  "mensagem": "Estoque insuficiente: disponível=2, solicitado=5.",
  "timestamp": "2026-10-28T14:00:00Z",
  "path": "/produtos/4/retiradas",
  "detalhes": []
}
```

Os campos `timestamp` e `path` são gerados dinamicamente. Na validação 400, `detalhes` contém pares `campo` / `mensagem`.

### Atenção

- A mensagem de conflito de negócio pode ser pública porque contém apenas informações do estoque do exemplo. **Não exponha mensagens de exceções de sistema, SQL, tokens ou stack trace**.
- O endpoint usa `@Transactional` e dirty checking do Hibernate. **Não implementa locking concorrente**; numa aplicação real, use controle de concorrência (`@Version` ou atualização atômica) para impedir retiradas simultâneas inconsistentes.
- Para testar o projeto com PostgreSQL, consulte `docs/producao/README.md`.
