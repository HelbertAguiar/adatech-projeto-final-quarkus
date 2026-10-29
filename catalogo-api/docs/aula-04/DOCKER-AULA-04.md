> **Arquivo histórico:** caminhos e comandos podem corresponder à versão anterior dos pacotes ou do Docker Compose. Para executar a versão reorganizada, veja [README raiz](../../README.md), [arquitetura](../ARQUITETURA.md) e [produção local](../producao/README.md).

# Aula 04 — Docker Compose (Estoque Local + Catálogo API)

A partir da pasta `catalogo-api/` (onde está o `docker-compose.yml`), basta **um comando**. O Docker Compose constrói as duas imagens a partir dos códigos-fonte, cria a rede privada e inicia os containers na ordem correta.

## 1. Build + run (Quarkus JVM, recomendado para a aula)

Pré-requisito: Docker Desktop com engine Linux e `docker compose` disponíveis. **Não é necessário instalar Maven/JDK na máquina para este fluxo.**

```powershell
docker compose up --build -d
```

O `--build` força a construção das imagens antes de iniciar. `-d` executa em segundo plano. O catálogo é compilado numa imagem Maven/JDK 21 e executado numa imagem JRE 21. O estoque é compilado com JDK 21 e executado com JRE 21.

## 2. Endpoints para testar no Windows / Postman

| Endpoint | Serviço | Esperado |
| --- | --- | --- |
| `http://localhost:8081/products` | Estoque Local | JSON do estoque |
| `http://localhost:9091/products` | Catálogo → Estoque | HTTP 200, `origem: "ESTOQUE"` |
| `http://localhost:9091/products-native` | Catálogo → Estoque | HTTP 200 (funciona também no modo JVM) |
| `http://localhost:9091/info` | Catálogo | Nome / informações |
| `http://localhost:9091/q/health/live` | Catálogo | Liveness |
| `http://localhost:9091/q/health/ready` | Catálogo | Readiness |
| `http://localhost:9091/q/metrics` | Catálogo | Métricas Micrometer / Prometheus |

Teste via PowerShell (usar `curl.exe`, e não o alias `curl`):

```powershell
curl.exe -i http://localhost:8081/products
curl.exe -i http://localhost:9091/products
curl.exe -i http://localhost:9091/q/health/ready
```

**Importante:** `http://localhost:8081` funciona **no host** devido ao mapeamento de portas. Dentro do container Quarkus, o endereço configurado é `http://estoque-local:8081`, usando o DNS da rede do Compose. Não trocar por `localhost` dentro da API.

As portas 8081 e 9091 são publicadas apenas em `127.0.0.1` do computador (ambiente didático). Se houver outro processo usando alguma destas portas, pare-o ou altere o mapeamento host:container no Compose.

## 3. Logs, status, interrupção e limpeza

```powershell
# Lista containers do projeto e seus estados
docker compose ps

# Logs contínuos (Ctrl+C sai dos logs, sem parar os containers)
docker compose logs -f

# Logs de um serviço
docker compose logs -f catalogo-api

# Parar ambos e remover a rede criada pelo Compose
docker compose down
```

`docker compose down` **não apaga as imagens** nem modifica os códigos-fonte. Para forçar rebuild após alterações:

```powershell
docker compose up --build -d
```

## 4. Demonstrar Fault Tolerance: retry e fallback

```powershell
# Para o estoque, mas mantém o catálogo no ar
docker compose stop estoque-local

# Retorna HTTP 503 e origem FALLBACK após tentativas configuradas
curl.exe -i http://localhost:9091/products-native

# Recupera o estoque
docker compose start estoque-local

# Deve voltar a HTTP 200 e origem ESTOQUE
curl.exe -i http://localhost:9091/products-native
```

Ao iniciar os dois serviços com `up`, o Compose aguarda a verificação de saúde do estoque (`GET /products` via `EstoqueHealthCheck`) antes de iniciar o catálogo. Isso **não** torna chamadas entre serviços infalíveis; a aplicação já possui timeouts, `@Retry` e `@Fallback` para lidar com falhas durante o funcionamento.

## 5. Opcional — construir e executar o catálogo em Native Image

Preserva a demonstração GraalVM da Aula 04, sem compilar o binário manualmente no Windows:

```powershell
# Encerre o projeto se estiver em modo JVM
docker compose down

# Compila o catálogo nativamente e inicia ambos os serviços
docker compose -f docker-compose.yml -f docker-compose.native.yml up --build -d
```

A versão nativa é construída a partir do **código-fonte** com Mandrel no estágio de build e executada numa imagem Linux UBI9. É um build mais demorado e que exige mais RAM/CPU do Docker Desktop do que o modo JVM (sugestão: pelo menos 6–8 GB de memória disponíveis para o Docker no build, conforme a máquina). **Não** requer o binário `target/*-runner` gerado antes pelo host. O estoque continua usando a JVM.

Para logs e encerramento da variante nativa:

```powershell
docker compose -f docker-compose.yml -f docker-compose.native.yml ps
docker compose -f docker-compose.yml -f docker-compose.native.yml logs -f catalogo-api
docker compose -f docker-compose.yml -f docker-compose.native.yml down
```

## 6. Arquivos adicionados / alterados

- `docker-compose.yml`: serviços, portas, configurações, rede e ordem de inicialização.
- `docker-compose.native.yml`: troca o builder/runtime JVM por Native Image só para o catálogo.
- `src/main/docker/Dockerfile.jvm`: multi-stage Maven → JRE.
- `src/main/docker/Dockerfile.multistage-native`: multi-stage Maven/Mandrel → UBI9 runtime.
- `estoque-local/src/EstoqueHealthCheck.java`: health check sem `curl`/`wget` instalado.
- `estoque-local/Dockerfile`: compila também a classe de health check.
- `.dockerignore` e `estoque-local/.dockerignore`: reduzem o contexto dos builds.

O código dos recursos `GET /products`, `GET /info`, health/metrics e fault tolerance foi preservado. O `src/main/docker/Dockerfile.native` original permanece disponível para o fluxo antigo que empacota um runner já compilado.

> Observação: se ainda existirem os containers manuais `estoque-local`/`catalogo-native` da aula, remova-os ou pare-os antes para libertar as portas: `docker rm -f estoque-local catalogo-native` (use apenas se não precisar daqueles containers).
