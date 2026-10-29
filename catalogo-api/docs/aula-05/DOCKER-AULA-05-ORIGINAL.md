> **Arquivo histórico:** caminhos e comandos podem corresponder à versão anterior dos pacotes ou do Docker Compose. Para executar a versão reorganizada, veja [README raiz](../../README.md), [arquitetura](../ARQUITETURA.md) e [produção local](../producao/README.md).

# Docker Desktop — Aula 05

Execute na raiz do projeto, com Docker Desktop ativo em modo Linux.

```powershell
docker compose up --build -d
docker compose ps
```

Serviços: `catalogo-api:9091` (JVM), `estoque-local:8081`, `postgres:5432` (somente rede Docker, não exposto ao host).

```powershell
# logs
docker compose logs -f catalogo-api

# consultar Postgres sem expor a porta
docker compose exec postgres psql -U catalogo -d catalogodb -c "SELECT * FROM produtos;"

# reiniciar API preservando banco
docker compose restart catalogo-api

# parar preservando volume
docker compose down

# zerar BANCO de demonstracao (destrutivo!)
docker compose down -v
```

- `GET http://localhost:9091/produtos`: começa `[]` no PostgreSQL; faça um POST para criar o primeiro produtoBo.
- `GET http://localhost:9091/products`: segue chamando `estoque-local`.
- `database/init/01-schema.sql`: roda apenas quando volume novo é criado.
- A imagem nativa opcional usa `docker compose -f docker-compose.yml -f docker-compose.native.yml up --build -d`.
- Docker Compose contém senha didática, nunca usar em produção.
