# Agendamento de Massagem

API REST para reservar salas de massagem por data e período (`MANHA`, `TARDE` ou `NOITE`). Evita reservas da mesma sala no mesmo período e impede agendamentos em feriados da UF informada, consultados por HTTP em um mock local.

**Equipe 1 — Sala 1:** Helbert Oscar de Aguiar Silva, Gabriel Moraes Kleiman, Andre Augusto de Araujo Costa, Kali Rocha Ribeiro e Khalil de Freitas.

## Documentação

- [Especificação do professor](trabalho-final-especificacao-professor.md).
- [Arquitetura técnica](ARQUITETURA.md): componentes, fluxos, persistência.
- [Checklist de requisitos e evidências](checklist.md): matriz de requisitos e evidencias.

## Tecnologias e estrutura

As versões declaradas no [pom.xml](agendamento-api/pom.xml) são: aplicação `studio:agendamento-api:1.0.0`, Java **21**, Quarkus **3.40.1**, Lombok **1.18.48**, Maven Compiler Plugin **3.15.0** e Surefire/Failsafe **3.5.6**. As versões individuais de Hibernate ORM, Panache, Jackson, H2, driver PostgreSQL, Hibernate Validator, SmallRye Health/Fault Tolerance, Micrometer/Prometheus, JUnit e REST Assured são gerenciadas pelo BOM Quarkus. O projeto não contém Maven Wrapper; use Maven 3.9.x, também adotado pelo Dockerfile JVM.

Há um módulo Maven, em `agendamento-api`. O código adota organização por funcionalidade e camadas Resource → Service → Repository:

```text
agendamento-api/src/main/java/studio/agendamento/
├── agendamentos/
│   ├── boundary/       # REST e DTOs de entrada/saída
│   ├── domain/         # serviço CDI, BO, regras, exceções e evento
│   └── repository/     # entidade JPA, PanacheRepository e enum de período
├── integracao/feriados/ # REST Client, contrato externo e resiliência
└── platform/           # erros, health, info e métricas
```

## Executar localmente

Os blocos **PowerShell e Bash** contêm comandos comuns aos dois ambientes. Quando a sintaxe difere, os exemplos são identificados separadamente. PowerShell 7 (`pwsh`) funciona no Windows, Linux e macOS. Instale os executáveis citados e disponibilize-os no `PATH`; caminhos relativos com `/` funcionam nos dois interpretadores.

Na raiz do repositório:

**PowerShell e Bash**

```bash
cd agendamento-api
java -version
mvn -version
```

Os dois comandos devem apontar para Java 21; ajuste `JAVA_HOME` e `PATH` se necessário, substituindo o caminho de exemplo pelo diretório do JDK instalado:

**PowerShell**

```powershell
# Windows; no Linux/macOS use o caminho real do JDK, como /opt/jdk-21.
$env:JAVA_HOME = 'C:/Program Files/Java/jdk-21'
$env:PATH = (Join-Path $env:JAVA_HOME 'bin') + [IO.Path]::PathSeparator + $env:PATH
```

**Bash**

```bash
export JAVA_HOME='/opt/jdk-21' # No macOS, use o diretório Home do JDK instalado.
export PATH="$JAVA_HOME/bin:$PATH"
```

Confira novamente `java -version` e `mvn -version`. Os comandos seguintes partem de `agendamento-api`.

Terminal 1 — mock HTTP (somente JDK, sem Maven):

**PowerShell e Bash**

```bash
java feriados-local/src/FeriadosLocalServer.java
```

Terminal 2 — API, profile `dev`, H2 em arquivo:

**PowerShell e Bash**

```bash
mvn quarkus:dev
```

API: `http://localhost:9091`. Mock: `http://127.0.0.1:8081`. As portas devem estar livres. O mock marca apenas 1º de janeiro de qualquer ano e datas de `FERIADOS_EXTRAS` como feriado; não é um calendário oficial. Veja [instruções do mock](agendamento-api/feriados-local/README.md).

### Compilar, testar e executar o JAR

**PowerShell e Bash**

```bash
mvn -B -ntp "-Dquarkus.hibernate-orm.sql-load-script=no-file" test
mvn -B -ntp "-Dquarkus.profile=staging" "-DskipTests" package
java "-Dquarkus.profile=staging" -jar target/quarkus-app/quarkus-run.jar
```

O empacotamento acima seleciona `staging` para usar H2, evitando as exigências de PostgreSQL do profile padrão `prod`. Pare a API anterior antes de iniciar o JAR e mantenha o mock ativo para POST/PUT. Não abra duas instâncias sobre o mesmo arquivo H2.

