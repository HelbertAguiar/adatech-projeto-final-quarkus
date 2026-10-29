# Aula 05 — Persistência com Hibernate ORM e Panache

> **Material do professor — aula de aproximadamente 3 horas**  
> Continuação direta do código da Aula 03 e da Aula 04. Java 21, Quarkus 3.40.1, porta 9091.

## 1. Objetivos e arquitetura (10 min)

Ao terminar, o aluno deve conseguir:

1. Explicar o mapeamento objeto-relacional: classe Java ↔ tabela SQL; instância ↔ linha.
2. Reconhecer as anotações `@Entity`, `@Table`, `@Id`, `@GeneratedValue` e `@Column`.
3. Configurar JDBC, datasource e um pequeno pool de conexões.
4. Implementar operações `persist`, `listAll`, `find`, `findById` e `deleteById` com `PanacheRepository`.
5. Usar `@Transactional` ao gravar dados.
6. Executar e testar um CRUD REST.
7. Comparar H2 em memória e PostgreSQL persistente.

```text
                  catalogo-api :9091
                         |
          +--------------+----------------+
          |                               |
    GET /products                   /produtos (CRUD)
   (Aulas 03 e 04)                    (Aula 05)
          |                               |
    ProductService                 ProdutoResource
    @Retry/@Fallback                   |
          |                         ProdutoService
     EstoqueGateway                     |
    REST Client HTTP              ProdutoRepository
          |                        PanacheRepository
   estoque-local :8081                  |
                                Hibernate ORM / JDBC
                                        |
                           H2 (dev/test) ou PostgreSQL (Docker/prod)
```

**Por que dois módulos?** Os produtos obtidos de `/products` representam um catálogo calculado a partir do microserviço de estoque externo. Os produtos salvos via `/produtos` são registros *locais* e não sincronizam automaticamente o estoque externo. Essa separação evita misturar duas fontes de dados diferentes numa aula de introdução.

## 2. Sequência de 3 horas (sugestão)

| Min | Atividade |
|---|---|
| 0–15 | Revisão de `/products` (remoto) versus `/produtos` (persistência) |
| 15–40 | Hibernate ORM/JPA: entidade, tabela, chave primária, SQL gerado |
| 40–65 | Dependências, H2, datasource, pool e profiles |
| 65–90 | `ProdutoRepository` + `ProdutoService`, operações e `@Transactional` |
| 90–105 | Intervalo |
| 105–130 | Postman: CRUD completo, respostas HTTP e validação |
| 130–155 | PostgreSQL com Docker Compose, SQL e volume persistente |
| 155–175 | Laboratório dos alunos (`LABORATORIO.md`) |
| 175–180 | Fechamento e pesquisa individual |

Se quiser reservar 40–50 minutos para laboratório, reduza a demonstração do PostgreSQL a um resumo e transfira a execução para depois da aula.

## 3. O que foi adicionado ao `pom.xml` (10 min)

| Extensão | Finalidade |
|---|---|
| `quarkus-hibernate-orm-panache` | Hibernate ORM/Jakarta Persistence + métodos Panache para repositórios |
| `quarkus-jdbc-h2` | Driver H2; exemplo sem instalar banco no computador |
| `quarkus-jdbc-postgresql` | Driver PostgreSQL; execução com Docker Compose |
| `quarkus-hibernate-validator` | Validação de payload com `@Valid`, `@NotBlank`, `@NotNull`, `@Min` etc. |

A extensão Panache **não é outro ORM**: ela simplifica o uso do Hibernate ORM. Continuamos utilizando transações, SQL e conceitos de JPA.

## 4. Entidade e mapeamento objeto-relacional (25 min)

Abrir `src/main/java/studio/catalogo/produtos/repository/ProdutoEntity.java`:

```java
@Entity                     // esta classe sera persistida no banco
@Table(name = "produtos")   // nome da tabela
public class ProdutoEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;         // chave primaria gerada pelo banco

    @Column(nullable = false, length = 120)
    public String nome;

    @Column(nullable = false, precision = 10, scale = 2)
    public BigDecimal preco;

    @Column(nullable = false)
    public int quantidade;
}
```

| Anotação | Explicação |
|---|---|
| `@Entity` | Marca a classe como entidade gerenciada pelo Hibernate |
| `@Table(name = "produtos")` | Define a tabela da entidade |
| `@Id` | Identifica a chave primária |
| `@GeneratedValue(IDENTITY)` | Banco atribui o próximo ID gerado |
| `@Column` | Controla características da coluna, como tamanho, precisão e nulidade |

