# Subir o ambiente prod-like com Docker Desktop e PostgreSQL

> **Produção local para demonstração**: usa o profile `prod` (JVM otimizada, sem hot reload) e PostgreSQL persistente. **Não constitui uma infraestrutura de produção pública**: sem TLS, autenticação, backups ou secret manager. Os endereços publicados no host usam `127.0.0.1`.

## 1. Pré-requisitos

- Docker Desktop em modo **Linux containers**, com Compose v2 habilitado.
- Portas `9091` (API) e `8081` (Estoque) livres no Windows.
- Executar os comandos na **raiz do projeto** (diretório que contém `pom.xml` e `docker-compose.yml`).
- Não precisa instalar Maven, JDK ou PostgreSQL no computador para executar os contêineres.

## 2. Configurar a senha (PowerShell)

```powershell
Copy-Item .env.example .env
notepad .env
```

Troque `CATALOGO_DB_PASSWORD` por uma senha local. **Não publique `.env` no Git nem use este exemplo na Internet.** Compose usa o arquivo para substituir variáveis do YAML. Para verificar a configuração sem mostrar senhas no ecrã, confira apenas os serviços:

```powershell
docker compose config --services
```

## 3. Build das imagens e inicialização

```powershell
docker compose up --build -d
docker compose ps
```

Isso inicia **três contêineres** na rede `interna`:

1. `postgres`: cria `catalogodb`, recebe esquema inicial de `database/init/01-schema.sql` e armazena dados no volume `catalogo_pgdata`.
2. `estoque-local`: servidor HTTP do JDK usado por `/products`.
3. `catalogo-api`: compilada pelo Maven dentro do Docker, inicia com profile `prod` e recebe `DB_URL=jdbc:postgresql://postgres:5432/catalogodb`.

**Por que não `localhost`?** Dentro do contêiner da API, `localhost` é a **própria API**. O host `postgres` é o nome do serviço Docker resolvido na rede compartilhada. A porta `5432` **não é exposta ao Windows**.

## 4. Testes rápidos (PowerShell)

```powershell
Invoke-RestMethod http://localhost:9091/info
Invoke-RestMethod http://localhost:9091/q/health/live
Invoke-RestMethod http://localhost:9091/q/health/ready
Invoke-RestMethod http://localhost:9091/produtos
Invoke-RestMethod http://localhost:9091/products
```

`GET /produtos` devolve `[]` **na primeira execução** do PostgreSQL, porque `import.sql` só é executado no H2 dev/test. Para criar um produtoBo:

```powershell
$body = @{ nome='Mochila'; preco=79.90; quantidade=4 } | ConvertTo-Json
Invoke-RestMethod -Uri http://localhost:9091/produtos -Method POST -ContentType 'application/json' -Body $body
Invoke-RestMethod http://localhost:9091/produtos
```

Inspecione o banco com `psql` **dentro** do contêiner:

```powershell
docker compose exec postgres sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "SELECT id, nome, preco, quantidade FROM produtos;"'
```

Alternativa mais simples com os valores padrão do `.env.example` (se não alterou nome e usuário):

```powershell
docker compose exec postgres psql -U catalogo -d catalogodb -c 'SELECT * FROM produtos;'
```

Verifique logs:

```powershell
docker compose logs -f catalogo-api
# Ctrl+C para parar de acompanhar os logs (contêineres continuam em execução).
```

## 5. Persistência e paragem

```powershell
docker compose restart catalogo-api    # dados continuam no volume do PostgreSQL
docker compose down                  # remove contêineres e rede, preserva o volume
# Na próxima execução, docker compose up -d usa o mesmo volume
```

**CUIDADO: perde todos os dados da aula**:

```powershell
docker compose down -v
```

Ao recriar um volume vazio, o PostgreSQL executa novamente o script em `database/init`. Alterar o SQL depois de o volume já ter dados **não migra** tabelas existentes: para evoluir esquemas corporativos utilize Flyway ou Liquibase.

## 6. Erros frequentes

| Sintoma | Verificação / solução |
| --- | --- |
| `CATALOGO_DB_PASSWORD is required` | Crie `.env` a partir de `.env.example` e defina a senha |
| API não sobe | `docker compose logs catalogo-api postgres` |
| Health do Postgres falha | `docker compose ps` e logs; confira credenciais e volume |
| Porta 9091 ocupada | Encerre outro `mvn quarkus:dev` antes de subir Docker |
| `relation "produtos" does not exist` | O volume foi inicializado antes do script existir; use migração ou, **apenas sem dados úteis**, `docker compose down -v` |
| Senha mudou mas erro permanece | Credenciais do banco foram criadas na primeira inicialização do volume; não são atualizadas automaticamente ao trocar `.env` |
| `/products` dá 503 | Veja logs de `estoque-local`: é outra aplicação e não usa a tabela `produtos` |

## 7. Build local fora do Docker (alternativo)

Dev/Test (H2):

```powershell
mvn quarkus:dev
mvn test
```

Para empacotar **para produção**, o perfil de build precisa ser `prod`, com PostgreSQL. O `Dockerfile.jvm` já faz isso automaticamente. Não gere um JAR com configurações de H2 e tente mudar apenas `db-kind` por uma variável de ambiente em runtime: `db-kind` faz parte das configurações resolvidas no build.

Para Native Image, consulte o material da Aula 04 e `docker-compose.native.yml` (opcional e não necessário para esta demonstração).
