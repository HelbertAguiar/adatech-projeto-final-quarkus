# Panache: Repository, Active Record e Hibernate tradicional

**O projeto funcional usa `ProdutoRepository implements PanacheRepository<ProdutoEntity>`**. O exemplo Active Record está em [`exemplos/ProdutoActiveRecordExemplo.java`](exemplos/ProdutoActiveRecordExemplo.java) e tem `@Entity`/`@Table` comentadas. Está em `docs/`, não entra na compilação e não cria endpoint novo.

## Mesmo CRUD, duas formas de aceder aos dados

| Ação | Repository (ativo) | Active Record (alternativa) |
| --- | --- | --- |
| Buscar | `repository.findById(id)` | `ProdutoActiveRecordExemplo.findById(id)` |
| Listar | `repository.listAll()` | `ProdutoActiveRecordExemplo.listAll()` |
| Criar | `repository.persist(entity)` | `entity.persist()` |
| Excluir | `repository.deleteById(id)` | `ProdutoActiveRecordExemplo.deleteById(id)` |
| Buscar por nome | `repository.buscarPorNome(nome)` | `ProdutoActiveRecordExemplo.buscarPorNome(nome)` |

### Como habilitar o exemplo (somente exercício separado)

1. Criar uma branch/cópia do projeto; mover o exemplo para `src/main/java/studio/catalogo/produtos/repository/`.
2. Descomentar `@Entity` e `@Table`; **retirar ou desativar o mapeamento de `ProdutoEntity`** e adaptar `ProdutoService`/testes ao Active Record; somente comentar anotações não é suficiente para preservar o funcionamento do projeto.
3. Atenção: `PanacheEntity` possui `Long id` gerado automaticamente, enquanto `ProdutoEntity` usa `@GeneratedValue(strategy=IDENTITY)`. Adequar a geração de ID e recriar o schema do ambiente *didático* conforme o novo mapeamento antes do teste. **Não fazer migração destrutiva num banco com dados reais.** Para manter `IDENTITY` explícito no projeto corporativo, usar `PanacheEntityBase` e declarar `@Id/@GeneratedValue` manualmente.
4. Refatorar temporariamente o serviço para chamar operações estáticas da entidade. Manter `@Transactional` nas escritas. Nunca habilitar as duas implementações no mesmo CRUD sem adaptação.

## Limitações e trade-offs

- **Panache não substitui Hibernate/JPA**: usa a mesma unidade de persistência, transações, fetch e mapeamentos. Em consultas complexas pode usar `EntityManager` no próprio repository.
- **Active Record** reduz classes, mas coloca persistência na entidade e depende de métodos estáticos, podendo aumentar acoplamento e dificultar alguns tipos de isolamento/teste. Repository deixa a regra de negócio dependente de uma abstração injetável.
- **JPQL abreviado do Panache** (`find("nome", nome)`) é conveniente para consultas simples, mas filtros combináveis, joins complexos e critérios programáticos podem ficar mais verbosos e mais fáceis de errar usando strings concatenadas. O Criteria API e o metamodel JPA são alternativas; veja [`ProdutoCriteriaQueryExemplo.java`](../../src/main/java/studio/catalogo/produtos/repository/ProdutoCriteriaQueryExemplo.java).
- **`PanacheEntity` pressupõe a sua própria estratégia padrão de ID**. `PanacheEntityBase` ou entidades JPA convencionais dão mais controle de chaves/IDs.
- **Performance**: Panache não elimina N+1, SQL caro ou paginação. Essas limitações vêm do acesso ORM/SQL e exigem `join fetch`, projeções, paginação, índices e análise de queries conforme necessário.

### Demonstração curta: consulta dinâmica

Requisito: buscar produtos com filtros opcionais `nome`, `precoMin` e `quantidadeMin`.

Com Panache uma consulta simples é:

```java
repository.find("lower(nome) like ?1", "%caderno%").list();
```

Quando os três filtros são opcionais, normalmente montamos uma condição dinamicamente e fazemos bind de cada parâmetro, ou usamos `EntityManager`/Criteria. Compare com o exemplo Criteria real. **Não é uma impossibilidade do Panache**, mas uma diferença de ergonomia e controle para consultas sofisticadas.