**Esclarecimento:** `@Entity`, `@Id` e `@Column` são da especificação **Jakarta Persistence**. Hibernate é sua implementação. Panache acrescenta métodos convenientes sobre Hibernate/JPA.

```text
ProdutoEntity { id=1, nome="Caderno", preco=12.50, quantidade=5 }
                             |
                             v
SQL: SELECT id, nome, preco, quantidade FROM produtos WHERE id = 1;
```

`ProdutoRequestDto` é o JSON de entrada; `ProdutoResponseDto` é o JSON de saída; `ProdutoBo` é o objeto simples de domínio; `ProdutoEntity` representa a linha persistida. A conversão para DTO HTTP ocorre em `boundary`, evitando que o serviço de domínio dependa do contrato REST.

## 5. `application.properties`: datasource, pool e schema (25 min)

Abra `src/main/resources/application.properties`:

```properties
# Base para DEV, TEST e staging didatico: H2 em memoria.
quarkus.datasource.db-kind=h2
quarkus.datasource.jdbc.url=jdbc:h2:mem:catalogo;DB_CLOSE_DELAY=-1
quarkus.datasource.username=sa
quarkus.datasource.password=

# Agroal: gerenciador de conexoes JDBC do Quarkus.
quarkus.datasource.jdbc.min-size=1
quarkus.datasource.jdbc.max-size=5

# Desenvolvimento e testes: recria schema e insere dados de exemplo.
%dev.quarkus.hibernate-orm.schema-management.strategy=drop-and-create
%test.quarkus.hibernate-orm.schema-management.strategy=drop-and-create
%dev.quarkus.hibernate-orm.sql-load-script=import.sql
%test.quarkus.hibernate-orm.sql-load-script=import.sql

# Executavel empacotado em modo NORMAL usa PostgreSQL.
%prod.quarkus.datasource.db-kind=postgresql
%prod.quarkus.datasource.jdbc.url=${DB_URL:jdbc:postgresql://localhost:5432/catalogodb}
%prod.quarkus.datasource.username=${DB_USERNAME:catalogo}
%prod.quarkus.datasource.password=${DB_PASSWORD}
%prod.quarkus.hibernate-orm.schema-management.strategy=validate
```

**Definições:**

- `db-kind`: determina o banco/dialeto e o driver selecionado.
- `jdbc.url`: endereço JDBC; `mem:` significa H2 na memória do processo.
- `username`/`password`: credenciais (não commitar credenciais reais).
- `min-size`/`max-size`: mínimo/máximo de conexões mantidas pelo pool **Agroal**, não quantidade de consultas.
- `drop-and-create`: elimina e recria as tabelas em desenvolvimento; **apaga dados existentes**. Não use em produção.
- `import.sql`: carrega os 3 produtos locais de exemplo durante a inicialização de dev/test (há configuração também para `staging`).
- `validate`: verifica se o schema já existe e corresponde ao mapeamento, sem modificá-lo.
- `DB_URL`/`DB_USERNAME`/`DB_PASSWORD`: configuração recebida do ambiente quando em modo `prod`.

**Onde os dados ficam?** `H2` usa memória e perde os dados quando o processo termina. `PostgreSQL`, iniciado pelo Compose, usa o volume `dados-postgres` e mantém os dados entre reinicializações normais.

**O que é um pool?** Sem pool, cada consulta poderia abrir uma nova conexão física (caro); com Agroal, conexões JDBC são mantidas e reutilizadas. Uma operação HTTP não equivale necessariamente a uma conexão nova.

**Observação sobre profiles:** `mvn quarkus:dev` usa o modo de desenvolvimento e, por padrão, profile `dev`. `mvn quarkus:dev "-Dquarkus.profile=staging"` ativa um profile chamado `staging`, mas permanece em modo de desenvolvimento. O modo normal (JAR/container) usa `prod` e PostgreSQL. `db-kind` é configuração de build-time: não trate a troca entre H2 e PostgreSQL como mero override em runtime de um binário já construído.

## 6. Repositório Panache e transações (25 min)

Abra `ProdutoRepository.java`:

```java
@ApplicationScoped
public class ProdutoRepository implements PanacheRepository<ProdutoEntity> {
    public List<ProdutoEntity> buscarPorNome(String trecho) {
        return find("lower(nome) like ?1 order by id", "%" + trecho.toLowerCase() + "%").list();
    }
}
```

O `PanacheRepository<ProdutoEntity>` oferece, sem boilerplate de `EntityManager`:

