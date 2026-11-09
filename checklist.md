# Checklist de requisitos e evidências

[README](README.md) · [Arquitetura técnica](ARQUITETURA.md) · [Especificação do professor](trabalho-final-especificacao-professor.md)

A matriz preserva os 16 nomes e critérios de aceitação da seção 5 da especificação.

## HTTP, REST e Lombok (R01–R03)

| ID | Requisito | Implementação | Evidência |
|---|---|---|---|
| R01 | **Recursos e métodos HTTP**<br>**Requisito:** Criar recursos com URIs claras e utilizar GET, POST, PUT e DELETE conforme o significado da operação.<br>**Critério de aceitação:** Demonstrar ao menos um recurso, com GET de coleção, GET por ID, POST, PUT e DELETE, sem verbos no caminho (ex.: `/tarefas`, `/tarefas/{id}`). | [Resource][resource]: GET coleção/ID, POST, PUT e DELETE em `/agendamentos` e `/{id}`. | **Esperado pela especificacão:** Rotas e requisições Postman executadas.<br>**Encontrada:** [Teste HTTP][http], `crudCompletoComLocationEAtualizacaoPersistida`; [coleção Postman][postman] e scripts [PowerShell][script] e [Bash][script-bash]. |
| R02 | **Códigos de status e contratos**<br>**Requisito:** Retornar códigos de status HTTP coerentes com os cenários da API.<br>**Critério de aceitação:** Demonstrar 200 (consulta), 201 (criação), 204 (exclusão), 400 (requisição inválida) e 404 (recurso ausente); informar `Location` ao criar, quando aplicável. | [Resource][resource] retorna 200/201/204 e `Location`; [mappers][mappers] cobrem 400/404. | **Esperado pela especificacão:** Postman com respostas e status verificáveis.<br>**Encontrada:** `crudCompletoComLocationEAtualizacaoPersistida`, `validaCamposObrigatoriosEFormatoDeErro400`, `padroniza404EmConsultaAtualizacaoEExclusao` em [Teste HTTP][http]. |
| R03 | **Lombok aplicado conscientemente**<br>**Requisito:** Usar Lombok para evitar código repetitivo em pelo menos uma classe apropriada.<br>**Critério de aceitação:** Aplicar, por exemplo, `@Getter`, `@Setter` ou `@Builder`, explicar o código gerado e evitar `@Data` indiscriminado em entidade JPA. | `@RequiredArgsConstructor` em [Resource][resource] e [Service][service] gera construtores dos campos finais para CDI; entidade sem `@Data`. | **Esperado pela especificacão:** Classe anotada e uso compilável.<br>**Encontrada:** compilação Java 21 concluída; [POM][pom] declara Lombok e annotation processor. Explicação na [arquitetura](ARQUITETURA.md#cdi-e-lombok). |

## Quarkus REST, CDI e REST Client (R04–R06)

| ID | Requisito | Implementação | Evidência |
|---|---|---|---|
| R04 | **API Quarkus/JAX-RS**<br>**Requisito:** Implementar endpoints com Quarkus REST e anotações Jakarta REST.<br>**Critério de aceitação:** Demonstrar `@Path`, `@GET` ou `@POST`, `@Produces` / `@Consumes` quando necessários e retorno JSON correto. | [Resource][resource] usa Jakarta REST, `@Path`, verbos HTTP, `@Produces` e `@Consumes` JSON. | **Esperado pela especificacão:** Classe `*Resource` e resposta real.<br>**Encontrada:** [Teste HTTP][http] verifica campos JSON no CRUD e content type dos erros. |
| R05 | **CDI e regra de negócio**<br>**Requisito:** Injetar um serviço via CDI e implementar uma regra de negócio no serviço.<br>**Critério de aceitação:** O Resource delega a um serviço CDI injetado (por exemplo, com `@ApplicationScoped`); o serviço executa uma regra de negócio que vai além de apenas repassar a chamada ao repositório. | [Service][service] `@ApplicationScoped`, injetado por construtor; impede conflito de sala e feriado. | **Esperado pela especificacão:** Resource + service + cenário da regra.<br>**Encontrada:** `bloqueiaConflitosMasPermiteOutrasSalasEPeriodos`, `putIgnoraProprioRegistroEBloqueiaReservaDeOutro`, `feriadoBloqueiaPostEPutSemModificarDados` em [Teste HTTP][http]. |
| R06 | **Consumo de API externa**<br>**Requisito:** Consumir outro serviço HTTP com Quarkus REST Client.<br>**Critério de aceitação:** Usar interface cliente e configuração de URL; demonstrar resposta do serviço externo ou mock local controlado. | [Client][client] `@RegisterRestClient`, URL configurada; [FeriadosService][feriados] consome contrato externo via HTTP. | **Esperado pela especificacão:** Client + endpoint integrado + resposta.<br>**Encontrada:** [mock de testes][mocktest] em porta aleatória e cenários de feriado/indisponibilidade em [Teste HTTP][http]. [Mock local][mocklocal] disponível para demonstração local. |

## MicroProfile, observabilidade e resiliência (R07–R10)

| ID | Requisito | Implementação | Evidência |
|---|---|---|---|
| R07 | **Configurações e profiles**<br>**Requisito:** Externalizar ao menos uma configuração real e demonstrar dois profiles.<br>**Critério de aceitação:** Usar MicroProfile Config ou `@ConfigProperty`, `dev` e `prod` / `staging`, com ao menos uma variável de ambiente sobrescrevendo valor. | [Properties][config] e [staging][staging] externalizam nome, readiness, banco e URL; `/info` usa `@ConfigProperty`. | **Esperado pela especificacão:** Arquivo(s) properties + execução em dois profiles.<br>**Encontrada:** expressões de ambiente e profiles. `saudeEConfiguracaoRespondem` no profile test e [StagingReadinessTest][healthtest] em staging. |
| R08 | **Liveness e readiness**<br>**Requisito:** Disponibilizar duas verificações de saúde com propósitos distintos.<br>**Critério de aceitação:** `/q/health/live` e `/q/health/ready` respondem e a equipe explica quando cada uma seria DOWN; readiness tem condição controlável. | [Liveness][live] retorna sempre UP quando executa; [readiness][ready] depende de `agendamento.pronto`; datasource contribui com sua saúde. | **Esperado pela especificacão:** Health checks + requisições de teste.<br>**Encontrada:** `saudeEConfiguracaoRespondem` e `stagingPodeFicarNaoProntoSemFalharLiveness` em [Teste HTTP][http] e [StagingReadinessTest][healthtest] verificam live UP, ready UP e DOWN. |
| R09 | **Tolerância a falhas**<br>**Requisito:** Tratar falha de integração externa com mecanismo de resiliência.<br>**Critério de aceitação:** Implementar `@Retry` e `@Fallback` (ou equivalente MicroProfile Fault Tolerance), simular indisponibilidade e demonstrar resposta controlada. | [FeriadosService][feriados]: `@Retry(maxRetries=2, delay=100, jitter=0)` e fallback que lança exceção mapeada para 503. | **Esperado pela especificacão:** Cenários externo UP/DOWN e classe responsável.<br>**Encontrada:** `falhaExternaExecutaTresTentativasESemGravacao` conta 3 chamadas; `falhaExternaNoPutPreservaReserva` e `respostaExternaIncompletaTambemImpedeGravacao`, em [Teste HTTP][http]. |
| R10 | **Métrica de negócio**<br>**Requisito:** Registrar uma métrica de negócio com Micrometer.<br>**Critério de aceitação:** Implementar pelo menos um contador ou timer e demonstrar seu valor em `/q/metrics`, variando após requisições. | [AgendamentoMetrics][metrics] registra contador após commit e o expõe como `agendamentos_criados_total`. | **Esperado pela especificacão:** Métrica Prometheus antes/depois.<br>**Encontrada:** `contadorSomenteAumentaAposCriacaoConfirmada` compara o `MeterRegistry` antes/depois e verifica presença do nome em `/q/metrics`. [PowerShell][script], [Bash][script-bash] e [Postman][postman] comparam valores Prometheus. |

## Hibernate ORM e Panache (R11–R13)

| ID | Requisito | Implementação | Evidência |
|---|---|---|---|
| R11 | **Entidade JPA**<br>**Requisito:** Mapear uma entidade persistente com Hibernate ORM.<br>**Critério de aceitação:** Ter `@Entity`, identificador e campos adequados ao domínio; não usar diretamente o DTO HTTP como entidade. | [Entity][entity] separada de DTO/BO, `@Entity`, ID identity e cinco campos do domínio; tabela `agendamentos`. | **Esperado pela especificacão:** Classe Entity + tabela criada.<br>**Encontrada:** CRUD sobre schema H2 criado pelo ORM; [OrganizacaoPacotesTest][archtest] verifica `@Entity` e separação dos modelos. |
| R12 | **CRUD Panache Repository**<br>**Requisito:** Persistir e consultar dados utilizando Panache.<br>**Critério de aceitação:** Utilizar `PanacheRepository` (ou `PanacheEntity`, se justificado) e realizar CRUD completo sobre o recurso principal. | [Repository][repo] implementa `PanacheRepository<AgendamentoEntity>`; [Service][service] usa listagem, busca, persistência, dirty checking e exclusão. | **Esperado pela especificacão:** Código de persistência + CRUD real.<br>**Encontrada:** `crudCompletoComLocationEAtualizacaoPersistida` em [Teste HTTP][http]. |
| R13 | **Transações e atualização**<br>**Requisito:** Garantir consistência nas operações de escrita.<br>**Critério de aceitação:** Operações de criação/atualização/exclusão usam `@Transactional` quando necessário; demonstrar que PUT altera o dado consultado depois. | [Service][service] usa `@Transactional` nas três escritas; PUT altera entidade gerenciada e força `flush`. | **Esperado pela especificacão:** Serviço transacional + sequência PUT/GET.<br>**Encontrada:** `crudCompletoComLocationEAtualizacaoPersistida` faz PUT/GET; `duasReservasSimultaneasGravamApenasUma` e cenários de falha verificam consistência em [Teste HTTP][http]. |

## Validação e tratamento de erros (R14–R16)

| ID | Requisito | Implementação | Evidência |
|---|---|---|---|
| R14 | **Bean Validation**<br>**Requisito:** Validar dados de entrada por anotações.<br>**Critério de aceitação:** Utilizar `@Valid` com pelo menos duas restrições distintas, como `@NotBlank`, `@Size`, `@Min` ou `@Positive`. | [Request DTO][dto] tem `@NotNull`, `@Positive`, `@NotBlank`, `@Pattern`, `@FutureOrPresent`; [Resource][resource] usa `@Valid`. | **Esperado pela especificacão:** Requisição inválida rejeitada com 400.<br>**Encontrada:** `validaCamposObrigatoriosEFormatoDeErro400`, `rejeitaIdsNaoPositivosUfInvalidaEDataPassada` e `rejeitaJsonEnumEDataMalformados`, em [Teste HTTP][http]. |
| R15 | **ExceptionMapper personalizado**<br>**Requisito:** Tratar exceção de negócio com ExceptionMapper.<br>**Critério de aceitação:** Definir ao menos uma exceção específica (ex.: conflito de estado) e mapear para status apropriado sem try/catch repetido em cada endpoint. | Exceções específicas de ausência, conflito e feriado no domínio; [mappers][mappers] `@Provider` produzem 404/409 e integração produz 503. | **Esperado pela especificacão:** Exceção, mapper e cenário HTTP.<br>**Encontrada:** cenários de conflito/feriado e `padroniza404EmConsultaAtualizacaoEExclusao` em [Teste HTTP][http]. |
| R16 | **Resposta de erro padronizada**<br>**Requisito:** Padronizar erros REST e validar os cenários.<br>**Critério de aceitação:** Responder JSON consistente com, por exemplo, `codigo`, `mensagem` e `timestamp`, em ao menos dois erros distintos (ex.: 400 e 404). | [ErroResponse][erro] e `ErroHttp` padronizam `codigo`, `mensagem`, `timestamp`, `path` e `detalhes`. | **Esperado pela especificacão:** Postman mostrando corpos de erro e dois testes.<br>**Encontrada:** dois testes distintos, `validaCamposObrigatoriosEFormatoDeErro400` e `padroniza404EmConsultaAtualizacaoEExclusao`, usam helper `erro` para verificar JSON e campos. [Postman][postman] possui cenários 400/404. |

[resource]: agendamento-api/src/main/java/studio/agendamento/agendamentos/boundary/AgendamentoResource.java
[service]: agendamento-api/src/main/java/studio/agendamento/agendamentos/domain/AgendamentoService.java
[entity]: agendamento-api/src/main/java/studio/agendamento/agendamentos/repository/AgendamentoEntity.java
[repo]: agendamento-api/src/main/java/studio/agendamento/agendamentos/repository/AgendamentoRepository.java
[dto]: agendamento-api/src/main/java/studio/agendamento/agendamentos/boundary/AgendamentoRequestDto.java
[client]: agendamento-api/src/main/java/studio/agendamento/integracao/feriados/FeriadosClient.java
[feriados]: agendamento-api/src/main/java/studio/agendamento/integracao/feriados/FeriadosService.java
[mappers]: agendamento-api/src/main/java/studio/agendamento/platform/error/mapper
[erro]: agendamento-api/src/main/java/studio/agendamento/platform/error/ErroResponse.java
[metrics]: agendamento-api/src/main/java/studio/agendamento/platform/metrics/AgendamentoMetrics.java
[live]: agendamento-api/src/main/java/studio/agendamento/platform/health/AgendamentoLivenessCheck.java
[ready]: agendamento-api/src/main/java/studio/agendamento/platform/health/AgendamentoReadinessCheck.java
[http]: agendamento-api/src/test/java/studio/agendamento/agendamentos/boundary/AgendamentoResourceTest.java
[mocktest]: agendamento-api/src/test/java/studio/agendamento/agendamentos/boundary/FeriadosTestResource.java
[healthtest]: agendamento-api/src/test/java/studio/agendamento/platform/StagingReadinessTest.java
[archtest]: agendamento-api/src/test/java/studio/agendamento/architecture/OrganizacaoPacotesTest.java
[mocklocal]: agendamento-api/feriados-local/src/FeriadosLocalServer.java
[pom]: agendamento-api/pom.xml
[config]: agendamento-api/src/main/resources/application.properties
[staging]: agendamento-api/src/main/resources/application-staging.properties
[script]: agendamento-api/scripts/validar-http.ps1
[script-bash]: agendamento-api/scripts/validar-http.sh
[postman]: agendamento-api/postman/projeto-final.postman_collection.json
