# Evidências — Projeto final de agendamento de massagem

Enunciado principal: [trabalho-final.md](../trabalho-final.md).
Os 16 itens abaixo têm implementação e roteiro reproduzível.
A pontuação cabe à avaliação do professor; este documento não atribui nota automaticamente.

## Execução automatizada

Executado em 31/10/2026 com Java 21.0.11, Maven 3.9.16 e Quarkus 3.40.1:

```text
mvn -B -ntp package
AgendamentoResourceTest: 15 testes, 0 falhas, 0 erros
StagingReadinessTest:     1 teste,  0 falhas, 0 erros
OrganizacaoPacotesTest (agendamento): 1 teste, 0 falhas, 0 erros
Testes anteriores de catálogo: 15 testes, 0 falhas, 0 erros
Total:                  32 testes, 0 falhas, 0 erros, 0 ignorados
```

Os testes usam HTTP real e mock em porta dinâmica, sem depender de Docker ou API pública.
Relatórios gerados em target/surefire-reports (não versionados).
O warning SQLState 23505 no cenário concorrente é esperado: a constraint rejeita uma
segunda reserva e a API a converte em 409, preservando uma única gravação.

## Arquivos e lacunas completados nesta reavaliação

- Requisitos 01/02/04: adicionado `AgendamentoResponseDto.from(AgendamentoBo)`.
  O campo existente `salaDisponivel` retorna false para uma reserva ocupada.
- Requisitos 05/11/12/13: completado `AgendamentoRepository` como bean CDI,
  consulta de conflito com exclusão do próprio ID no PUT, enum JPA textual e
  constraint única por sala/data/período. UF não faz parte da chave global da sala.
- Requisitos 14/16: substituído `org.wildfly.common.annotation.NotNull` por
  Bean Validation Jakarta; adicionadas restrições de IDs, UF e data. Usuário
  continua opcional, com ID positivo quando informado.
- Requisitos 06/07/08/09: completadas configurações do cliente de feriados,
  nome, readiness e staging. Dev/staging/prod usam H2 persistente sem recriar
  schema. Somente testes usam schema descartável em memória.
- Requisitos 07/16: resolvida duplicidade de `/info`; a informação do catálogo
  anterior passa a `/catalogo/info`, com teste correspondente atualizado.
  Mapper de validação tem prioridade explícita e detalhes por nome do campo.
- Entrega e requisitos 01–16: README atualizado para o domínio atual, com
  comandos, exemplos, profiles e matriz de evidências. README anterior preservado
  em [README-catalogo-aulas.md](README-catalogo-aulas.md); arquitetura atualizada.
  `.gitignore` passa a excluir bancos H2 locais, sem remover arquivos existentes.
- Testes: a URL do mock restrito ao teste passou a ser opcional na injeção,
  evitando falha de inicialização ao executar a suíte antiga. O teste HTTP só
  a utiliza quando seu mock está iniciado. Acrescentadas verificações de
  `id_usuario` opcional, sala ocupada na resposta e conflito entre UFs distintas.

Os arquivos de mock, coleção Postman, script HTTP, Service, integração e mappers
já estavam presentes e foram conferidos. Não foi necessário recriar esses arquivos.

## Matriz dos requisitos

Os nomes de classes referem-se aos pacotes em studio.agendamento.

| Nº | Implementação | Evidência / reprodução |
|---|---|---|
| 01 | AgendamentoResource: GET coleção/ID, POST, PUT, DELETE | crudCompletoComLocationEAtualizacaoPersistida; pasta principal do Postman |
| 02 | Status 200/201/204/400/404; Location na criação | CRUD e testes de erro; validar-http.ps1 |
| 03 | @RequiredArgsConstructor em Resource/Service | Construtores dos campos final gerados pelo Lombok, usados pelo CDI; compilação JDK 21. Sem @Data em Entity |
| 04 | Jakarta REST, @Path, @Produces/@Consumes JSON | Resource e respostas HTTP dos testes/coleção |
| 05 | AgendamentoService @ApplicationScoped, injetado no Resource | bloqueiaConflitosMasPermiteOutrasSalasEPeriodos e feriadoBloqueiaPostEPutSemModificarDados |
| 06 | FeriadosClient @RegisterRestClient e FeriadosService | Mock HTTP real; dia comum permite POST, feriado devolve 409 |
| 07 | application.properties e application-staging.properties; @ConfigProperty no /info | StagingReadinessTest; comandos dev/staging e AGENDAMENTO_NOME no README |
| 08 | AgendamentoLivenessCheck, AgendamentoReadinessCheck e saúde do datasource | saudeEConfiguracaoRespondem e StagingReadinessTest: live 200/UP enquanto ready 503/DOWN |
| 09 | @Retry(maxRetries=2) e @Fallback na consulta de feriados | falhaExternaExecutaTresTentativasESemGravacao verifica 3 chamadas e 503; falhaExternaNoPutPreservaReserva |
| 10 | AgendamentoMetrics observa AgendamentoCriado AFTER_SUCCESS | contadorSomenteAumentaAposCriacaoConfirmada; concorrência/falha preservam contador; /q/metrics |
| 11 | AgendamentoEntity JPA separada de DTO e BO, enum textual | CRUD cria tabela no H2; OrganizacaoPacotesTest verifica separação |
| 12 | AgendamentoRepository implements PanacheRepository | CRUD HTTP persiste, consulta, altera e exclui |
| 13 | @Transactional nas escritas, flush e constraint única | PUT seguido de GET; duasReservasSimultaneasGravamApenasUma retorna 201 e 409 |
| 14 | @Valid, @NotNull, @Positive, @NotBlank, @Pattern e @FutureOrPresent | Testes de obrigatórios, IDs, UF e data; 400 em JSON padronizado |
| 15 | Exceções de ausência/conflito/feriado/integração e respectivos ExceptionMappers | 404/409/503 sem try/catch repetido nos endpoints |
| 16 | ErroResponse/ErroHttp, validação e mapeamento de JSON inválido | Testes distintos de 400 e 404 verificam codigo, mensagem, timestamp, path e detalhes |

