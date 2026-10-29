# Aula 05 — Hibernate ORM e Panache

- [Roteiro do professor](ROTEIRO-PROFESSOR.md)
- [Mapa da refatoração e demonstração da arquitetura](REESTRUTURACAO.md)
- [Laboratório dos alunos](LABORATORIO.md)
- [Postman Aula 05](postman/AULA-05-CATALOGO.postman_collection.json)
- [Active Record vs Repository vs Hibernate](ACTIVE-RECORD-VS-REPOSITORY.md)
- [Exemplo Active Record desativado](exemplos/ProdutoActiveRecordExemplo.java)
- [Exemplo Criteria API tradicional](../../src/main/java/studio/catalogo/produtos/repository/ProdutoCriteriaQueryExemplo.java)
- [Guia de produção local com Docker](../producao/README.md)

**Nota:** o módulo de `/produtos` (PostgreSQL/H2) não sincroniza os registros do serviço remoto `/products` (Aulas 03/04). A refatoração mudou apenas pacotes Java; contratos REST preservados.

**Arquitetura da versão atual:** `produtos/boundary`, `produtos/domain`, `produtos/repository`. O serviço devolve `ProdutoBo` e a fronteira HTTP converte para `ProdutoResponseDto`. Veja [mapa completo dos pacotes](../ARQUITETURA.md).
