# Agendamento de Massagem — projeto final

API REST para reservar uma sala por data e período inteiro (`MANHA`, `TARDE` ou `NOITE`). Impede duas reservas da mesma sala no mesmo período e bloqueia agendamentos em feriados da UF informada.

**Equipe:** a informar. **Integrantes:** a informar.
**Tecnologias:** Java 21, Quarkus 3.40.1, Maven 3.9+, H2, Panache, Bean Validation, Lombok, MicroProfile Health/Fault Tolerance e Micrometer.

Enunciado: [trabalho-final.md](trabalho-final.md). Evidências e situação das demonstrações: [docs/evidencias.md](docs/evidencias.md).

## Iniciar localmente

Use dois terminais PowerShell na raiz de `catalogo-api`, com JDK 21. Confirme `java -version` e `mvn -version`: ambos devem indicar Java 21. Se necessário, ajuste `JAVA_HOME` para a instalação do JDK 21 antes de executar Maven.

Terminal 1 — mock HTTP, sem dependências adicionais:

```powershell
java feriados-local/src/FeriadosLocalServer.java
```

Terminal 2 — API:

```powershell
mvn quarkus:dev
```

A API responde em http://localhost:9091; o mock, em http://127.0.0.1:8081.
O mock considera feriado apenas 1º de janeiro de qualquer ano e datas configuradas em `FERIADOS_EXTRAS`. Consulte [feriados-local/README.md](feriados-local/README.md). Encerre qualquer estoque-local que já esteja usando a porta 8081, ou configure outra porta para o mock e a URL correspondente na API.

## Contrato e regras

| Campo de entrada | Regra |
|---|---|
| `id_sala` | Obrigatório e positivo; identificador global |
| `id_usuario` | Opcional; quando informado, deve ser positivo |
| `uf` | Sigla válida em maiúsculas, incluindo DF |
| `data` | Data ISO `yyyy-MM-dd`, hoje ou futura |
| `periodo` | Obrigatório: `MANHA`, `TARDE` ou `NOITE` |

Uma sala só pode ter uma reserva por data/período, independentemente da UF. PUT ignora a própria reserva ao verificar conflitos. A consulta de feriados ocorre no POST e no PUT. Falha externa faz três tentativas ao todo e retorna 503 sem gravar a alteração. A constraint única do banco também protege reservas simultâneas.

A resposta inclui `id` e `salaDisponivel: false`, pois representa uma reserva que ocupa o período inteiro. Não há cadastro de salas ou usuários, frontend ou autenticação.

| Método e rota | Resultado |
|---|---|
| GET `/agendamentos` | 200, lista ordenada por ID |
| GET `/agendamentos/{id}` | 200 ou 404 |
| POST `/agendamentos` | 201 e cabeçalho Location |
| PUT `/agendamentos/{id}` | 200 com reserva atualizada |
| DELETE `/agendamentos/{id}` | 204 ou 404 |
| GET `/info` | 200 com nome configurado |
| GET `/q/health/live` | 200/UP enquanto a aplicação responde |
| GET `/q/health/ready` | UP ou 503/DOWN conforme configuração e banco |
| GET `/q/metrics` | Métricas Prometheus |

Entradas inválidas retornam 400; ausência retorna 404; sala ocupada ou feriado retorna 409; indisponibilidade de feriados retorna 503. Os erros seguem o mesmo contrato:

```json
{
  "codigo": "AGENDAMENTO_NAO_ENCONTRADO",
  "mensagem": "Agendamento 999999 não encontrado.",
  "timestamp": "2026-10-31T12:00:00Z",
  "path": "/agendamentos/999999",
  "detalhes": []
}
```

## Executar requisições

Com API e mock iniciados, o roteiro verifica CRUD, Location, PUT/GET, 400/404/409, feriado e variação do contador. Usa sala aleatória e exclui apenas a reserva que criou:

```powershell
./scripts/validar-http.ps1
```

Para experimentar manualmente:

```powershell
$ano = (Get-Date).Year + 1
$body = @{id_sala=101; id_usuario=10; uf='SP'; data="$ano-02-10"; periodo='MANHA'} | ConvertTo-Json
$reserva = Invoke-RestMethod http://localhost:9091/agendamentos -Method Post -ContentType 'application/json' -Body $body
Invoke-RestMethod "http://localhost:9091/agendamentos/$($reserva.id)"
$body = @{id_sala=101; id_usuario=10; uf='SP'; data="$ano-02-10"; periodo='TARDE'} | ConvertTo-Json
Invoke-RestMethod "http://localhost:9091/agendamentos/$($reserva.id)" -Method Put -ContentType 'application/json' -Body $body
Invoke-RestMethod "http://localhost:9091/agendamentos/$($reserva.id)" -Method Delete
```

Importe [postman/projeto-final.postman_collection.json](postman/projeto-final.postman_collection.json) no Postman e execute a pasta **Fluxo principal — mock UP** na ordem. Datas são calculadas para o próximo ano. Ajuste `baseUrl` se alterar a porta.

Para demonstrar resiliência, pare somente o mock com Ctrl+C e execute:

```powershell
./scripts/validar-http.ps1 -FalhaExterna
```

O roteiro espera 503, nenhuma reserva gravada e contador inalterado. Reinicie o mock para voltar às criações normais. Na coleção, habilite `testarFalha=true` e execute apenas o cenário opcional correspondente.

## Banco e profiles