| Chamada | Efeito |
|---|---|
| `repository.listAll()` | Retorna todos os registros |
| `repository.findById(id)` | Encontra pelo ID ou devolve `null` |
| `repository.find("nome", "Caderno").list()` | Executa busca simplificada (HQL) |
| `repository.persist(entidade)` | Marca um novo objeto para inserção |
| `repository.deleteById(id)` | Exclui por ID, retorna `boolean` |
| `repository.count()` | Conta registros |

Abra `ProdutoService.java`. Compare `criar`, `buscar`, `atualizar` e `excluir`.

```java
@Transactional
public Produto atualizar(Long id, String nome, BigDecimal preco, int quantidade) {
    ProdutoEntity entity = repository.findById(id);
    if (entity == null) return null;

    entity.nome = nome.trim();
    entity.preco = preco;
    entity.quantidade = quantidade;
    return toDomain(entity);
}
```

**Separação de responsabilidades:** o método acima retorna `ProdutoBo` do domínio; `ProdutoResource` o transforma em `ProdutoResponseDto`, sem expor `ProdutoEntity` pela REST API.

**Por que não existe `save` no UPDATE?** O objeto buscado é gerenciado pelo EntityManager enquanto a transação está ativa. O Hibernate detecta alterações (*dirty checking*) e as sincroniza com o banco até o commit. `@Transactional` abre/associa uma transação e faz commit ou rollback. Os métodos de escrita devem executá-la; leituras simples não precisam dela neste exemplo.

**Panache x Hibernate tradicional:** a API Panache diminui código repetitivo; Hibernate/JPA oferece controle detalhado sobre EntityManager, lifecycle e queries. A escolha do padrão Repository ajuda a separar acesso a dados da regra de negócio. Panache não elimina custo de consultas nem problemas N+1, pool, indexação ou transações.

## 7. Demonstração REST com Postman (25 min)

**Inicialização local — PowerShell (na pasta com `pom.xml`):**

```powershell
mvn quarkus:dev
```

Para o novo CRUD **não é necessário iniciar o estoque-local**. `GET /products` continuará dependendo do estoque; `GET /produtos` consulta o H2.

| Ordem | HTTP | URL | Corpo | Esperado |
|---|---|---|---|---|
| 1 | GET | `http://localhost:9091/produtos` | — | `200`, lista de 3 produtos |
| 2 | GET | `/produtos?nome=cade` | — | `200`, só Caderno |
| 3 | POST | `/produtos` | JSON abaixo | `201`, `Location` com ID |
| 4 | GET | `/produtos/{id}` | — | `200`, produtoBo criado |
| 5 | PUT | `/produtos/{id}` | JSON alterado | `200`, dados atualizados |
| 6 | DELETE | `/produtos/{id}` | — | `204`, sem body |
| 7 | GET | `/produtos/{id}` | — | `404` após remover |
| 8 | POST | `/produtos` | preço negativo | `400` de validação |

POST (e PUT com modificações):

```json
{
  "nome": "Mochila",
  "preco": 79.90,
  "quantidade": 4
}
```

Enviar o header `Content-Type: application/json`. Após POST, copie o `id` retornado (ou use o Postman da aula, que o guarda numa variável). A validação de entrada usa `@Valid` e restrições no `ProdutoRequestDto`.

**Discussão:** 201 representa novo recurso, 200 sucesso com conteúdo, 204 sucesso sem conteúdo, 404 recurso inexistente, 400 corpo inválido. O status HTTP não vem automaticamente do ORM: o `ProdutoResource` escolhe a resposta.

## 8. Demonstração opcional com PostgreSQL no Docker Desktop (25 min)

**Não precisa instalar PostgreSQL localmente.** Docker Desktop deve estar aberto e usando contêineres Linux.

```powershell
# Na raiz do projeto:
docker compose up --build -d

docker compose ps

# Novo CRUD (no PostgreSQL, inicialmente sem registros):
# GET http://localhost:9091/produtos => []
# POST http://localhost:9091/produtos => 201

# Conferir diretamente no banco:
docker compose exec postgres psql -U catalogo -d catalogodb -c "SELECT id, nome, preco, quantidade FROM produtos;"

# Reiniciar a API (mantendo o banco/volume):
docker compose restart catalogo-api

# Os dados persistem apos o restart da API.
docker compose down
```

O arquivo `database/init/01-schema.sql` cria a tabela na **primeira** inicialização do volume. Se esse arquivo for alterado depois, o PostgreSQL não o reexecuta automaticamente em um volume já existente. Em um ambiente de aula, para zerar dados deliberadamente:

