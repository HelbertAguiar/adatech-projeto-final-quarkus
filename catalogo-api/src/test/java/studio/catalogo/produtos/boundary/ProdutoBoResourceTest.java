package studio.catalogo.produtos.boundary;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/** Aula 05: CRUD real no H2 usando HTTP; nao depende do Estoque Local. */
@QuarkusTest
class ProdutoBoResourceTest {

    @Test
    void deveListarProdutosDoBancoEFiltrarPorNome() {
        given().when().get("/produtos").then()
                .statusCode(200)
                .body("size()", greaterThanOrEqualTo(3))
                .body("nome", hasItems("Caderno", "Caneta", "Pasta"));

        given().queryParam("nome", "cade").when().get("/produtos").then()
                .statusCode(200)
                .body("size()", is(1))
                .body("[0].nome", is("Caderno"));
    }

    @Test
    void deveExecutarFluxoCompletoDeCrud() {
        Map<String, Object> inicial = Map.of(
                "nome", "Produto de teste", "preco", 19.90, "quantidade", 3);

        int id = given().contentType("application/json").body(inicial)
                .when().post("/produtos").then()
                .statusCode(201)
                .header("Location", containsString("/produtos/"))
                .body("nome", is("Produto de teste"))
                .extract().path("id");

        try {
            given().when().get("/produtos/" + id).then()
                    .statusCode(200).body("quantidade", is(3));

            given().contentType("application/json")
                    .body(Map.of("nome", "Produto atualizado", "preco", 25.50, "quantidade", 8))
                    .when().put("/produtos/" + id).then()
                    .statusCode(200)
                    .body("nome", is("Produto atualizado"))
                    .body("quantidade", is(8));

            given().when().get("/produtos/" + id).then()
                    .statusCode(200).body("nome", is("Produto atualizado"));
        } finally {
            given().when().delete("/produtos/" + id).then().statusCode(204);
        }

        given().when().get("/produtos/" + id).then().statusCode(404);
    }

    @Test
    void deveRejeitarDadosInvalidos() {
        given().contentType("application/json")
                .body(Map.of("nome", "", "preco", -10, "quantidade", -1))
                .when().post("/produtos").then().statusCode(400);
    }

    @Test
    void deveRetornar404SeNaoEncontrarProduto() {
        given().when().get("/produtos/99999999").then().statusCode(404);
        given().contentType("application/json")
                .body(Map.of("nome", "Outro", "preco", 3, "quantidade", 1))
                .when().put("/produtos/99999999").then().statusCode(404);
        given().when().delete("/produtos/99999999").then().statusCode(404);
    }
}
