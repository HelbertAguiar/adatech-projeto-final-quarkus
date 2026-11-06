# Mock HTTP de feriados

Executar na raiz da API com JDK 21:

```powershell
java feriados-local/src/FeriadosLocalServer.java
```

GET http://127.0.0.1:8081/feriados?uf=SP&data=2030-01-01 retorna feriado=true.
Somente 1º de janeiro de qualquer ano e datas configuradas são feriados neste mock.
Não representa um calendário oficial. Datas são locais, sem horário.

Para adicionar feriados antes de iniciar:

```powershell
$env:FERIADOS_EXTRAS='SP:2030-04-10,RJ:2030-05-20'
java feriados-local/src/FeriadosLocalServer.java
```

FERIADOS_HOST (padrão 127.0.0.1) e FERIADOS_PORT (8081) controlam o servidor.
Ctrl+C encerra o mock. A API então devolve 503 nas escritas que dependem da consulta.