O banco é H2 em arquivo, por padrão `data/agendamentos.mv.db`. Dev, staging e prod usam `update` e não recriam o schema. Não execute duas instâncias sobre o mesmo arquivo. Os testes padrão usam somente H2 em memória com schema descartável; a configuração de teste staging também aponta explicitamente para memória.

| Variável | Padrão / finalidade |
|---|---|
| `AGENDAMENTO_NOME` | Agendamento de Massagem; sobrescreve o nome no /info |
| `AGENDAMENTO_PRONTO` | true; false faz readiness retornar DOWN |
| `FERIADOS_URL` | http://127.0.0.1:8081 |
| `AGENDAMENTO_DB_URL` | jdbc:h2:file:./data/agendamentos |
| `QUARKUS_HTTP_PORT` | 9091 |
| `QUARKUS_PROFILE` | dev no modo desenvolvimento; prod no JAR |
| `FERIADOS_HOST` / `FERIADOS_PORT` | 127.0.0.1 / 8081; variáveis do processo do mock |
| `FERIADOS_EXTRAS` | Lista no formato SP:2030-04-10,RJ:2030-05-20; usada pelo mock |

Dev: `mvn quarkus:dev`. Para staging, pare a instância anterior e execute:

```powershell
$env:QUARKUS_PROFILE='staging'
mvn quarkus:dev
```

Sem sobrescrita, `GET /info` retorna “Agendamento de Massagem - STAGING”. Para demonstrar configuração externa, reinicie com:

```powershell
$env:AGENDAMENTO_NOME='Massagem - demonstracao'
$env:AGENDAMENTO_PRONTO='false'
mvn quarkus:dev
```

`/info` deve refletir o nome; liveness continua 200/UP e readiness retorna 503/DOWN. A liveness verifica que a aplicação está viva; ausência do processo ou bloqueio impede resposta. Readiness verifica a habilitação controlável e o datasource; falha do banco também pode torná-la DOWN. O mock não participa da readiness: sua falha aparece como 503 nas operações que o consultam.

Depois da demonstração, limpe as variáveis:

```powershell
Remove-Item Env:QUARKUS_PROFILE, Env:AGENDAMENTO_NOME, Env:AGENDAMENTO_PRONTO -ErrorAction SilentlyContinue
```

## Compilar e testar

```powershell
mvn -B -ntp package
java -jar target/quarkus-app/quarkus-run.jar
```

Os testes iniciam mocks HTTP locais automaticamente. Os relatórios ficam em `target/surefire-reports`. O JAR usa H2 também no profile prod e precisa do mock para POST/PUT.

## Arquitetura e evidências dos requisitos 01–16

```text
studio.agendamento/
├── agendamentos/
│   ├── boundary/       # Resource, RequestDto, ResponseDto
│   ├── domain/         # Service CDI, BO, regras e eventos
│   └── repository/     # Entity JPA, PanacheRepository, enum
├── integracao/feriados/ # REST Client, contrato externo, Retry/Fallback
└── platform/           # Erros, health, /info e métrica
```

O Resource passa campos ao Service e converte o BO por `ResponseDto.from`. As escritas são transacionais no Service. A métrica `agendamentos_criados_total` aumenta somente após commit de criação. Lombok `@RequiredArgsConstructor` gera os construtores dos campos finais usados na injeção CDI, sem `@Data` em entidades.

| Requisito | Onde encontrar a evidência |
|---|---|
| 01 — Métodos HTTP | AgendamentoResource; CRUD no Postman e validar-http.ps1 |
| 02 — Status e Location | CRUD e testes de erro em AgendamentoResourceTest |
| 03 — Lombok | Resource e Service com @RequiredArgsConstructor |
| 04 — Quarkus/JAX-RS | AgendamentoResource; respostas JSON nos testes |
| 05 — CDI e negócio | AgendamentoService; conflitos e feriados |
| 06 — REST Client | FeriadosClient, FeriadosService e mock local |
| 07 — Configuração e profiles | application.properties, application-staging.properties, /info |
| 08 — Health | platform/health; StagingReadinessTest |
| 09 — Resiliência | FeriadosService; teste de três tentativas e 503 |
| 10 — Métrica | AgendamentoMetrics; contador antes/depois |
| 11 — JPA | AgendamentoEntity; tabela criada nos testes H2 |
| 12 — Panache | AgendamentoRepository; CRUD HTTP |
| 13 — Transações | AgendamentoService; PUT/GET e concorrência |
| 14 — Bean Validation | AgendamentoRequestDto e @Valid no Resource |
| 15 — ExceptionMapper | platform/error/mapper; testes 404/409/503 |
| 16 — Erro padronizado | ErroResponse; testes distintos de 400 e 404 |

Consulte [docs/evidencias.md](docs/evidencias.md) para separar implementação, testes executados e demonstrações pendentes.

## Material anterior preservado

Os pacotes `studio.catalogo`, seus testes e as documentações das aulas continuam no repositório. As rotas didáticas `/produtos`, `/products` e `/products-native` permanecem disponíveis; as informações antigas foram movidas para `/catalogo/info` para liberar `/info` ao agendamento.

A [documentação histórica do catálogo](docs/README-catalogo-aulas.md), os arquivos Compose/PostgreSQL e os guias das aulas descrevem a configuração anterior. O caminho de execução deste projeto final é o H2 com mock de feriados descrito acima; os Compose antigos não foram adaptados nem validados para o agendamento. Nenhum banco existente deve ser removido para iniciar ou testar a API.
