# Aula 06 — Roteiro do professor (3 horas)

**Problema gerador:** como garantir a integridade dos dados e oferecer feedback consistente ao usuário por meio de validação e tratamento de erros?

**Ponto de partida:** projeto evolutivo das Aulas 03–05. O aluno já conhece `ProdutoResource`, `ProdutoService`, Panache, `@Transactional` e DTOs. Não criar outro microserviço nem exigir Docker para esta aula.

## Objetivos ao final

1. Distinguir validação de entrada de regra de negócio.
2. Usar Bean Validation em records DTOs com `@Valid`.
3. Definir exceções específicas no domínio sem depender de JAX-RS.
4. Utilizar `ExceptionMapper` com `@Provider` para HTTP 404/409/400.
5. Apresentar erro JSON estável com código de máquina, mensagem, timestamp, path e detalhes.
6. Demonstrar os casos no Postman e com testes automatizados.

## Cronograma sugerido

| Tempo | Atividade |
|---|---|
| 0–15 min | Revisão do CRUD Aula 05 e problema de mensagens inconsistentes |
| 15–40 min | Bean Validation em `ProdutoRequestDto`, `RetiradaEstoqueRequestDto` |
| 40–65 min | Exceções de domínio e `ExceptionMapper` |
| 65–80 min | Contrato de erro JSON e princípios de segurança |
| 80–105 min | Demo 400 / 404 / 409 / 200 no Postman |
| 105–150 min | Laboratório prático dos alunos (45 min) |
| 150–165 min | Pesquisa individual e discussão sobre tratamento de erros |
| 165–180 min | Revisão, testes e conexão com requisitos do projeto final |

## 1. O que já existia e o que foi acrescentado

Na Aula 05, `quarkus-hibernate-validator` já estava no POM, assim como o DTO principal usava restrições (`@NotBlank`, `@Size`, `@DecimalMin`, `@Digits`, `@Min`) e `@Valid`. A Aula 06 ensina **o significado desses elementos e como lidar com o retorno de falhas**.

Novidades: `RetiradaEstoqueRequestDto`, novo endpoint `POST /produtos/{id}/retiradas`, `ProdutoNaoEncontradoException`, `EstoqueInsuficienteException`, três exception mappers e `ErroResponse`.

## 2. Bean Validation, conceitos e anotação por anotação

| Anotação | Responsabilidade | Exemplo |
|---|---|---|
| `@Valid` | Aciona validação em cascata no payload | `@Valid ProdutoRequestDto request` |
| `@NotNull` | Proíbe valor ausente/nulo | `preco` e `quantidade` |
| `@NotBlank` | Proíbe strings vazias, nulas ou só espaços | `nome` |
| `@Size(max=120)` | Controla comprimento da string | `nome` |
| `@DecimalMin(value="0.00", inclusive=false)` | Impõe preço positivo | `preco` |
| `@Digits(integer=8,fraction=2)` | Limita precisão decimal | `preco` |
| `@Min(0)` | Impede saldo negativo | `quantidade` no CRUD |
| `@Positive` | Exige valor estritamente maior que zero | `quantidade` na retirada |

Demonstração: `POST /produtos` com `nome=""`, `preco=-2`, `quantidade=-1`. Mostrar que não é necessário escrever `if` manual para cada constraint.

**Por que `@NotNull` + `@Positive`?** A restrição `@Positive` não torna o campo obrigatório; `@NotNull` garante que foi informado.

**Como o Quarkus lida?** A extensão `quarkus-hibernate-validator` executa a validação nos parâmetros do endpoint. Se falhar, ocorre uma exceção de validação e o método não executa a persistência.

## 3. Bean Validation x regra de negócio

- **Validação declarativa (400):** tipo, formato e restrições simples do request (`quantidade > 0`).
- **Regra de negócio (409):** a quantidade requerida pode ser válida, mas **não há unidades suficientes**; depende do estado atual no banco.
- **Recurso não encontrado (404):** ID não existe; é uma situação de negócio convertida para HTTP no boundary.

Não coloque `Response`, `@Path` ou tipos HTTP nas exceções do domínio. O `ProdutoService.retirarEstoque` verifica o saldo e lança `EstoqueInsuficienteException`; o mapper, fora do domínio, decide HTTP 409.

## 4. Classes e fluxo de execução

```text
POST /produtos/{id}/retiradas
  -> ProdutoResource (@Valid request)
     -> se inválido: ValidacaoExceptionMapper -> 400 + detalhes
  -> ProdutoService.retirarEstoque (@Transactional)
     -> não existe: ProdutoNaoEncontradoException -> mapper -> 404
     -> estoque insuficiente: EstoqueInsuficienteException -> mapper -> 409
     -> sucesso: entidade.quantidade -= quantidade; commit -> 200
```

