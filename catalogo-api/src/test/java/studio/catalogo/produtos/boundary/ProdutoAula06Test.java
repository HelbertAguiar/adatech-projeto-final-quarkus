package studio.catalogo.produtos.boundary;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/** Testes HTTP completos, sem precisar iniciar o servidor de estoque. */
@QuarkusTest
class ProdutoAula06Test {

    @Test
    void devePadronizarErroDeValidacaoComListaDeCampos() {
        given().contentType("application/json")
                .body(Map.of("nome", "", "preco", -1, "quantidade", -2))
                .when().post("/produtos")
                .then().statusCode(400)
                .body("codigo", is("DADOS_INVALIDOS"))
                .body("mensagem", not(isEmptyOrNullString()))
                .body("timestamp", not(isEmptyOrNullString()))
                .body("path", is("/produtos"))
                .body("detalhes.campo", hasItems("nome", "preco", "quantidade"));
    }

    @Test
    void devePadronizarProdutoNaoEncontrado() {
        given().when().get("/produtos/99999999")
                .then().statusCode(404)
                .body("codigo", is("PRODUTO_NAO_ENCONTRADO"))
                .body("path", is("/produtos/99999999"))
                .body("detalhes", hasSize(0));
    }

    @Test
    void deveValidarRetiradaETratarConflitoDeEstoque() {
        int id = given().contentType("application/json")
                .body(Map.of("nome", "Produto demonstracao aula 06", "preco", 15.5, "quantidade", 2))
                .when().post("/produtos").then().statusCode(201)
                .extract().path("id");
        try {
            given().contentType("application/json")
                    .body(Map.of("quantidade", 0))
                    .when().post("/produtos/" + id + "/retiradas")
                    .then().statusCode(400)
                    .body("codigo", is("DADOS_INVALIDOS"))
                    .body("detalhes.campo", hasItem("quantidade"));

            given().contentType("application/json")
                    .body(Map.of("quantidade", 5))
                    .when().post("/produtos/" + id + "/retiradas")
                    .then().statusCode(409)
                    .body("codigo", is("ESTOQUE_INSUFICIENTE"))
                    .body("path", is("/produtos/" + id + "/retiradas"));

            given().when().get("/produtos/" + id)
                    .then().statusCode(200).body("quantidade", is(2));

            given().contentType("application/json")
                    .body(Map.of("quantidade", 1))
                    .when().post("/produtos/" + id + "/retiradas")
                    .then().statusCode(200).body("quantidade", is(1));

            given().when().get("/produtos/" + id)
                    .then().statusCode(200).body("quantidade", is(1));
        } finally {
            given().when().delete("/produtos/" + id).then().statusCode(204);
        }
    }

    @Test
    void retiradaDeProdutoInexistenteDeveRetornar404() {
        given().contentType("application/json")
                .body(Map.of("quantidade", 1))
                .when().post("/produtos/99999999/retiradas")
                .then().statusCode(404)
                .body("codigo", is("PRODUTO_NAO_ENCONTRADO"));
    }
}