Os testes usam H2 em memória, porta HTTP aleatória e mock próprio, sem necessidade de iniciar o mock manualmente. `StagingReadinessTest` também sobrescreve o datasource para memória. Relatórios são gerados em `agendamento-api/target/surefire-reports/`; a situação da execução desta revisão está no [checklist](checklist.md#validacao-desta-revisao).

Sem seleção explícita de profile, o build/JAR usa `prod`: requer PostgreSQL e `DB_PASSWORD`, além de `DB_URL` e `DB_USERNAME` adequados ao ambiente. O [Compose JVM](agendamento-api/docker-compose.yml) fornece banco PostgreSQL 17 Alpine, mock e API; o build é feito no [Dockerfile JVM](agendamento-api/src/main/docker/Dockerfile.jvm). Para demonstração opcional, substitua o valor local de senha:

**PowerShell**

```powershell
$env:CATALOGO_DB_PASSWORD = 'substitua-por-uma-senha-local'
```

**Bash**

```bash
export CATALOGO_DB_PASSWORD='substitua-por-uma-senha-local'
```

**PowerShell e Bash**

```bash
docker compose up --build -d
# Ao terminar, preservando o volume:
docker compose down
```

Depois de encerrar, remova a variável da sessão:

**PowerShell**

```powershell
Remove-Item Env:CATALOGO_DB_PASSWORD -ErrorAction SilentlyContinue
```

**Bash**

```bash
unset CATALOGO_DB_PASSWORD
```

## Contrato HTTP e regras

| Campo de entrada | Regra implementada |
|---|---|
| `id_sala` | Obrigatório, positivo; identificador global da sala |
| `id_usuario` | Opcional; positivo quando informado |
| `uf` | Obrigatória; sigla brasileira válida em maiúsculas, incluindo DF |
| `data` | Obrigatória, ISO `yyyy-MM-dd`, hoje ou futura |
| `periodo` | Obrigatório: `MANHA`, `TARDE` ou `NOITE` |

A combinação sala/data/período é única, independentemente da UF. PUT substitui os campos da reserva e ignora o próprio ID na busca de conflito. POST/PUT consultam feriados após a verificação de conflito. Feriado ou conflito gera 409; falha na consulta externa gera 503 após até três tentativas, sem gravar a alteração.

A resposta contém `id` e `salaDisponivel: false`, pois descreve uma reserva existente. Esse campo não é uma consulta de disponibilidade. Não há cadastro de salas/usuários, autenticação ou frontend.

| Método e caminho | Resposta de sucesso / comportamento |
|---|---|
| GET `/agendamentos` | 200; coleção ordenada por ID, sem paginação |
| GET `/agendamentos/{id}` | 200; 404 se ausente |
| POST `/agendamentos` | 201; JSON e cabeçalho `Location` |
| PUT `/agendamentos/{id}` | 200; JSON atualizado; 404 se ausente |
| DELETE `/agendamentos/{id}` | 204 sem corpo; 404 se ausente |
| GET `/info` | 200; `{"nome":"..."}` com valor configurado |
| GET `/q/health/live` | 200/UP quando o check executa |
| GET `/q/health/ready` | 200/UP ou 503/DOWN, conforme habilitação e saúde do datasource |
| GET `/q/metrics` | Exposição Prometheus, incluindo `agendamentos_criados_total` |

Essas rotas pertencem à API. O mock oferece `GET /feriados?uf=SP&data=...` em outra porta.

### Exemplos de requisições e respostas

Exemplos com data calculada para o próximo ano e sala escolhida para a demonstração (troque-a se já estiver reservada):

**PowerShell**

```powershell
$ano = (Get-Date).Year + 1
$dados = @{ id_sala=101; id_usuario=10; uf='SP'; data="$ano-02-10"; periodo='MANHA' }
$resposta = Invoke-WebRequest -Uri 'http://localhost:9091/agendamentos' -Method Post -ContentType 'application/json' -Body ($dados | ConvertTo-Json) -UseBasicParsing
$resposta.StatusCode
$resposta.Headers
$resposta.Content
```

**Bash** (requer `curl`)

```bash
ano=$(($(date +%Y) + 1))
curl -i http://localhost:9091/agendamentos \
  -H 'Content-Type: application/json' \
  -d "{\"id_sala\":101,\"id_usuario\":10,\"uf\":\"SP\",\"data\":\"$ano-02-10\",\"periodo\":\"MANHA\"}"
```

Resposta **ilustrativa**, não captura de execução; ID e ano variam:

```http
HTTP/1.1 201 Created
Location: http://localhost:9091/agendamentos/1
Content-Type: application/json

{"id":1,"id_sala":101,"salaDisponivel":false,"id_usuario":10,"uf":"SP","data":"2027-02-10","periodo":"MANHA"}
```

Use o ID retornado para consultar, atualizar com o mesmo contrato de entrada e excluir (substitua `1` nos exemplos). Continue no mesmo terminal para reutilizar o ano e, no PowerShell, `$dados`:

**PowerShell**

```powershell
Invoke-WebRequest -Uri 'http://localhost:9091/agendamentos/1' -UseBasicParsing
$dados.periodo = 'TARDE'
Invoke-WebRequest -Uri 'http://localhost:9091/agendamentos/1' -Method Put -ContentType 'application/json' -Body ($dados | ConvertTo-Json) -UseBasicParsing
Invoke-WebRequest -Uri 'http://localhost:9091/agendamentos/1' -UseBasicParsing
Invoke-WebRequest -Uri 'http://localhost:9091/agendamentos/1' -Method Delete -UseBasicParsing
try {
    Invoke-WebRequest -Uri 'http://localhost:9091/agendamentos/1' -UseBasicParsing
} catch {
    if ($null -eq $_.Exception.Response) { throw }
    [int]$_.Exception.Response.StatusCode
    $_.ErrorDetails.Message
}
```

O PowerShell lança exceção para respostas HTTP de erro; a última consulta captura o 404 e exibe seu corpo. Esses exemplos evitam o alias `curl` do Windows PowerShell. No Bash, `curl -i` exibe status, cabeçalhos e corpo, inclusive nos erros HTTP.

**Bash**

```bash
curl -i http://localhost:9091/agendamentos/1
curl -i -X PUT http://localhost:9091/agendamentos/1 \
  -H 'Content-Type: application/json' \
  -d "{\"id_sala\":101,\"id_usuario\":10,\"uf\":\"SP\",\"data\":\"$ano-02-10\",\"periodo\":\"TARDE\"}"
curl -i http://localhost:9091/agendamentos/1
curl -i -X DELETE http://localhost:9091/agendamentos/1
curl -i http://localhost:9091/agendamentos/1
```

A última consulta retorna 404. Corpo ilustrativo, com `path` relativo obtido de `UriInfo.getPath()`:

```json
{
  "codigo": "AGENDAMENTO_NAO_ENCONTRADO",
  "mensagem": "Agendamento 1 não encontrado.",
  "timestamp": "2026-11-08T12:00:00Z",
  "path": "agendamentos/1",
  "detalhes": []
}
```

Para 400, envie `{}` no POST. A resposta usa `codigo: DADOS_INVALIDOS` e a mesma estrutura, com detalhes de validação quando disponíveis. O contrato e os mappers estão detalhados na [arquitetura](ARQUITETURA.md#validacao-e-erros).

### Postman e script automatizado

Importe a [coleção Postman](agendamento-api/postman/projeto-final.postman_collection.json) e execute a pasta **Fluxo principal — mock UP** na ordem. `baseUrl` aponta para a API; a coleção calcula datas do próximo ano e uma sala aleatória. Os testes verificam CRUD, `Location`, PUT/GET, erros e métrica antes/depois. A presença desses scripts não equivale a um relatório de execução.

Alternativamente, com API e mock ativos, use o [script PowerShell](agendamento-api/scripts/validar-http.ps1) ou o [script Bash](agendamento-api/scripts/validar-http.sh), a partir de `agendamento-api`. Ambos verificam CRUD, erros, `Location`, persistência de PUT e contador antes/depois, criam uma reserva temporária e tentam removê-la ao terminar. Execute sem criações concorrentes para a comparação exata do contador. O Bash requer **Bash 3.2+, curl e Python 3** (`python3` no `PATH`); o PowerShell usa .NET, sem curl ou Python.

**PowerShell**

```powershell
./scripts/validar-http.ps1
# URL alternativa:
./scripts/validar-http.ps1 -BaseUrl 'http://localhost:9091'
```

**Bash**

```bash
bash scripts/validar-http.sh
# URL alternativa:
bash scripts/validar-http.sh --base-url 'http://localhost:9091'
```

A invocação com `bash` dispensa permissão de execução. Para execução direta em Linux/macOS, no **Bash**:

```bash
chmod +x scripts/validar-http.sh
./scripts/validar-http.sh
```

Para executar o `.ps1` em um novo processo com **PowerShell 7 instalado**, use este comando comum a **PowerShell e Bash** (Windows/Linux/macOS):

```bash
pwsh -File scripts/validar-http.ps1
```

No Windows, a política de execução pode impedir o `.ps1`: consulte `Get-ExecutionPolicy -List` no **PowerShell** e siga a política da organização. Se o arquivo revisado estiver bloqueado por ter sido baixado, use `Unblock-File ./scripts/validar-http.ps1` no **PowerShell do Windows**; isso não altera a política. Em Linux/macOS, `pwsh -File` dispensa `chmod` para o `.ps1`. Para chamar o `.sh` pelo PowerShell, use `bash scripts/validar-http.sh` com Bash e dependências instalados. No Windows, execute o roteiro Bash em Git Bash ou WSL, com suas dependências nesse ambiente; no WSL, confira a acessibilidade da API e ajuste `--base-url` se necessário.

Para demonstrar falha externa, pare apenas o mock e execute:

**PowerShell**

```powershell
./scripts/validar-http.ps1 -FalhaExterna
```

**Bash**

```bash
bash scripts/validar-http.sh --falha-externa
```

Espera-se 503, ausência de gravação e contador inalterado. Reinicie o mock depois. No Postman, habilite `testarFalha=true` e execute apenas o cenário opcional correspondente, após o fluxo principal.

<a id="configuracoes-e-profiles"></a>
## Configurações e profiles

Fontes: [application.properties](agendamento-api/src/main/resources/application.properties) e [application-staging.properties](agendamento-api/src/main/resources/application-staging.properties).

| Profile | Banco / schema | Nome padrão em `/info` |
|---|---|---|
| `dev` | H2 em arquivo; `update` | Agendamento de Massagem |
| `staging` | H2 em arquivo; `update` | Agendamento de Massagem - STAGING |
| `prod` | PostgreSQL; `update` | Agendamento de Massagem - PROD |
| `test` | H2 em memória; `drop-and-create` | Agendamento de Massagem |

| Variável | Padrão / finalidade |
|---|---|
| `AGENDAMENTO_NOME` | Sobrescreve o nome do profile |
| `AGENDAMENTO_PRONTO` | `true`; `false` torna o check de readiness DOWN |
| `FERIADOS_URL` | `http://127.0.0.1:8081` |
| `AGENDAMENTO_DB_URL` | `jdbc:h2:file:./data/agendamentos`, para dev/staging |
| `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` | Em prod: `jdbc:postgresql://postgres:5432/catalogodb` / `catalogo` / senha sem padrão |
| `QUARKUS_HTTP_PORT` | Sobrescreve a porta 9091 |
| `QUARKUS_PROFILE` | Seleciona profile; dev no modo desenvolvimento, prod no JAR sem seleção explícita |
| `FERIADOS_HOST` / `FERIADOS_PORT` | Variáveis do mock: `127.0.0.1` / `8081` |
| `FERIADOS_EXTRAS` | Variável do mock; lista como `SP:2030-04-10,RJ:2030-05-20` |

Para demonstrar dois profiles, consulte `/info` com `mvn quarkus:dev`, pare essa instância e inicie `staging`. Primeiro confira o nome padrão com `mvn quarkus:dev "-Dquarkus.profile=staging"`; depois reinicie com sobrescritas por ambiente:

**Bash**

```bash
AGENDAMENTO_NOME='Massagem - demonstracao' AGENDAMENTO_PRONTO=false \
  mvn quarkus:dev "-Dquarkus.profile=staging"
```

**PowerShell**

```powershell
$env:AGENDAMENTO_NOME='Massagem - demonstracao'
$env:AGENDAMENTO_PRONTO='false'
mvn quarkus:dev "-Dquarkus.profile=staging"
# Depois de encerrar:
Remove-Item Env:AGENDAMENTO_NOME, Env:AGENDAMENTO_PRONTO -ErrorAction SilentlyContinue
```

Consulte `/info`, `/q/health/live` e `/q/health/ready`: nome sobrescrito, live 200/UP e ready 503/DOWN são os resultados esperados. No Postman, habilite `testarReadinessDown=true` para o cenário opcional. O check de liveness sempre retorna UP quando executa; processo parado não responde, em vez de produzir um JSON DOWN. A integração de feriados não participa da readiness.

## Cobertura de requisitos

Os 16 requisitos possuem implementação identificada e estão individualmente rastreados no [checklist](checklist.md). A comprovação integral depende dos resultados de testes e demonstrações ali registrados, especialmente dois profiles com sobrescrita por ambiente.
