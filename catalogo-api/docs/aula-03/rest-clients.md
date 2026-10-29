# Comparação de REST Clients no Quarkus

O serviço `estoque-local` deste projeto expõe:

```text
GET http://127.0.0.1:8081/products
```

e responde com um catálogo como este:

```json
{
  "products": [
    { "id": 1, "title": "Caderno", "stock": 5 }
  ],
  "total": 1
}
```

Os exemplos abaixo usam os mesmos tipos para desserializar a resposta:

```java
public record Product(long id, String title, int stock) {}

public record Catalogo(List<Product> products, int total) {}
```

## Escolha rápida

| Cliente | Use quando | Evite quando |
|---|---|---|
| Java `HttpClient` | Não há Quarkus/CDI, você precisa de uma chamada muito específica ou quer depender apenas da JDK. | Você quer configuração externa, injeção CDI, métricas e uma interface declarativa prontas. |
| RESTEasy Classic Client | Você mantém uma aplicação que já usa RESTEasy Classic ou precisa integrar código legado baseado nele. | Você está iniciando uma aplicação Quarkus nova. É a pilha legada do Quarkus. |
| Quarkus REST Client | Aplicações Quarkus novas; é a escolha padrão para interfaces declarativas, configuração em `application.properties` e injeção CDI. | Você precisa que o mesmo cliente funcione fora do Quarkus sem dependências MicroProfile/Quarkus. |

Para projetos novos em Quarkus, prefira **Quarkus REST Client**.

## 1. Java `HttpClient`

O `HttpClient` vem na JDK 11 ou superior. Ele não participa do CDI: a URL, o envio da requisição, a validação do status e a conversão do JSON ficam explícitos no código.

Dependência Maven para JSON com Jackson, caso ela ainda não exista no projeto:

```xml
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
</dependency>
```

Exemplo:

```java
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class EstoqueComJavaHttpClient {
    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public Catalogo buscarProdutos() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:8081/products"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(
                request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IllegalStateException("Estoque respondeu " + response.statusCode());
        }
        return objectMapper.readValue(response.body(), Catalogo.class);
    }
}
```

Para chamadas não bloqueantes, use `client.sendAsync(...)`, que devolve um `CompletableFuture`.

## 2. RESTEasy Classic Client

RESTEasy Classic é a implementação cliente tradicional do RESTEasy. No Quarkus, ela é indicada principalmente para manutenção/migração de aplicações que já adotaram essa pilha. Não misture as extensões Classic e Quarkus REST para o mesmo cliente.

Para usá-lo **neste projeto**, substitua a dependência atual `quarkus-rest-client-jackson` pela dependência Classic abaixo. As duas implementações não devem coexistir para o mesmo cliente.

```xml
<!-- Remover: quarkus-rest-client-jackson -->
```

Adicione:

```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-resteasy-client-jackson</artifactId>
</dependency>
```

O código da interface, a injeção com `@RestClient` e a propriedade `quarkus.rest-client.estoque-local.url` podem ser iguais aos usados pelo Quarkus REST Client. A troca está na dependência e, principalmente, na implementação que o Quarkus usa internamente em tempo de execução. Essa semelhança facilita uma migração de projetos existentes; para código novo, mantenha a extensão `quarkus-rest-client-jackson`.

## 3. Quarkus REST Client

O Quarkus REST Client implementa o padrão MicroProfile REST Client. Você descreve o contrato HTTP em uma interface e injeta a implementação gerada pelo Quarkus. A URL fica fora do código, em `application.properties`.

Dependência:

```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-rest-client-jackson</artifactId>
</dependency>
```

Interface cliente:

```java
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("/products")
@RegisterRestClient(configKey = "estoque-local")
public interface EstoqueGateway {
    @GET
    Catalogo products();
}
```

Configuração:

```properties
quarkus.rest-client.estoque-local.url=http://127.0.0.1:8081
```

Uso em um serviço CDI:

```java
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@ApplicationScoped
public class ProductService {
    private final EstoqueGateway estoqueGateway;

    public ProductService(@RestClient EstoqueGateway estoqueGateway) {
        this.estoqueGateway = estoqueGateway;
    }

    public Catalogo buscarProdutos() {
        return estoqueGateway.products();
    }
}
```

`configKey = "estoque-local"` faz a interface procurar propriedades iniciadas por `quarkus.rest-client.estoque-local`, como `url`, timeouts e cabeçalhos. Sem essa chave, a configuração precisaria usar o nome completo da interface, por exemplo `quarkus.rest-client."studio.EstoqueGateway".url`.

## RESTEasy Classic × Quarkus REST

RESTEasy Classic não é uma versão antiga do Quarkus. As duas pilhas funcionam em versões atuais do Quarkus; elas são implementações REST diferentes.

| Aspecto | RESTEasy Classic | Quarkus REST |
|---|---|---|
| Origem | Pilha REST tradicional do RESTEasy | Pilha REST mais recente, construída para o Quarkus |
| Extensões de servidor | `quarkus-resteasy-*` | `quarkus-rest-*` |
| Extensões de cliente | `quarkus-resteasy-client-*` | `quarkus-rest-client-*` |
| Processamento | Conserva o modelo tradicional, com mais descoberta/configuração em runtime | Move mais processamento para o build da aplicação |
| Inicialização e memória | Em geral, demanda mais trabalho ao iniciar | Em geral, inicia mais rápido e usa menos memória |
| Melhor cenário | Manutenção ou migração de aplicações que já dependem de RESTEasy Classic | Projetos Quarkus novos |
| Código com MicroProfile REST Client | Pode ser igual | Pode ser igual |

### O que muda no funcionamento?

Em ambos os casos, o resultado da chamada é o mesmo: o `EstoqueGateway` envia `GET /products`, recebe o JSON e o converte em `Catalogo`.

A diferença é quando e como a infraestrutura é preparada. O Quarkus REST analisa mais informações durante o build, por exemplo rotas, tipos JSON e provedores. Assim, sobra menos descoberta para a inicialização da aplicação. Isso normalmente favorece tempo de start e consumo de memória, principalmente em aplicações com muitos endpoints ou em imagens nativas.

RESTEasy Classic preserva a arquitetura RESTEasy tradicional. Essa compatibilidade é valiosa se o projeto já usa extensões, filtros, providers ou bibliotecas específicos dessa pilha. Trocar para Quarkus REST nesses casos pode exigir adaptação e testes adicionais.

Para o `catalogo-api`, que é um projeto novo e não possui dependências Classic, a escolha recomendada é Quarkus REST:

```xml
<artifactId>quarkus-rest-jackson</artifactId>
<artifactId>quarkus-rest-client-jackson</artifactId>
```

## Resumo de diferenças entre os três clientes

| Cliente | Estilo | Configuração da URL | Escolha recomendada |
|---|---|---|---|
| Java `HttpClient` | Imperativo: você monta e envia a requisição manualmente | Normalmente no próprio código ou lida manualmente a partir de configuração | Aplicações Java sem Quarkus ou casos com controle HTTP muito específico |
| RESTEasy Classic Client | JAX-RS/MicroProfile, com API tradicional | `application.properties` quando usado como MicroProfile REST Client | Sistemas Quarkus existentes baseados em RESTEasy Classic |
| Quarkus REST Client | Declarativo: interface Java + CDI | `application.properties`, usando `configKey` | Este `catalogo-api` e novos projetos Quarkus |