- `@Provider`: registra o mapper no Jakarta REST.
- `ExceptionMapper<T>`: define o tipo de exceção e o método `toResponse`.
- `@Context UriInfo`: obtém o caminho HTTP atual para o erro.
- `ErroHttp`: criação centralizada do objeto JSON e status.
- `@Transactional`: delimita transação no serviço; se a exceção não verificada surgir antes da mutação, não haverá alteração; rollback aplica-se por padrão a `RuntimeException`.
- `ResteasyReactiveViolationException`: exceção específica de violações de parâmetros HTTP. O mapper específico preserva o tratamento distinto de validações internas, que não devem automaticamente gerar 400.

**Por que não `ExceptionMapper<Exception>` genérico?** Pode esconder bugs, expor detalhes e tratar acidentalmente exceções não relacionadas. Em sistemas reais, um fallback geral de 500 deve retornar mensagem neutra e registrar o incidente.

## 5. Resposta padronizada

```json
{
  "codigo": "DADOS_INVALIDOS",
  "mensagem": "Verifique os dados enviados.",
  "timestamp": "2026-10-28T14:00:00Z",
  "path": "/produtos",
  "detalhes": [
    { "campo": "nome", "mensagem": "Nome é obrigatório" }
  ]
}
```

**Por que usar códigos estáveis?** O frontend automatiza o tratamento de `codigo`, enquanto a equipe pode alterar a mensagem de apresentação sem quebrar o contrato. Nunca usar mensagens de erro SQL para feedback público.

## 6. Demonstração

Siga [o passo a passo](DEMO-PASSO-A-PASSO.md) ou importe a coleção `postman/AULA-06-CATALOGO.postman_collection.json`. Demonstre 400, 404, 409 e 200, confirmando depois do conflito que o banco não sofreu alteração.

## 7. Pesquisa individual — perguntas e respostas esperadas

**1. Quando usar Bean Validation, e quando lançar uma exceção de negócio?** Use Bean Validation para propriedades do dado de entrada (obrigatório, comprimento, valor positivo). Use uma exceção de negócio para uma condição dependente de estado/invariante: estoque disponível, agendamento conflitante, saldo insuficiente.

**2. Qual diferença entre HTTP 400, 404 e 409?** 400 para entrada inválida; 404 quando não existe o recurso procurado; 409 quando a operação conflita com o estado atual de um recurso. Há nuances por domínio, por isso documentar o contrato.

**3. Quais informações mostrar ao cliente e quais manter nos logs?** Ao cliente: código seguro, mensagem clara, campo (quando aplicável), identificador de correlação, status. Logs internos: exceção completa, contexto técnico seguro e correlation ID. Não retornar credenciais, stack traces, consultas SQL ou informações pessoais.

**4. `ExceptionMapper` substitui `@Retry`/`@Fallback`?** Não. `ExceptionMapper` adapta uma falha para resposta HTTP; Fault Tolerance tenta recuperar/mitigar uma falha antes que ela chegue à fronteira HTTP.

**5. E no Spring Boot?** `@RestControllerAdvice` com `@ExceptionHandler` centraliza respostas HTTP. No Quarkus, `@Provider` + `ExceptionMapper<T>` (ou `@ServerExceptionMapper`) cumprem finalidade semelhante. As anotações e integrações diferem.

**6. Por que evitar `ExceptionMapper<ConstraintViolationException>` que trata TODA violação como 400?** Uma validação disparada internamente num serviço pode indicar erro do próprio sistema (500), não necessariamente entrada inválida. O exemplo usa o tipo especializado das violações HTTP do Quarkus.

## 8. Desafio em sala

O aluno implementa uma operação de **reposição de estoque**, combinando validação declarativa e regra de negócio. Ver [laboratório](LABORATORIO.md) para critérios e teste esperado. O roteiro não entrega a solução completa.

## 9. Limitações didáticas e recomendações de produção

- O exemplo de retirada usa leitura + atualização JPA em transação, sem `@Version` nem locking. **Requisições simultâneas podem causar inconsistência**. Em produção, use controle de concorrência e testes apropriados.
- Nem todo erro de Jackson, framework ou infraestrutura está padronizado neste exemplo. O objetivo é ensinar primeiro os três cenários controlados.
- Persistência em H2 é suficiente para a aula; PostgreSQL no Docker é opcional.
- Não confundir anotações `@NotNull` do Hibernate ORM/JPA com Jakarta Bean Validation. Verificar imports.

## Referências oficiais

- [Quarkus: Validation with Hibernate Validator](https://quarkus.io/guides/validation)
- [Quarkus: Writing REST services — Exception mapping](https://quarkus.io/guides/rest)
- [Jakarta Bean Validation](https://jakarta.ee/specifications/bean-validation/)
