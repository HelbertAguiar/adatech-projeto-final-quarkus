# Aula 06 — Bean Validation e tratamento de erros

Esta aula continua o CRUD persistido em `/produtos` da Aula 05, sem modificar o fluxo remoto `/products` das Aulas 03/04.

- [Roteiro do professor e respostas das pesquisas](ROTEIRO-PROFESSOR.md)
- [Laboratório de 30–50 minutos — versão do aluno](LABORATORIO.md)
- [Postman — demo com testes automatizados](postman/AULA-06-CATALOGO.postman_collection.json)
- [Guia rápido de execução e respostas HTTP](DEMO-PASSO-A-PASSO.md)

## Classes implementadas

```text
produtos/
  boundary/ProdutoRequestDto.java                # @NotBlank, @Size, @NotNull, @DecimalMin, @Min...
  boundary/RetiradaEstoqueRequestDto.java         # @NotNull, @Positive
  boundary/ProdutoResource.java                   # POST /produtos/{id}/retiradas
  domain/ProdutoNaoEncontradoException.java      # 404 no boundary
  domain/EstoqueInsuficienteException.java        # 409 no boundary
  domain/ProdutoService.java                      # regra de retirada + @Transactional
platform/error/
  ErroResponse.java, DetalheErro.java, ErroHttp.java
  ValidacaoExceptionMapper.java                   # 400 (entrada inválida)
  ProdutoNaoEncontradoMapper.java                 # 404
  EstoqueInsuficienteMapper.java                  # 409
```

> A dependência `quarkus-hibernate-validator` já existia no POM da Aula 05 e foi reaproveitada; **não é necessário instalar outra extensão para a Aula 06**.

O exemplo focaliza 400, 404 e 409. Outros erros do framework (por exemplo, JSON malformado, rotas inexistentes ou falhas internas) não são cobertos pelo envelope customizado deste laboratório. Em sistemas reais, considere padronização mais ampla sem expor dados internos.
