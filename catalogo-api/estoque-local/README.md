# Estoque Local

Servidor HTTP simples, feito somente com o JDK, que disponibiliza `GET /products` na porta `8081`. Ele é a dependência externa usada pelo `catalogo-api`.

Os comandos abaixo devem ser executados a partir da raiz do projeto `catalogo-api/`.

## Executar na JVM

Requer JDK 21 ou superior.

### Pelo IntelliJ IDEA

O projeto contém a configuração compartilhada `Estoque Local JVM`. Após recarregar o projeto, selecione-a no menu de configurações no canto superior direito do IntelliJ IDEA e clique em **▶ Run**. Ela executa o mesmo comando abaixo a partir da raiz do projeto.

```powershell
java --class-path estoque-local/src estoque-local/src/EstoqueLocalServer.java
```

Por padrão, o servidor responde apenas em `127.0.0.1:8081`. Teste em outro terminal:

```powershell
curl.exe -i http://localhost:8081/products
```

Para informar outra porta ou interface de rede, passe os argumentos em pares:

```powershell
java --class-path estoque-local/src estoque-local/src/EstoqueLocalServer.java --port 8082 --host 127.0.0.1
```

Pare o processo com `Ctrl+C`.

## Executar em Docker

O `Dockerfile` compila o servidor com JDK 21 e cria uma imagem final com JRE 21. Dentro do contêiner, o servidor escuta em `0.0.0.0:8081`, permitindo que outros contêineres o acessem.

Crie a imagem:

```powershell
docker build -f estoque-local/Dockerfile -t catalogo-estoque:aula04 ./estoque-local
```

Execute o contêiner e publique a porta para testes a partir do Windows:

```powershell
docker run --rm --name estoque-local -p 8081:8081 catalogo-estoque:aula04
```

Em outro terminal:

```powershell
curl.exe -i http://localhost:8081/products
```

## Integrar com o catálogo em Docker

Quando o catálogo também está em um contêiner, não use `localhost` ou `127.0.0.1` como URL do estoque: dentro do contêiner, esses endereços apontam para o próprio catálogo. Coloque os dois na mesma rede Docker:

```powershell
docker network create catalogo-aula04
docker run -d --name estoque-local --network catalogo-aula04 -p8081:8081 catalogo-estoque:aula04
docker run -d --name catalogo-native --network catalogo-aula04 -p 9091:9091 -e ESTOQUE_LOCAL_URL="http://estoque-local:8081" catalogo-api:aula04-native
```

O hostname `estoque-local` é resolvido pelo DNS da rede `catalogo-aula04`. Teste a integração:

```powershell
curl.exe -i http://localhost:9091/products
curl.exe -i http://localhost:9091/products-native
```

Para simular a indisponibilidade do estoque e validar o fallback do catálogo:

```powershell
docker stop estoque-local
curl.exe -i http://localhost:9091/products-native
docker start estoque-local
```

O endpoint `/products-native` é a variante com resposta tipada para Native Image; quando o estoque estiver parado, ele deve retornar HTTP `503` e `origem: "FALLBACK"`.