## Demonstração manual reproduzível

1. Iniciar mock e API conforme README; apresentar Resource → Service → Repository, DTO → BO → Entity.
2. Executar a pasta “Fluxo principal — mock UP” da coleção Postman ou scripts/validar-http.ps1.
3. Mostrar PUT seguido de GET, conflito 409, feriado 409 e erros 400/404 com o mesmo formato JSON.
4. Comparar agendamentos_criados_total antes/depois da criação.
5. Encerrar o mock e executar validar-http.ps1 -FalhaExterna: 503, sem registro nem incremento.
6. Reiniciar o mock; demonstrar nova criação com 201.
7. Executar dev e staging, sobrescrever AGENDAMENTO_NOME e consultar /info.
8. Reiniciar com AGENDAMENTO_PRONTO=false; live permanece UP e ready fica DOWN.
9. Preencher os integrantes no README antes de entregar o repositório.

No Postman, testarFalha=true habilita o cenário que exige mock desligado.
testarReadinessDown=true habilita o cenário de readiness DOWN.
Deixe ambos false na execução normal da coleção. Para demonstrar falhas, execute apenas
as requisições opcionais correspondentes; não a pasta principal, que pressupõe dependências UP.

## Limites da evidência

- Os testes Maven e o mock Java foram executados; o calendário é simulado.
- A coleção Postman é fornecida para a equipe executar; o runner Postman/Newman não está instalado neste ambiente.
- Docker Compose é opcional e não foi executado.
- Nome da equipe e integrantes não foram fornecidos; o README mantém campos
  explicitamente a informar. O pacote nativo não foi gerado nesta reavaliação.

## Verificação fora dos testes nesta reavaliação — 31/10/2026

Executados com JDK 21, API na porta 19091, mock na 18081 e H2 de verificação
sob `target/`, sem usar os arquivos de `data/`:

- `mvn -B -ntp package`: BUILD SUCCESS, 32 testes sem falhas/erros/ignorados.
- JAR `target/quarkus-app/quarkus-run.jar` iniciado em prod com H2 em arquivo.
- `AGENDAMENTO_NOME` sobrescrito por variável, confirmado em `/info`.
- `scripts/validar-http.ps1 -BaseUrl http://localhost:19091`: passou o fluxo
  completo com CRUD, Location, PUT/GET, 400/404/409, feriado e contador.
- Mock interrompido; execução com `-FalhaExterna` passou, verificando 503 sem
  gravação nem incremento do contador. Mock reiniciado e criação voltou a 201.
- JAR iniciado em staging; `/info` retornou o nome padrão STAGING. Com
  `AGENDAMENTO_PRONTO=false`, liveness retornou 200/UP, readiness 503/DOWN e
  o health do banco permaneceu UP.
- Reserva criada em staging recuperada após reiniciar o JAR em prod, no mesmo
  arquivo H2 isolado. Reserva temporária excluída após a consulta.
- Mock fonte executado diretamente pelo Java 21; `FERIADOS_EXTRAS` marcou
  11/02/2027 como feriado em SP e dia comum em RJ.
- Links locais do README, arquitetura e evidências conferidos. Coleção Postman
  parseada como JSON válido (21 requisições); sintaxe PowerShell do roteiro validada.

Os processos de verificação foram encerrados. Os arquivos preexistentes
`data/agendamentos.mv.db` e `data/catalogo*` mantiveram tamanho e data de
modificação anteriores à reavaliação; nenhum deles foi aberto pela verificação.

O runner Postman/Newman, Docker e o modo nativo continuam como demonstrações
não executadas nesta reavaliação. O modo dev consta do registro anterior abaixo;
nesta revisão, os profiles executados fora dos testes foram prod e staging.

## Verificação fora dos testes (registro anterior preservado)

O registro abaixo já constava neste arquivo antes da reavaliação atual; não
deve ser confundido com uma nova execução desses passos nesta revisão.

Também executado em 31/10/2026, em portas isoladas (API 19091 e mock 18081):

- Mock iniciado diretamente do fonte com Java 21; feriado de 1º de janeiro e dia comum confirmados.
- FERIADOS_EXTRAS demonstrado: 11/02/2027 feriado em SP e dia comum em RJ.
- Pacote target/quarkus-app/quarkus-run.jar iniciado em staging com H2 em arquivo de teste sob target/.
- AGENDAMENTO_NOME sobrescrito por variável: /info retornou “Massagem - verificacao por variavel”.
- scripts/validar-http.ps1 passou: CRUD, Location, PUT/GET, 400/404/409, feriado e contador antes/depois.
- Mock encerrado; scripts/validar-http.ps1 -FalhaExterna passou: 503 sem reserva ou incremento de contador.
- Mock reiniciado; uma reserva foi criada e a aplicação encerrada.
- mvn quarkus:dev iniciado usando o mesmo arquivo H2 de teste; GET recuperou a reserva anterior.
- Em dev com AGENDAMENTO_PRONTO=false, live retornou 200/UP e ready retornou 503/DOWN; datasource permaneceu UP.
- A reserva de verificação foi excluída. Os processos de verificação foram encerrados.

Os arquivos antigos data/catalogo* não foram alterados ou removidos.
