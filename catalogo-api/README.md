# Catálogo API — Java 21 / Quarkus 3.40.1 (Aulas 03–06)

Projeto didático evolutivo: Aulas 03 (REST Client), 04 (MicroProfile/observabilidade/resiliência) 05 (Hibernate ORM/Panache/PostgreSQL) e 06 (Bean Validation/ExceptionMapper). Código organizado por **funcionalidade + Boundary / Domain / Repository**, mantendo a separação entre contratos HTTP, lógica de negócio e persistência.

## Primeiro contato

**Desenvolvimento sem Docker:** instale JDK 21 e Maven 3.9+ e execute:

```powershell
mvn quarkus:dev
```

- `GET http://localhost:9091/produtos` — CRUD da Aula 05, H2 local com 3 registros de exemplo quando o schema é recriado.
- `POST http://localhost:9091/produtos/{id}/retiradas` — Aula 06: validação de entrada, erro 404/409 e persistência transacional.
- `GET http://localhost:9091/info` — MicroProfile Config.
- `GET http://localhost:9091/q/health/live` / `ready` — Health checks.
- `GET http://localhost:9091/q/metrics` — métricas.
- `GET http://localhost:9091/products` — integração da Aula 03/04 (precisa iniciar o servidor Estoque Local; sem ele, fallback 503).

**Ambiente com PostgreSQL no Docker Desktop:**

```powershell
Copy-Item .env.example .env
# Edite CATALOGO_DB_PASSWORD em .env
docker compose up --build -d
docker compose ps
```

Veja [o guia completo de produção local](docs/producao/README.md) para comandos, dados persistidos, logs e troubleshooting.

## Documentação

- [Arquitetura corporativa e estrutura dos pacotes](docs/ARQUITETURA.md)
- [Aula 03 — REST Clients](docs/aula-03/rest-clients.md)
- [Aula 04 — MicroProfile, Health, Fault Tolerance e GraalVM](docs/aula-04/README.md)
- [Aula 05 — Hibernate ORM e Panache](docs/aula-05/README.md)
- [Comparativo Panache Repository, Active Record e Hibernate](docs/aula-05/ACTIVE-RECORD-VS-REPOSITORY.md)
- [Aula 06 — Validação e tratamento de erros](docs/aula-06/README.md)
- [Referências](docs/referencias/)

## Organização das classes

```text
studio.catalogo/
├── produtos/
│   ├── boundary/       # ProdutoResource, ProdutoRequest e ProdutoResponse
│   ├── domain/         # ProdutoService, Produto
│   └── repository/     # ProdutoEntity, ProdutoRepository, Criteria API
├── catalogoremoto/
│   ├── boundary/       # GET /products, DTOs de saída HTTP
│   ├── domain/         # Retry/Fallback e resultado do catálogo
│   └── integration/    # REST Client do Estoque Local
└── platform/
    ├── health/         # Liveness/Readiness
    └── info/           # GET /info
```

Veja [regras de dependência e mapa de migração](docs/ARQUITETURA.md). Serviços de domínio não dependem dos DTOs HTTP; a conversão ocorre no boundary.

## Estrutura principal

```text
catalogo-api/
├── src/main/java/studio/catalogo/  # pacotes por funcionalidade e camadas internas
├── src/main/resources/             # dev H2 / prod PostgreSQL
├── src/test/java/studio/catalogo/  # testes HTTP
├── estoque-local/                  # servidor HTTP da Aula 03
├── database/init/01-schema.sql     # schema PostgreSQL para volume novo
├── docs/aula-04/                  # roteiro, laboratório, Postman
├── docs/aula-05/                  # roteiro, laboratório, exemplos, Postman
├── docs/aula-06/                  # validação, mappers, demo e laboratório
├── docs/producao/README.md        # Docker Desktop + banco persistente
├── docker-compose.yml             # API + estoque + PostgreSQL
├── docker-compose.native.yml      # opcional: API em Native Image
└── .env.example                    # variáveis locais (não guardar credenciais reais)
```

**Contratos HTTP preservados.** `/products` (estoque externo) e `/produtos` (CRUD local) são fontes de dados diferentes.

> Aviso: o Compose é um laboratório **prod-like em máquina local**, não uma configuração pronta para operação pública; requer TLS, autenticação, gestão de segredos, migrations e backups em ambientes reais.