```powershell
# ATENCAO: APAGA o volume PostgreSQL e todos os registros da demo!
docker compose down -v
```

**Produção real:** usar migrações versionadas (Liquibase/Flyway), credenciais vindas de Secrets e política de backup. As credenciais fixas do Compose destinam-se apenas à prática local.

**Native Image:** o projeto mantém `docker-compose.native.yml` como override, agora com PostgreSQL externo; H2 em memória não é uma solução para executar o banco embutido dentro da Native Image.

## 9. Perguntas de pesquisa individual e respostas esperadas (10 min)

**1. Hibernate ORM e Jakarta Persistence são a mesma coisa?** Não. Jakarta Persistence (JPA) define API e anotações; Hibernate ORM é uma implementação popular dessa especificação.

**2. O Panache substitui Hibernate?** Não; Panache fornece uma camada de conveniência sobre Hibernate ORM.

**3. `PanacheEntity` versus `PanacheRepository`?** `PanacheEntity` usa padrão Active Record, com operações no próprio modelo; `PanacheRepository` coloca operações em classe dedicada. Nesta aula preferimos Repository por separar responsabilidades.

**4. Por que usar `@Transactional`?** Para coordenar escrita, commit e rollback e garantir consistência transacional. Em atualizações, permite dirty checking sobre entidades gerenciadas.

**5. Por que H2 para iniciar e PostgreSQL no Docker?** H2 reduz dependências e tempo de preparação; PostgreSQL é um SGBD persistente, mais representativo de serviços reais. Atenção: dialetos podem ter diferenças; testes reais devem cobrir também o banco de produção.

**6. Quando não usar `drop-and-create`?** Nunca em um banco de produção com dados valiosos: apagar e recriar o schema destrói registros. Usar Flyway ou Liquibase.

**7. Como Panache reduz boilerplate?** Remove a necessidade de escrever CRUD básico e chamadas repetidas ao `EntityManager`, mas não elimina a lógica de negócio nem a necessidade de entender SQL.

**8. Por que usar DTO de entrada e saída?** Para não expor o estado interno da entidade, controlar quais campos podem ser recebidos e evoluir o contrato REST sem acoplar a persistência.

## 10. Laboratório individual

Entregar aos alunos **`LABORATORIO.md`**. O exercício estende o CRUD existente em vez de exigir a recriação de tudo em 40 minutos.

## 11. Dificuldades frequentes

- **`404` em `/produtos`**: confirmar `mvn quarkus:dev` na raiz do projeto (pasta do `pom.xml`), porta 9091 e path no singular `/produtos`.
- **`GET /products` retorna 503**: é o módulo da Aula 04 e precisa do Estoque Local; o CRUD `/produtos` é independente.
- **`400` em POST**: validar `Content-Type`, `nome`, preço maior que zero e quantidade não negativa.
- **Após reiniciar H2 perdeu alterações**: esperado, é memória; `import.sql` repõe os dados de exemplo.
- **PostgreSQL não inicia no Docker**: `docker compose logs postgres`; conferir Docker Desktop, espaço em disco, credenciais e volume.
- **Schema do PostgreSQL diferente da entidade**: comparar `database/init/01-schema.sql` e `ProdutoEntity.java`; `validate` impede a aplicação de subir se incompatível.
- **`DB_PASSWORD` ausente no modo NORMAL**: setar o ambiente no processo; Compose já injeta essa variável. Não registrar segredos reais no repositório.
- **Erro após mudar o `db-kind` na Native Image**: essa escolha é de build-time; recompilar para a plataforma de banco desejada.

## Referências oficiais

- Panache: https://quarkus.io/guides/hibernate-orm-panache
- Hibernate ORM: https://quarkus.io/guides/hibernate-orm
- Datasource e Agroal: https://quarkus.io/guides/datasource
- Configuração: https://quarkus.io/guides/config-reference


## Complemento — Repository vs Active Record vs Criteria (10–15 min opcionais)

Veja [comparativo detalhado](ACTIVE-RECORD-VS-REPOSITORY.md). A alternativa Active Record com `PanacheEntity` fica **desativada** em `exemplos/ProdutoActiveRecordExemplo.java` (anotações `@Entity` e `@Table` comentadas) para não duplicar o mapeamento da tabela `produtos`. A consulta dinâmica JPA Criteria está em `../../src/main/java/studio/catalogo/produtos/repository/ProdutoCriteriaQueryExemplo.java`. Debater a diferença de ergonomia sem afirmar que Panache não suporta consultas complexas: Panache funciona sobre Hibernate ORM e permite `EntityManager`.
